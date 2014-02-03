/** Time Trial Agent. */

#ifndef TimeTrial_HH_
#define TimeTrial_HH_

#include "ScalaPipe.h"

#include <pthread.h>
#include <unistd.h>
#include <map>

#define TTA_COOKIE          0x1337
#define TTA_BUFFER_COOKIE   (TTA_COOKIE + 1)
#define TTA_ENTRY_COOKIE    (TTA_BUFFER_COOKIE + 1)

#define TTA_TYPE_QUIT       0   // No more data for this tap.
#define TTA_TYPE_PUSH       1   // Push a value on to a queue.
#define TTA_TYPE_POP        2   // Pop a value off of a queue.
#define TTA_TYPE_FULL       3   // Full queue.
#define TTA_TYPE_START      4   // Start data for this tap.
#define TTA_TYPE_HIST       5   // Queue occupancy histogram from hardware.
#define TTA_TYPE_HPUSH      6   // Hardware pushes.
#define TTA_TYPE_HPOP       7   // Hardware pops.
#define TTA_TYPE_HFULL      8   // Hardware fulls.
#define TTA_TYPE_HINTERPUSH 9   // Interpush histogram from hardware.
#define TTA_TYPE_HINTERPOP  10  // Interpop histogram from hardware.

#define TTA_STAT_AVG    0
#define TTA_STAT_MIN    1
#define TTA_STAT_MAX    2
#define TTA_STAT_SUM    3
#define TTA_STAT_HIST   4
#define TTA_STAT_TRACE  5

#define TTA_MEASURE_RATE            0
#define TTA_MEASURE_UTILIZATION     1
#define TTA_MEASURE_OCCUPANCY       2
#define TTA_MEASURE_LATENCY         3
#define TTA_MEASURE_BACKPRESSURE    4
#define TTA_MEASURE_VALUE           5
#define TTA_MEASURE_INTERPUSH       6
#define TTA_MEASURE_INTERPOP        7

/** Buffer entry. */
struct TTAEntry {
    uint16_t type;
    uint16_t tap_id;
    uint32_t data_length;
    uint64_t time_ns;
    uint64_t value;
};

/** Event for describing an edge to be measured. */
struct TTAStartup {
    TTAEntry header;
    char name[64];          /**< Name of the edge. */
    uint16_t stat_type;     /**< Statistic to be collected. */
    uint16_t measure_type;  /**< What to measure. */
    uint32_t queue_depth;   /**< Depth of the queue. */
};

struct TTABuffer {
    SPQ *q;
    struct TTABuffer *next;
};

class Measure;

class TimeTrialAgent {
public:

    /** Constructor.
     * @param buffer_size The size of each buffer in bytes.
     * @param affinity Thread affinity mask.
     */
    TimeTrialAgent(const size_t buffer_size,
                   const uint64_t affinity,
                   const char *filename);

    /** Destructor. */
    ~TimeTrialAgent();

    /** Send a startup message. */
    void SendStart(const uint16_t tap_id,
                   const uint16_t stat_type,
                   const uint16_t measure_type,
                   const uint32_t queue_depth,
                   const char *name);

    /** Log an event. */
    void LogEvent(const uint16_t tap_id,
                  const uint16_t type,
                  const uint64_t value = 0);

private:

    /** Get time since startup in nanoseconds. */
    uint64_t GetTime() const
    {
        const uint64_t ticks = sp_get_ticks() - m_start_ticks;
        return (uint64_t)(ticks * m_ticks_per_ns);
    }

    /** Read a buffer entry.
     * Note that FinishRead must be called before calling this again.
     * @param q The queue to read.
     * @return A pointer to the entry (NULL if none are available).
     */
    TTAEntry *ReadEntry(SPQ *q);

    /** Mark that we are finished with a read. */
    void FinishRead(SPQ *q, TTAEntry *entry);

    /** Get a buffer for writing.
     * Note that FinishWrite must be called before calling this again.
     * @param buffer The buffer to use.
     * @param data_length The data length in bytes (may be zero).
     * @return A pointer to an entry or NULL if there is no room.
     */
    TTAEntry *StartWrite(uint32_t size);

    /** Mark that we are finished with a write. */
    void FinishWrite(TTAEntry *entry);

    /** The thread body. */
    void Run();

    /** Static method to start the thread. */
    static void *StartThread(void *arg);

    const size_t    m_buffer_size;
    const uint64_t  m_affinity;
    const char     *m_filename;
    volatile bool   m_should_stop;

    uint64_t        m_start_ticks;
    uint64_t        m_ticks_per_second;
    double          m_ticks_per_ns;
    pthread_key_t   m_tls_key;
    pthread_t       m_tid;

    // Per-thread buffers.
    volatile TTABuffer *m_buffers;

    // Mapping of tap IDs to measure objects.
    std::multimap<uint16_t, Measure*> m_measures;


};

#endif /* TimeTrial_HH_ */
