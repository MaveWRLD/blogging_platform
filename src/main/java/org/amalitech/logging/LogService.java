package org.amalitech.logging;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class LogService {
    private final Map<String, LogEntry> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public LogEntry append(String level, String logger, String className, String methodName, String message, String args, String exception) {
        LogEntry e = new LogEntry();
        String id = String.valueOf(seq.getAndIncrement());
        e.setId(id);
        e.setTimestamp(Instant.now());
        e.setLevel(level);
        e.setLogger(logger);
        e.setClassName(className);
        e.setMethodName(methodName);
        e.setMessage(message);
        e.setArgs(args);
        e.setException(exception);
        store.put(id, e);
        return e;
    }

    public List<LogEntry> list(int limit) {
        return store.values().stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Optional<LogEntry> get(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public void clear() {
        store.clear();
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }
}
