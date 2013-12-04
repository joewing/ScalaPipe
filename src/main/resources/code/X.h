#ifndef __X_H_
#define __X_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <fcntl.h>

#ifdef __APPLE__
#include <sys/types.h>
#include <sys/sysctl.h>
#endif

/** XLIKELY/XUNLIKELY macros. */
#ifdef __GNUC__
#  if __GNUC__ >= 3
#     define XLIKELY(x)   __builtin_expect(!!(x), 1)
#     define XUNLIKELY(x) __builtin_expect(!!(x), 0)
#  else
#     define XLIKELY(x) (x)
#     define XUNLIKELY(x) (x)
#  endif
#else
#  define XLIKELY(x) (x)
#  define XUNLIKELY(x) (x)
#endif

/** Method to get CPU ticks. */
static inline uint64_t xrdtsc()
{
#ifdef __x86_64
   uint64_t a, d;
   asm volatile("rdtsc" : "=A"(a), "=d"(d));
   return (d << 32) | (a & 0xFFFFFFFF);
#else
   return 0;
#endif
}

/** Method to get CPU frequency. */
static inline uint64_t xgetfreq()
{
#if defined(__APPLE__)
   int name[2] = { CTL_HW, HW_CPU_FREQ };
   uint64_t freq = 2000000000;
   size_t len = sizeof(freq);
   int rc;
   rc = sysctl(name, 2, &freq, &len, NULL, 0);
   if(XUNLIKELY(rc < 0)) {
      fprintf(stderr, "Could not determine CPU frequency\n");
   }
   return freq;
#else
   return 2000000000;
#endif
}

/** Compute a square root. */
#define AP_SQRT_FUNC(NAME, TYPE) \
   static inline TYPE NAME(TYPE v) {  \
      TYPE guess = 1; \
      TYPE last = 0; \
      while (last != guess) { \
         last = guess; \
         guess = (guess + v / guess) / 2; \
      } \
      return guess; \
   }
AP_SQRT_FUNC(ap_sqrt8, int8_t)
AP_SQRT_FUNC(ap_sqrt16, int16_t)
AP_SQRT_FUNC(ap_sqrt32, int32_t)
AP_SQRT_FUNC(ap_sqrt64, int64_t)

/** Structure to represent an input port for a block.
 * This should align to 8 bytes.
 */
typedef struct {
   void *data;
   int   count;
   int   credit;
   char  stop;
   char  reserved[sizeof(void*) - 1];
} AP_input_port;

/** Structure to represent an output port for a block.
 * This should align to 8 bytes.
 */
typedef struct {
   void *data;
   int   count;
   int   credit;
   char  reserved[8 - sizeof(void*)];
} AP_output_port;

/** Structure to represent the per-instance fields for a block.
 * This should align to 8 bytes.
 */
typedef struct {

   int   (*get_free)(int out_port);
   void *(*allocate)(int out_port, int count);
   void  (*send)(int out_port, int count);
   void  (*release)(int in_port, int count);
   void  (*send_signal)(int out_port, int type, int value);

   int in_port_count;
   int out_port_count;

   int max_send_count;
   int instance;

   AP_input_port  *in_ports;
   AP_output_port *out_ports;

} AP_block_data;

/** Auto-Pipe types. */
typedef uint8_t      UNSIGNED8;
typedef uint16_t     UNSIGNED16;
typedef uint32_t     UNSIGNED32;
typedef uint64_t     UNSIGNED64;
typedef int8_t       SIGNED8;
typedef int16_t      SIGNED16;
typedef int32_t      SIGNED32;
typedef int64_t      SIGNED64;
typedef float        FLOAT32;
typedef double       FLOAT64;
typedef long double  FLOAT96;
typedef char        *STRING;

/** Get the internal Auto-Pipe block structure from the user structure. */
#define ap_get_internal( block ) \
   ((AP_block_data*)((char*)(block) - sizeof(AP_block_data)))

#define ap_get_max_send( block, out_port ) \
   (ap_get_internal(block)->max_send_count)

#define ap_get_free( block, out_port) \
   (ap_get_internal(block)->get_free)(out_port)

#define ap_allocate( block, out_port, count) \
   (ap_get_internal(block)->allocate)((out_port), (count))

#define ap_send(block, out_port, count) \
   (ap_get_internal(block)->send)((out_port), (count))

#define ap_release(block, in_port, count) \
   (ap_get_internal(block)->release)((in_port), (count))

#define ap_send_signal(block, out_port, type, value) \
   (ap_get_internal(block)->send_signal)((out_port), (type), (value))

#define ap_get_input_count(block, in_port) \
   (ap_get_internal(block)->in_ports[in_port].count)

#define ap_get_input_data(block, in_port) \
   (ap_get_internal(block)->in_ports[in_port].data)

#define ap_get_instance(block) \
   (ap_get_internal(block)->instance)

static inline int ap_check_inputs_upto(void *block, int in_port)
{
   const AP_block_data *bp = ap_get_internal(block);
   int i;
   for(i = 0; i < bp->in_port_count; i++) {
      if(bp->in_ports[i].data == NULL) {
         return 0;
      }
   }
   return 1;
}

/** Connect to a socket. */
static inline int ap_connect(int port)
{

   struct sockaddr_in addr;
   int fd;
   int rc;

   memset(&addr, 0, sizeof(addr));
   addr.sin_addr.s_addr = inet_addr("127.0.0.1");
   addr.sin_port = htons(port);
   addr.sin_family = AF_INET;

   fd = socket(AF_INET, SOCK_STREAM, 0);
   if (fd < 0) {
      return -1;
   }

   rc = connect(fd, (struct sockaddr*)&addr, sizeof(addr));
   if (rc < 0) {
      close(fd);
      return -1;
   }

   fcntl(fd, F_SETFL, fcntl(fd, F_GETFL) | O_NONBLOCK);

   return fd;

}

/** Accept a connection. */
static inline int ap_accept(int port)
{

   struct sockaddr_in addr;
   int server_fd;
   int client_fd;
   int rc;

   memset(&addr, 0, sizeof(addr));
   addr.sin_port = port;
   addr.sin_family = AF_INET;

   server_fd = socket(AF_INET, SOCK_STREAM, 0);
   if (server_fd < 0) {
      return -1;
   }

   rc = bind(server_fd, (struct sockaddr*)&addr, sizeof(addr));
   if (rc < 0) {
      close(server_fd);
      return -1;
   }

   rc = listen(server_fd, 1);
   if (rc < 0) {
      close(server_fd);
      return -1;
   }

   client_fd = accept(server_fd, NULL, NULL);
   close(server_fd);
   return client_fd;

}

#ifdef __cplusplus
}
#endif

#endif
