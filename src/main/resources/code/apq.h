
#ifndef APQ_H
#define APQ_H

#include "X.h"

typedef struct {
   uint64_t start_ticks;
   uint64_t total_ticks;
   uint64_t count;
} APC;

static inline void APC_Init(APC *c)
{
   c->start_ticks = 0;
   c->total_ticks = 0;
   c->count = 0;
}

static inline void APC_Start(APC *c)
{
   c->start_ticks = xrdtsc();
}

static inline void APC_Stop(APC *c)
{
   const uint64_t t = xrdtsc();
   c->total_ticks += t - c->start_ticks;
}

#define APQ_COOKIE 0x1337
#define APQ_FLAG_CLOSED    (1 << 0)

// Note that read_ptr, write_ptr, and wrap_ptr are in units of width bytes.
typedef struct {

   uint16_t cookie;
   uint16_t flags;
   uint8_t  pad0[4];

   volatile uint32_t read_ptr;
   uint8_t pad1[4];

   volatile uint32_t write_ptr;
   uint8_t pad2[4];

   volatile uint32_t wrap_ptr;
   uint8_t pad3[4];

   uint32_t depth;      // Number of items that can be put in the queue.
   uint32_t width;      // Number of bytes for each element.

   uint64_t reserved;   // Make this structure 16-byte aligned.
   char data[0];
} APQ;

/** Initialize the queue.
 * The memory for the queue must have already been allocated.
 */
static inline void APQ_Initialize(APQ *q, uint32_t depth, uint32_t width)
{
   q->flags = 0;
   q->read_ptr = 0;
   q->write_ptr = 0;
   q->wrap_ptr = 0;
   q->depth = depth;
   q->width = width;
   q->reserved = 0;
   q->cookie = APQ_COOKIE;
}

/** Mark the queue as closed. */
static inline void APQ_Close(APQ *q)
{
   q->flags |= APQ_FLAG_CLOSED;
}

/** Determine how many bytes are needed for the specified queue. */
static inline size_t APQ_GetSize(uint32_t depth, uint32_t width)
{
   return sizeof(APQ) + depth * width;
}

/** Determine if the queue is valid. */
static inline int APQ_IsValid(APQ *q)
{
   return q->cookie == APQ_COOKIE;
}

/** Determine if the queue has been closed. */
static inline int APQ_IsClosed(APQ *q)
{
   return (q->flags & APQ_FLAG_CLOSED) != 0;
}

/** Determine if the queue is empty. */
static inline int APQ_IsEmpty(APQ *q)
{
   return (q->read_ptr == q->write_ptr)
      || (q->write_ptr == 0 && q->read_ptr == q->wrap_ptr);
}

/** Determine how much space is available in the queue.
 * Assuming the thread pushing data on to the queue calls this,
 * the queue will have at least this much space free.
 * This will return the maximum size of a write that can be supported.
 * In other words, the buffer may have more free space than is returned.
 */
static inline int APQ_GetFree(APQ *q)
{

   /* Get a local copy of volatiles. */
   const int read_ptr = (int)q->read_ptr;
   const int write_ptr = (int)q->write_ptr;

   if(read_ptr <= write_ptr) {

      /* (free) read (data) write (free) */
      /* Return the largest contiguous segement. */
      const int end_space = q->depth - write_ptr;
      const int begin_space = read_ptr - 1;
      if(end_space > begin_space) {
         return end_space;
      } else {
         return begin_space;
      }

   } else { /* read_ptr > write_ptr */

      /* (data) write (free) read (data/wrap) */
      return read_ptr - write_ptr - 1;

   }

}
/** Determine how much of a queue is used.
 * This assumes that the queue will not change size while running.
 */
static inline int APQ_GetUsed(APQ *q)
{
   const int read_ptr = (int)q->read_ptr;
   const int write_ptr = (int)q->write_ptr;

   if(read_ptr <= write_ptr) {

      /* (free) read (data) write (free) */
      return write_ptr - read_ptr;

   } else {

      /* (data) write (free) read (data) wrap */
      return write_ptr + q->depth - read_ptr;

   }

}

