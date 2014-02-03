/** TimeTrial Agent. */

#include "TimeTrial.hh"
#include "Measure.hh"
#include "Stat.hh"

/** Mapping of stat types to factory methods. */
static struct StatMap {
    uint16_t type;
    Stat *(*create)(uint16_t id, const char *name, FILE *fd);
} g_stat_map[] = {
    { TTA_STAT_AVG,     &StatAvg::Create      },
    { TTA_STAT_MIN,     &StatMin::Create      },
    { TTA_STAT_MAX,     &StatMax::Create      },
    { TTA_STAT_SUM,     &StatSum::Create      },
    { TTA_STAT_HIST,    &StatHist::Create     },
    { TTA_STAT_TRACE,   &StatTrace::Create    },
    { 0,                NULL                  }
};

/** Mapping of measure types to factory methods. */
static struct MeasureMap {
    uint16_t type;
    Measure *(*create)(Stat *stat, bool hw);
} g_measure_map[] = {
    { TTA_MEASURE_RATE,             &MeasureRate::Create            },
    { TTA_MEASURE_UTILIZATION,      &MeasureUtil::Create            },
    { TTA_MEASURE_OCCUPANCY,        &MeasureOccupancy::Create       },
    { TTA_MEASURE_LATENCY,          &MeasureLatency::Create         },
    { TTA_MEASURE_BACKPRESSURE,     &MeasureBackpressure::Create    },
    { TTA_MEASURE_INTERPUSH,        &MeasureInterPush::Create       },
    { TTA_MEASURE_INTERPOP,         &MeasureInterPop::Create        },
    { 0,                            NULL                            }
};

/** Static method to start the thread. */
void *TimeTrialAgent::StartThread(void *arg)
{
    reinterpret_cast<TimeTrialAgent*>(arg)->Run();
    return NULL;
}

/** Constructor. */
TimeTrialAgent::TimeTrialAgent(const size_t buffer_size,
                               const uint64_t affinity,
                               const char *filename) :

    m_buffer_size(buffer_size),
    m_affinity(affinity),
    m_filename(filename),
    m_should_stop(false),
    m_buffers(NULL)

{

    /* Get a rough estimate of how many CPU ticks are in a second. */
    m_ticks_per_second = sp_get_ticks();
    sleep(1);
    m_ticks_per_second = sp_get_ticks() - m_ticks_per_second;
    m_ticks_per_ns = (double)m_ticks_per_second / 1000000000.0;
    m_start_ticks = sp_get_ticks();

    pthread_key_create(&m_tls_key, NULL);
    pthread_create(&m_tid, NULL, StartThread, this);
}

/** Destructor. */
TimeTrialAgent::~TimeTrialAgent()
{

    m_should_stop = true;
    pthread_join(m_tid, NULL);
    pthread_key_delete(m_tls_key);

    while(m_buffers) {
        TTABuffer *next = m_buffers->next;
        delete [] m_buffers->q;
        delete m_buffers;
        m_buffers = const_cast<TTABuffer*>(next);
    }

}

/** Get a buffer for writing. */
TTAEntry *TimeTrialAgent::StartWrite(uint32_t size)
{
    SPQ *q = reinterpret_cast<SPQ*>(pthread_getspecific(m_tls_key));
    if(SPUNLIKELY(q == NULL)) {
        const size_t qsize = spq_get_size(m_buffer_size, 1);
        q = reinterpret_cast<SPQ*>(new char[qsize]);
        spq_init(q, m_buffer_size, 1);
        pthread_setspecific(m_tls_key, q);
        TTABuffer *buf = new TTABuffer;
        buf->q = q;
        buf->next = const_cast<TTABuffer*>(m_buffers);
        m_buffers = buf;
    }
    const uint32_t total_size = sizeof(TTAEntry) + size;
    char *data = spq_start_blocking_write(q, total_size);
    TTAEntry *entry = reinterpret_cast<TTAEntry*>(data);
    entry->data_length = size;
    return entry;
}

/** Mark that we are finished with a write. */
void TimeTrialAgent::FinishWrite(TTAEntry *entry)
{
    SPQ *q = reinterpret_cast<SPQ*>(pthread_getspecific(m_tls_key));
    const uint32_t bytes = sizeof(TTAEntry) + entry->data_length;
    spq_finish_write(q, bytes);
}

/** Send a startup message. */
void TimeTrialAgent::SendStart(const uint16_t tap_id,
                               const uint16_t stat_type,
                               const uint16_t measure_type,
                               const uint32_t queue_depth,
                               const char *name)
{

    // Get the start time.
    const uint64_t time_ns = GetTime();

    // Get a buffer.
    const size_t data_length = sizeof(TTAStartup) - sizeof(TTAEntry);
    TTAEntry *entry = StartWrite(data_length);

    // Write the data.
    TTAStartup *startup     = reinterpret_cast<TTAStartup*>(entry);
    entry->tap_id           = tap_id;
    entry->type             = TTA_TYPE_START;
    entry->value            = 0;
    entry->time_ns          = time_ns;
    startup->queue_depth    = queue_depth;

    strncpy(startup->name, name, sizeof(startup->name));
    startup->stat_type      = stat_type;
    startup->measure_type   = measure_type;

    // Finish the write.
    FinishWrite(entry);

}

