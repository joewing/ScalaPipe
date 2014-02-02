#ifndef SCALAPIPE_H_
#define SCALAPIPE_H_

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <setjmp.h>
#include <string.h>
#include <sched.h>
#include <math.h>

#ifdef __cplusplus
extern "C" {
#endif

/** SPLIKELY/SPUNLIKELY macros. */
#ifdef __GNUC__
#   if __GNUC__ >= 3
#       define SPLIKELY(x)   __builtin_expect(!!(x), 1)
#       define SPUNLIKELY(x) __builtin_expect(!!(x), 0)
#   else
#       define SPLIKELY(x) (x)
#       define SPUNLIKELY(x) (x)
#   endif
#else
#   define SPLIKELY(x) (x)
#   define SPUNLIKELY(x) (x)
#endif

/** Atomic decrement. */
static inline void sp_decrement(volatile uint32_t *v)
{
    asm volatile("lock decl %0" : "=m" (*v) : "m" (*v));
}

/** Structure to represent the per-instance fields for a kernel.
 * This should align to 8 bytes.
 */
typedef struct {

    int   (*get_free)(int out_port);
    void *(*allocate)(int out_port);
    void  (*send)(int out_port);

    int   (*get_available)(int in_port);
    void *(*read_value)(int in_port);
    void  (*release)(int in_port);

    int in_port_count;
    int out_port_count;

    int instance;
    int padding;

} SPKernelData;

/** Primitive types. */
typedef uint8_t     UNSIGNED8;
typedef uint16_t    UNSIGNED16;
typedef uint32_t    UNSIGNED32;
typedef uint64_t    UNSIGNED64;
typedef int8_t      SIGNED8;
typedef int16_t     SIGNED16;
typedef int32_t     SIGNED32;
typedef int64_t     SIGNED64;
typedef float       FLOAT32;
typedef double      FLOAT64;
typedef long double FLOAT96;
typedef char       *STRING;

/** Get a pointer to the private kernel data from the public data. */
#define sp_get_private( kernel ) \
    ((SPKernelData*)((char*)(kernel) - sizeof(SPKernelData)))

/** Get free space on an output port. */
#define sp_get_free( kernel, out_port ) \
    (sp_get_private(kernel)->get_free)(out_port)

/** Allocate space on an output port (blocks if necessary). */
#define sp_allocate( kernel, out_port ) \
    (sp_get_private(kernel)->allocate)(out_port)

/** Send data on an output port. */
#define sp_send( kernel, out_port ) \
    (sp_get_private(kernel)->send)(out_port)

/** Get available data on an input port. */
#define sp_get_available( kernel, in_port ) \
    (sp_get_private(kernel)->get_available)(in_port)

/** Read a value from an input port (blocks if necessary). */
#define sp_read_value( kernel, in_port ) \
    (sp_get_private(kernel)->read_value)(in_port)

/** Release data on an input port. */
#define sp_release( kernel, in_port ) \
    (sp_get_private(kernel)->release)(in_port)

/** Get the instance ID for a kernel. */
#define sp_get_instance( kernel ) \
    (sp_get_private(kernel)->instance)

/** Create a function to read a value from an input port. */
#define SP_READ_FUNCTION( RTYPE, KTYPE, port ) \
    static inline RTYPE sp_read_input ## port ( struct KTYPE *kernel ) { \
        RTYPE result = *(RTYPE*)sp_read_value( kernel, port ); \
        sp_release(kernel, port); \
        return result; \
    }

/** Method to get CPU ticks. */
static inline uint64_t sp_get_ticks()
{
#ifdef __x86_64
    uint64_t a, d;
    asm volatile("rdtsc" : "=A"(a), "=d"(d));
    return (d << 32) | (a & 0xFFFFFFFF);
#else
    static uint64_t ticks = 0;
    ticks += 1;
    return ticks;
#endif
}

/** Struct for keeping track of kernel execution time. */
typedef struct {
    uint64_t start_ticks;   /**< Start ticks of the last invocation. */
    uint64_t total_ticks;   /**< Total number of ticks so far. */
    uint64_t count;         /**< Number of invocations. */
} SPC;

/** Initialize an SPC structure. */
static inline void spc_init(SPC *c)
{
   c->start_ticks = 0;
   c->total_ticks = 0;
   c->count = 0;
}

/** Set the start ticks. */
static inline void spc_start(SPC *c)
{
   c->start_ticks = sp_get_ticks();
}