/** Get an item offset for writing.
 * Note that "count" can be no more than half the size of the queue.
 * This will return -1 if there is no room.
 */
static inline int APQ_StartWriteOffset(APQ *q, uint32_t count)
{

   uint32_t wrap_needed = 0;
   uint32_t start;
   uint32_t end;
   uint32_t read_ptr;
   uint32_t wrap_ptr;

   start    = q->write_ptr;
   end      = start + count;
   read_ptr = q->read_ptr;
   wrap_ptr = q->wrap_ptr;

   if(end > q->depth) {

      // New end is past the end of the queue.

      // Make sure there is room.
      if(read_ptr <= count) {
         return -1;
      }
      end = count;
      wrap_ptr = start;
      start = 0;
      wrap_needed = 1;

   } else {

      // No wrap needed.

      // Update the wrap pointer if needed.
      if(end > wrap_ptr) {
         wrap_ptr = end;
      }

   }

   // Make sure there is enough space available.
   if(   (start < read_ptr && read_ptr <= end)
      || read_ptr > wrap_ptr
      || (wrap_needed && read_ptr <= end)) {

      return -1;
   }

   // We have room.
   // Update the write and wrap pointers.
   q->wrap_ptr = wrap_ptr;
   if(start == 0) {
      q->write_ptr = 0;
   }

   // Return a pointer to a place to write.
   return start;

}

/** Get a buffer for writing.
 * Note that "count" can be no more than half the size of the queue.
 * This will return NULL if there is no room.
 */
static inline char *APQ_StartWrite(APQ *q, uint32_t count)
{
   const int offset = APQ_StartWriteOffset(q, count);
   if (offset >= 0) {
      return &q->data[offset * q->width];
   } else {
      return NULL;
   }
}

/** Get a buffer for writing (blocking version). */
static inline char *APQ_StartBlockingWrite(APQ *q, uint32_t count)
{
   for(;;) {
      char *ptr = APQ_StartWrite(q, count);
      if(ptr != NULL) {
         return ptr;
      }
      sched_yield();
   }
}

/** Finish a write. */
static inline void APQ_FinishWrite(APQ *q, uint32_t count)
{
   q->write_ptr += count;
}

/** Start a read.
 * This function does not block.  It returns the number of items available.
 * The offset parameter is in items.
 */
static inline uint32_t APQ_StartReadOffset(APQ *q, int *offset)
{
   for(;;) {

      // Get a copy of the pointers.
      const uint32_t read_ptr = q->read_ptr;
      const uint32_t write_ptr = q->write_ptr;
      const uint32_t wrap_ptr = q->wrap_ptr;

      if(read_ptr == write_ptr) {

         // Queue is empty.
         return 0;

      } else if(read_ptr < write_ptr) {

         // Data available from the read pointer to the write pointer.
         *offset = read_ptr;
         return write_ptr - read_ptr;

      } else { // read_ptr >= write_ptr

         if(read_ptr == wrap_ptr) {

            // Wrapped around.
            q->read_ptr = 0;

         } else {

            // Data from read_ptr to wrap_ptr.
            *offset = read_ptr;
            return wrap_ptr - read_ptr;

         }

      }

   }

}

static inline uint32_t APQ_StartRead(APQ *q, char **buffer)
{
   int offset = 0;
   const uint32_t count = APQ_StartReadOffset(q, &offset);
   *buffer = &q->data[offset * q->width];
   return count;
}


/** Start a read (blocking version). */
static inline uint32_t APQ_StartBlockingRead(APQ *q, char **buffer)
{
   for(;;) {
      const uint32_t rc = APQ_StartRead(q, buffer);
      if(rc > 0) {
         return rc;
      }
      sched_yield();
   }
}

/** Finish a read. */
static inline void APQ_FinishRead(APQ *q, uint32_t count)
{
   q->read_ptr += count;
}

#endif