/** Log an event. */
void TimeTrialAgent::LogEvent(const uint16_t tap_id,
                              const uint16_t type,
                              const uint64_t value)
{

    // Get the time stamp first.
    const uint64_t time_ns = GetTime();

    // Get a buffer.
    TTAEntry *entry = StartWrite(0);

    // Write the entry.
    entry->tap_id       = tap_id;
    entry->type         = type;
    entry->time_ns      = time_ns;
    entry->value        = value;

    // Finish the write.
    FinishWrite(entry);

}


/** Read a buffer entry. */
TTAEntry *TimeTrialAgent::ReadEntry(SPQ *q)
{
    char *data;
    const uint32_t size = spq_start_read(q, &data);
    if(size < sizeof(TTAEntry)) {
        return NULL;
    }
    TTAEntry *entry = reinterpret_cast<TTAEntry*>(data);
    if(SPUNLIKELY(size < sizeof(TTAEntry) + entry->data_length)) {
        return NULL;
    }
    return entry;
}

/** Mark that we are finished with a read. */
void TimeTrialAgent::FinishRead(SPQ *q, TTAEntry *entry)
{
    const uint32_t bytes = sizeof(TTAEntry) + entry->data_length;
    spq_finish_read(q, bytes);
}

/** Thread body. */
void TimeTrialAgent::Run()
{

    // TODO: Set thread affinity.

    // Open the file.
    FILE *fd = stdout;
    if(m_filename != NULL) {
        fd = fopen(m_filename, "wb");
        if(fd == NULL) {
            fprintf(stderr, "ERROR: could not open file %s for output\n",
                    m_filename);
            exit(-1);
        }
    }

    // Loop processing the buffers.
    uint16_t stat_id = 0;
    typedef std::multimap<uint16_t, Measure*>::const_iterator MI;
    typedef std::list<SPQ*>::const_iterator BI;
    uint64_t last_ticks = 0;
    bool data_since_update = false;
    while(SPLIKELY(!m_should_stop)) {

        const uint64_t ticks = sp_get_ticks();

        // Perform per-second updates.
        if(ticks - last_ticks >= m_ticks_per_second) {
            if(data_since_update) {
                for(MI it = m_measures.begin(); it != m_measures.end(); ++it) {
                    it->second->Tick(1.0);
                }
                data_since_update = false;
            }
            last_ticks = ticks;
        }

        // Process data from the buffers.
        bool got_data = false;
        for(TTABuffer *buf = const_cast<TTABuffer*>(m_buffers);
            buf != NULL;
            buf = buf->next) {

            TTAEntry *entry = ReadEntry(buf->q);
            if(entry != NULL) {
                const uint16_t tap = entry->tap_id;

                // Handle startup messages.
                if(entry->type == TTA_TYPE_START) {

                    const TTAStartup *startup
                        = reinterpret_cast<const TTAStartup*>(entry);

                    // Create the stat object.
                    const uint16_t id = stat_id;
                    stat_id += 1;
                    Stat *s = NULL;
                    for(int x = 0; g_stat_map[x].create != NULL; x++) {
                        if(g_stat_map[x].type == startup->stat_type) {
                            s = (g_stat_map[x].create)(id, startup->name, fd);
                            break;
                        }
                    }
                    if(SPUNLIKELY(s == NULL)) {
                        fprintf(stderr, "ERROR: invalid stat type: %hu\n",
                                startup->stat_type);
                        exit(-1);
                    }

                    // Create the measure object.
                    Measure *m = NULL;
                    bool hw = false;
                    for(int x = 0; g_measure_map[x].create != NULL; x++) {
                        if(g_measure_map[x].type == startup->measure_type) {
                            m = (g_measure_map[x].create)(s, hw);
                            break;
                        }
                    }
                    if(SPUNLIKELY(m == NULL)) {
                        fprintf(stderr, "ERROR: invalid measure type: %hu\n",
                                startup->measure_type);
                        exit(-1);
                    }

                    // Tell the stat object to start.
                    s->Start();

                    // Insert the measure into our mapping.
                    m_measures.insert(std::make_pair(tap, m));

                }

                // Look up the measure for this tap.
                std::pair<MI, MI> range = m_measures.equal_range(tap);
                for(MI mit = range.first; mit != range.second; mit++) {
                    mit->second->ProcessEvent(entry);
                }
                if(SPUNLIKELY(range.first == range.second)) {
                    fprintf(stderr, "WARN: no measure for tap %hu\n", tap);
                }

                FinishRead(buf->q, entry);
                got_data = true;

            }
        }
        if(!got_data) {
            fflush(fd);
            sched_yield();
        } else {
            data_since_update = true;
        }

    }

    // Destroy measures.
    for(MI mit = m_measures.begin(); mit != m_measures.end(); ++mit) {
        mit->second->Stop(1.0);
        delete mit->second;
    }

    // Close the file.
    if(m_filename) {
        fclose(fd);
    }

}