/** Update the total. */
static inline void spc_stop(SPC *c)
{
   const uint64_t t = sp_get_ticks();
   c->total_ticks += t - c->start_ticks;
}

/** Ring buffer used for queues between kernels. */
#define SPQ_COOKIE 0x1337
#define SPQ_FLAG_CLOSED    (1 << 0)

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

    uint32_t depth;     /**< Number of items that can be put in the queue. */
    uint32_t width;     /**< Number of bytes for each element. */

    uint8_t pad4[8];    /**< Make this structure 16-byte aligned. */
    char data[0];
} SPQ;

/** Initialize the queue.
 * The memory for the queue must have already been allocated.
 */
static inline void spq_init(SPQ *q, uint32_t depth, uint32_t width)
{
    q->flags = 0;
    q->read_ptr = 0;
    q->write_ptr = 0;
    q->wrap_ptr = 0;
    q->depth = depth;
    q->width = width;
    q->cookie = SPQ_COOKIE;
}

/** Mark the queue as closed. */
static inline void spq_close(SPQ *q)
{
    q->flags |= SPQ_FLAG_CLOSED;
}

/** Determine how many bytes are needed for the specified queue. */
static inline size_t spq_get_size(uint32_t depth, uint32_t width)
{
    return sizeof(SPQ) + depth * width;
}

/** Determine if the queue is valid. */
static inline int spq_is_valid(SPQ *q)
{
    return q->cookie == SPQ_COOKIE;
}

/** Determine if the queue has been closed. */
static inline int spq_is_closed(SPQ *q)
{
    return (q->flags & SPQ_FLAG_CLOSED) != 0;
}

/** Determine if the queue is empty. */
static inline int spq_is_empty(SPQ *q)
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
static inline int spq_get_free(SPQ *q)
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
static inline int spq_get_used(SPQ *q)
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
static inline int spq_start_write_offset(SPQ *q, uint32_t count)
{

    uint32_t wrap_needed = 0;
    uint32_t start;
    uint32_t end;
    uint32_t read_ptr;
    uint32_t wrap_ptr;

    start     = q->write_ptr;
    end        = start + count;
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
    if(    (start < read_ptr && read_ptr <= end)
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
static inline char *spq_start_write(SPQ *q, uint32_t count)
{
    const int offset = spq_start_write_offset(q, count);
    if (offset >= 0) {
        return &q->data[offset * q->width];
    } else {
        return NULL;
    }
}

/** Get a buffer for writing (blocking version). */
static inline char *spq_start_blocking_write(SPQ *q, uint32_t count)
{
    for(;;) {
        char *ptr = spq_start_write(q, count);
        if(ptr != NULL) {
            return ptr;
        }
        sched_yield();
    }
}

/** Finish a write. */
static inline void spq_finish_write(SPQ *q, uint32_t count)
{
    q->write_ptr += count;
}

/** Start a read.
 * This function does not block.  It returns the number of items available.
 * The offset parameter is in items.
 */
static inline uint32_t spq_start_read_offset(SPQ *q, int *offset)
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

/** Start a read. */
static inline uint32_t spq_start_read(SPQ *q, char **buffer)
{
    int offset = 0;
    const uint32_t count = spq_start_read_offset(q, &offset);
    *buffer = &q->data[offset * q->width];
    return count;
}


/** Start a read (blocking version). */
static inline uint32_t spq_start_blocking_read(SPQ *q, char **buffer)
{
    for(;;) {
        const uint32_t rc = spq_start_read(q, buffer);
        if(rc > 0) {
            return rc;
        }
        sched_yield();
    }
}

/** Finish a read. */
static inline void spq_finish_read(SPQ *q, uint32_t count)
{
    q->read_ptr += count;
}

/** Compute a square root. */
#define SP_SQRT_FUNC(NAME, TYPE) \
   static inline TYPE NAME(TYPE v) {  \
      TYPE guess = 1; \
      TYPE last = 0; \
      while (last != guess) { \
         last = guess; \
         guess = (guess + v / guess) / 2; \
      } \
      return guess; \
   }
SP_SQRT_FUNC(sp_sqrt8, int8_t)
SP_SQRT_FUNC(sp_sqrt16, int16_t)
SP_SQRT_FUNC(sp_sqrt32, int32_t)
SP_SQRT_FUNC(sp_sqrt64, int64_t)

#ifdef __cplusplus
}
#endif

#endif
