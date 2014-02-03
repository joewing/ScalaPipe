#ifndef MEASURE_HH_
#define MEASURE_HH_

#include "TimeTrial.hh"
#include "Stat.hh"

#include <list>
#include <map>

class Measure {
public:

    Measure(Stat *stat, bool hardware) :
        m_stat(stat),
        m_hardware(hardware),
        m_software(!hardware)
    {
    }

    virtual ~Measure()
    {
    }

    virtual void Stop(double diff)
    {
        m_stat->Stop();
    }

    /** This function is called once per second. */
    virtual void Tick(double diff)
    {
        if(!m_hardware) {
            m_stat->Log();
        }
    }

    void ProcessEvent(const TTAEntry *entry)
    {
        switch(entry->type) {
        case TTA_TYPE_QUIT:
            ProcessQuit(entry);
            break;
        case TTA_TYPE_PUSH:
            ProcessPush(entry);
            break;
        case TTA_TYPE_POP:
            ProcessPop(entry);
            break;
        case TTA_TYPE_FULL:
            ProcessFull(entry);
            break;
        case TTA_TYPE_START:
            ProcessStart((const TTAStartup*)entry);
            break;
        case TTA_TYPE_HIST:
            ProcessHist(entry);
            break;
        case TTA_TYPE_HPUSH:
            ProcessHPush(entry);
            break;
        case TTA_TYPE_HPOP:
            ProcessHPop(entry);
            break;
        case TTA_TYPE_HFULL:
            ProcessHFull(entry);
            break;
        case TTA_TYPE_HINTERPUSH:
            ProcessHInterPush(entry);
            break;
        case TTA_TYPE_HINTERPOP:
            ProcessHInterPop(entry);
            break;
        default:
            break;
        }
    }

protected:

    virtual void ProcessStart(const TTAStartup *start)
    {
    }

    virtual void ProcessPush(const TTAEntry *entry)
    {
    }

    virtual void ProcessPop(const TTAEntry *entry)
    {
    }

    virtual void ProcessFull(const TTAEntry *entry)
    {
    }

    virtual void ProcessQuit(const TTAEntry *entry)
    {
    }

    virtual void ProcessHist(const TTAEntry *entry)
    {
    }

    virtual void ProcessHPush(const TTAEntry *entry)
    {
    }

    virtual void ProcessHPop(const TTAEntry *entry)
    {
    }

    virtual void ProcessHFull(const TTAEntry *entry)
    {
    }

    virtual void ProcessHInterPush(const TTAEntry *entry)
    {
    }

    virtual void ProcessHInterPop(const TTAEntry *entry)
    {
    }

    Stat *m_stat;
    const bool m_hardware;
    const bool m_software;

};

class MeasureRate : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureRate(stat, hw);
    }

    MeasureRate(Stat *stat, bool hw) : Measure(stat, hw)
    {
        stat->SetXLabel("Transfers per Second");
        m_count = 0;
    }

    virtual void Tick(double diff)
    {
        if(m_software) {
            m_stat->Record(0, m_count);
            m_stat->Log();
            m_count = 0;
        }
    }

protected:

    virtual void ProcessPush(const TTAEntry *entry)
    {
        if(m_software) {
            m_count += 1;
        }
    }

    virtual void ProcessHPush(const TTAEntry *entry)
    {
        if(m_hardware) {
            const uint64_t *values = (const uint64_t*)&entry[1];
            m_count += values[0];
            m_stat->Record(0, m_count);
        }
    }

private:

    uint64_t m_count;

};

class MeasureInterPush : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureInterPush(stat, hw);
    }

    MeasureInterPush(Stat *stat, bool hw) : Measure(stat, hw)
    {
        m_last_time = 0;
        stat->SetXLabel("Bin Number");
    }

