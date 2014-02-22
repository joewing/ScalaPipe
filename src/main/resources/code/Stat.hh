#ifndef STAT_HH_
#define STAT_HH_

#include <stdio.h>
#include <string.h>
#include <stdint.h>

#include <vector>
#include <set>

class Stat {
public:

    Stat(uint16_t id, const char *name, FILE *fd) :
        m_fd(fd),
        m_tap_id(id),
        m_xlabel(NULL)
    {
        m_name = strdup(name);
    }

    virtual ~Stat()
    {
        free(m_name);
        if(m_xlabel != NULL) {
            free(m_xlabel);
        }
    }

    virtual const char *GetType() const = 0;

    virtual void Record(uint64_t time_ns, uint64_t value) = 0;

    virtual void Log()
    {
        // Do nothing by default.
    }

    virtual void Start()
    {
        fprintf(m_fd, "s,%hu,%s,%s,%s\n", m_tap_id, GetType(), m_name,
                m_xlabel == NULL ? "" : m_xlabel);
    }

    virtual void Stop()
    {
        Log();
    }

    void SetXLabel(const char *xlabel)
    {
        if(m_xlabel != NULL) {
            free(m_xlabel);
        }
        m_xlabel = strdup(xlabel);
    }

protected:

    void Print(uint64_t frame, uint64_t index, uint64_t value)
    {
        fprintf(m_fd, "d,%hu,%lu,%lu,%lu\n", m_tap_id, frame, index, value);
    }

    FILE *m_fd;
    const uint16_t m_tap_id;
    char *m_name;
    char *m_xlabel;

};

class StatAvg : public Stat {
public:

    static Stat *Create(uint16_t id, const char *name, FILE *fd)
    {
        return new StatAvg(id, name, fd);
    }

    StatAvg(uint16_t id, const char *name, FILE *fd) : Stat(id, name, fd)
    {
        m_sum     = 0;
        m_count  = 0;
        m_index  = 0;
    }

    virtual const char *GetType() const
    {
        return "avg";
    }

    virtual void Record(uint64_t time_ns, uint64_t value)
    {
        m_sum += value;
        m_count += 1;
    }

    virtual void Log()
    {
        const uint64_t avg = m_count > 0 ? m_sum / m_count : 0;
        Print(m_index, m_index, avg);
        m_count = 0;
        m_sum = 0;
        m_index += 1;
    }

private:

    uint64_t m_sum;
    uint64_t m_count;
    uint64_t m_index;

};

class StatMin : public Stat {
public:

    static Stat *Create(uint16_t id, const char *name, FILE *fd)
    {
        return new StatMin(id, name, fd);
    }

    StatMin(uint16_t id, const char *name, FILE *fd) : Stat(id, name, fd)
    {
        m_current    = 0;
        m_index      = 0;
    }

    virtual const char *GetType() const
    {
        return "min";
    }

    virtual void Record(uint64_t time_ns, uint64_t value)
    {
        if(m_current < value) {
            m_current = value;
        }
    }

    virtual void Log()
    {
        Print(m_index, m_index, m_current);
        m_current = 0;
        m_index += 1;
    }

private:

    uint64_t m_current;
    uint64_t m_index;

};

class StatMax : public Stat {
public:

    static Stat *Create(uint16_t id, const char *name, FILE *fd)
    {
        return new StatMax(id, name, fd);
    }

    StatMax(uint16_t id, const char *name, FILE *fd) : Stat(id, name, fd)
    {
        m_current    = 0;
        m_index      = 0;
    }

    virtual const char *GetType() const
    {
        return "max";
    }

    virtual void Record(uint64_t time_ns, uint64_t value)
    {
        if(value > m_current) {
            m_current = value;
        }
    }

    virtual void Log()
    {
        Print(m_index, m_index, m_current);
        m_current = 0;
        m_index += 1;
    }

private:

    uint64_t m_current;
    uint64_t m_index;

};

class StatSum : public Stat {
public:

    static Stat *Create(uint16_t id, const char *name, FILE *fd)
    {
        return new StatSum(id, name, fd);
    }

    StatSum(uint16_t id, const char *name, FILE *fd) : Stat(id, name, fd)
    {
        m_sum     = 0;
        m_index  = 0;
    }

    virtual const char *GetType() const
    {
        return "sum";
    }

    virtual void Record(uint64_t time_ns, uint64_t value)
    {
        m_sum += value;
    }

    virtual void Log()
    {
        Print(m_index, m_index, m_sum);
        m_sum = 0;
        m_index += 1;
    }

private:

    uint64_t m_sum;
    uint64_t m_index;

};

class StatHist : public Stat {
public:

    static Stat *Create(uint16_t id, const char *name, FILE *fd)
    {
        return new StatHist(id, name, fd);
    }

    StatHist(uint16_t id, const char *name, FILE *fd) : Stat(id, name, fd)
    {
        m_buckets.reserve(1 << 16);
        m_frame = 0;
    }

    virtual const char *GetType() const
    {
        return "hist";
    }

    virtual void Record(uint64_t time_ns, uint64_t value)
    {
        if(value >= m_buckets.size()) {
            m_buckets.resize(value + 1, 0);
        }
        m_buckets[value] += time_ns;
    }

    virtual void Log()
    {
        for(size_t i = 0; i < m_buckets.size(); i++) {
            const uint64_t value = m_buckets[i];
            if(value > 0) {
                Print(m_frame, i, value);
                m_buckets[i] = 0;
            }
        }
        m_frame += 1;
    }

private:

    std::vector<uint64_t> m_buckets;
    uint64_t m_frame;

};

class StatTrace : public Stat {
public:

    static Stat *Create(uint16_t id, const char *name, FILE *fd)
    {
        return new StatTrace(id, name, fd);
    }

    StatTrace(uint16_t id, const char *name, FILE *fd) : Stat(id, name, fd)
    {
        m_index = 0;
    }

    virtual const char *GetType() const
    {
        return "trace";
    }

    virtual void Record(uint64_t time_ns, uint64_t value)
    {
        Print(m_index, m_index, value);
        m_index += 1;
    }

private:

    uint64_t m_index;

};

#endif