protected:

    virtual void ProcessPush(const TTAEntry *entry)
    {
        if(m_software) {
            if(m_last_time > 0) {
                uint64_t value = (entry->time_ns - m_last_time) / 1000;
                if(value > 10000) {
                    value = 10000;
                }
                m_stat->Record(1, value);
            }
            m_last_time = entry->time_ns;
        }
    }

    virtual void ProcessHInterPush(const TTAEntry *entry)
    {
        if(m_hardware) {
            const uint64_t *values = (const uint64_t*)&entry[1];
            for(size_t offset = 0; offset < entry->data_length / 8; ++offset) {
                const uint64_t index = values[offset] >> 48;
                const uint64_t count = values[offset] & 0x0000FFFFFFFFFFFFULL;
                if(count > 0) {
                    m_stat->Record(count, index);
                }
            }
            m_stat->Log();
        }
    }

private:

    uint64_t m_last_time;

};

class MeasureInterPop : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureInterPop(stat, hw);
    }

    MeasureInterPop(Stat *stat, bool hw) : Measure(stat, hw)
    {
        m_last_time = 0;
        stat->SetXLabel("Bin Number");
    }

protected:

    virtual void ProcessPop(const TTAEntry *entry)
    {
        if(m_software) {
            // Note that we only measure pops when the queue is non-empty.
            if(entry->value == 0) {
                if(m_last_time > 0) {
                    uint64_t value = (entry->time_ns - m_last_time) / 1000;
                    if(value > 10000) {
                        value = 10000;
                    }
                    m_stat->Record(1, value);
                }
                m_last_time = entry->time_ns;
            }
        }
    }

    virtual void ProcessHInterPop(const TTAEntry *entry)
    {
        if(m_hardware) {
            const uint64_t *values = (const uint64_t*)&entry[1];
            for(size_t offset = 0; offset < entry->data_length / 8; ++offset) {
                const uint64_t index = values[offset] >> 48;
                const uint64_t count = values[offset] & 0x0000FFFFFFFFFFFFULL;
                if(count > 0) {
                    m_stat->Record(count, index);
                }
            }
            m_stat->Log();
        }
    }

private:

    uint64_t m_last_time;

};

class MeasureUtil : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureUtil(stat, hw);
    }

    MeasureUtil(Stat *stat, bool hw) : Measure(stat, hw)
    {
        stat->SetXLabel("% Utilization");
        m_count = 0;
        m_total = 0;
    }

    virtual void Tick(double diff)
    {
        if(m_software) {
            if(m_total > 0) {
                m_stat->Record(0, (100 * m_count) / m_total);
            } else {
                m_stat->Record(0, 0);
            }
            m_count = 0;
            m_total = 0;
        }
    }

protected:

    virtual void ProcessHPush(const TTAEntry *entry)
    {
        if(m_hardware) {
            const uint64_t *values = (const uint64_t*)&entry[1];
            m_count += values[0];
            m_total += values[1];
            m_stat->Record(0, (100 * m_count) / m_total);
            m_stat->Log();
        }
    }

private:

    uint64_t m_count;
    uint64_t m_total;

};

class MeasureOccupancy : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureOccupancy(stat, hw);
    }

    MeasureOccupancy(Stat *stat, bool hw) : Measure(stat, hw)
    {
        stat->SetXLabel("Occupancy");
        m_depth = 0;
        m_time_map[0] = 0;
    }

    virtual void Stop(double diff)
    {

        Replay();

        // Note that there shouldn't be any pushes left here.

        while(!m_pops.empty() && m_depth > 0) {
            const uint64_t t = m_pops.front();
            m_pops.pop_front();

            const uint64_t current = t - m_time_map[m_depth];
            m_depth -= 1;
            m_time_map[m_depth] = t;

            m_stat->Record(current, m_depth);

        }

        Measure::Stop(diff);

    }

protected:

    virtual void ProcessPush(const TTAEntry *entry)
    {
        if(m_software) {
            m_pushes.push_back(entry->time_ns);
            Replay();
        }
    }

    virtual void ProcessPop(const TTAEntry *entry)
    {
        if(m_software) {
            m_pops.push_back(entry->time_ns);
            Replay();
        }
    }

    virtual void ProcessHist(const TTAEntry *entry)
    {
        if(m_hardware) {
            const uint64_t *values = (const uint64_t*)&entry[1];
            for(size_t offset = 0; offset < entry->data_length / 8; ++offset) {
                const uint64_t index = values[offset] >> 48;
                const uint64_t count = values[offset] & 0x0000FFFFFFFFFFFFULL;
                if(count > 0) {
                    m_stat->Record(count, index);
                }
            }
            m_stat->Log();
        }
    }

private:

    void Replay()
    {

        // While the queues can be changing, we need both
        // a push and a pop to be present.
        while(!m_pushes.empty() && !m_pops.empty()) {

            const uint64_t push = m_pushes.front();
            const uint64_t pop  = m_pops.front();
            const uint64_t t = m_time_map[m_depth];
            uint64_t current = 0;
            if(pop < push) {

                // Process the pop.
                m_pops.pop_front();

                if(m_depth > 0) {
                    if(t > 0) {
                        current = pop - t;
                        m_stat->Record(current, m_depth);
                    }
                    m_depth -= 1;
                    m_time_map[m_depth] = pop;
                }

            } else {

                // Process the push.
                m_pushes.pop_front();

                if(t > 0) {
                    current = push - t;
                    m_stat->Record(current, m_depth);
                }
                m_depth += 1;
                m_time_map[m_depth] = push;

            }


        }
    }

    std::list<uint64_t> m_pushes;
    std::list<uint64_t> m_pops;
    std::map<uint64_t, uint64_t> m_time_map;
    uint64_t m_depth;

};

class MeasureLatency : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureLatency(stat, hw);
    }

    MeasureLatency(Stat *stat, bool hw) : Measure(stat, hw)
    {
        stat->SetXLabel("Nanoseconds");
        m_full = 0;
    }

protected:

    virtual void ProcessFull(const TTAEntry *entry)
    {
        if(m_software) {
            m_pushes.push_back(entry->time_ns);
            m_full += 1;
        }
    }

    virtual void ProcessPush(const TTAEntry *entry)
    {
        if(m_software) {
            if(m_full == 0) {
                m_pushes.push_back(entry->time_ns);
            }
        }
    }

    virtual void ProcessPop(const TTAEntry *entry)
    {
        if(m_software) {
            if(!m_pushes.empty()) {
                const uint64_t push_time = m_pushes.front();
                m_pushes.pop_front();
                const uint64_t diff_time = entry->time_ns - push_time;
                if(m_full > 0) {
                    m_full -= 1;
                }
                m_stat->Record(0, diff_time);
            }
        }
    }

private:

    std::list<uint64_t> m_pushes;
    uint64_t m_full;

};

class MeasureBackpressure : public Measure {
public:

    static Measure *Create(Stat *stat, bool hw)
    {
        return new MeasureBackpressure(stat, hw);
    }

    MeasureBackpressure(Stat *stat, bool hw) : Measure(stat, hw)
    {
        stat->SetXLabel("% Time");
        m_start_time = 0;
        m_full_time = 0;
        m_total_full_time = 0;
    }

    virtual void Tick(double diff)
    {

        if(m_software) {

            const uint64_t now = sp_get_ticks();

            if(m_full_time > 0) {
                m_total_full_time += now - m_full_time;
                m_full_time = now;
            }

            const double percent = (100.0 * m_total_full_time)
                                        / (now - m_start_time);
            m_stat->Record(0, (uint64_t)(percent + 0.5));

            m_total_full_time = 0;
            m_start_time = now;

        }

    }

protected:

    virtual void ProcessStart(const TTAStartup *entry)
    {
        if(m_software) {
            m_start_time = sp_get_ticks();
        }
    }

    virtual void ProcessFull(const TTAEntry *entry)
    {
        if(m_software) {
            m_full_time = sp_get_ticks();
        }
    }

    virtual void ProcessPush(const TTAEntry *entry)
    {
        if(m_software) {
            if(m_full_time > 0) {
                const uint64_t now = sp_get_ticks();
                const uint64_t full_duration = now - m_full_time;
                m_total_full_time += full_duration;
                m_full_time = 0;
            }
        }
    }

    virtual void ProcessHFull(const TTAEntry *entry)
    {
        if(m_hardware) {
            const uint64_t *values = (const uint64_t*)&entry[1];
            const double percent = (100.0 * values[0]) / values[1];
            m_stat->Record(0, (uint64_t)percent);
            m_stat->Log();
        }
    }

private:

    uint64_t m_start_time;
    uint64_t m_full_time;
    uint64_t m_total_full_time;

};

#endif
