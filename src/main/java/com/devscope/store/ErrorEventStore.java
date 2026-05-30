package com.devscope.store;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Central in-memory store for all captured events.
 *
 * Holds two fixed-size ring buffers — one for ErrorEvents, one for LogEntries.
 * When either buffer reaches capacity the oldest entry is evicted automatically,
 * keeping memory usage flat regardless of how long the application runs.
 *
 * Registered as a singleton Spring bean. DevScopeRequestFilter writes errors
 * into it; DevScopeAppender writes log lines; DevScopeController reads both.
 *
 * Thread safety: ReentrantReadWriteLock allows multiple concurrent readers but
 * serialises writes. All lock releases happen in finally blocks so an exception
 * during any write never leaves the lock held.
 */
public class ErrorEventStore {
    private final int maxErrors;
    private final int maxLogs;

    // ArrayDeque gives O(1) add-to-tail and remove-from-head — the two
    // operations a ring buffer needs most. Faster than LinkedList for this
    // because it uses a resizable array with no per-node pointer overhead.
    private final Deque<ErrorEvent> errorEvents;
    private final Deque<LogEntry>   logEntries;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ErrorEventStore(int maxErrors, int maxLogs) {
        this.maxErrors = maxErrors;
        this.maxLogs = maxLogs;
        this.errorEvents = new ArrayDeque<>(maxErrors);
        this.logEntries = new ArrayDeque<>(maxLogs);
    }

    // Write operations

    /**
     * Stores a captured error event.
     *
     * Called by {@link com.devscope.web.DevScopeRequestFilter} each time it
     * catches an exception thrown by the application.
     *
     * Duplicate guard: events with the same exception class, message, and thread
     * arriving within 500ms are rejected. This prevents double-capture when the
     * servlet container re-dispatches an error request through the filter chain.
     */
    public void addError(ErrorEvent event) {
        lock.writeLock().lock();
        try {
            boolean isDuplicate = !errorEvents.isEmpty() && errorEvents.stream()
                    .anyMatch(existing ->
                            existing.getThreadName().equals(event.getThreadName())
                                    && existing.getExceptionClass().equals(event.getExceptionClass())
                                    && Objects.equals(existing.getExceptionMessage(), event.getExceptionMessage())
                                    && Math.abs(existing.getTimestamp() - event.getTimestamp()) < 500
                    );

            if (isDuplicate) return;

            if (errorEvents.size() >= maxErrors) errorEvents.pollFirst();
            errorEvents.addLast(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Stores a captured log line.
     *
     * Called by {@link com.devscope.appender.DevScopeAppender} for every log
     * event the application produces, across all levels.
     */
    public void addLogs(LogEntry entry) {
        lock.writeLock().lock();
        try {
            if (logEntries.size() >= maxLogs) logEntries.pollFirst();
            logEntries.addLast(entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Read Operations

    /** Returns all stored errors, newest first. */
    public List<ErrorEvent> getErrors() {
        lock.readLock().lock();
        try {
            List<ErrorEvent> list = new ArrayList<>(errorEvents);
            Collections.reverse(list);
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns stored log lines, newest first.
     *
     * @param level optional level filter ("ERROR", "WARN", "INFO", "DEBUG").
     *              Case-insensitive. Pass null to return all levels.
     */
    public List<LogEntry> getLogs(String level) {
        lock.readLock().lock();
        try {
            List<LogEntry> list = new ArrayList<>(logEntries);
            Collections.reverse(list);

            if (level != null && !level.isBlank()) {
                list = list.stream()
                        .filter(e -> e.getLevel().equalsIgnoreCase(level))
                        .collect(Collectors.toList());
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the most recent log lines from a specific thread, in chronological
     * order (oldest to newest).
     *
     * Called by {@link com.devscope.web.DevScopeRequestFilter} when building an
     * ErrorEvent — the returned lines become the error's correlated log context.
     *
     * @param threadName thread to filter by, e.g. "http-nio-8080-exec-3"
     * @param limit maximum number of entries to return
     */
    public List<LogEntry> getRecentLogsByThread(String threadName, int limit) {
        lock.readLock().lock();
        try {
            List<LogEntry> list = new ArrayList<>(logEntries);
            Collections.reverse(list); // newest first for efficient limiting

            return list.stream()
                    .filter(e -> threadName.equals(e.getThreadName()))
                    .limit(limit)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
                        Collections.reverse(result); // back to chronological for display
                        return result;
                    }));
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Wipes all stored errors and log lines. */
    public void clear() {
        lock.writeLock().lock();
        try {
            errorEvents.clear();
            logEntries.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getErrorCount() {
        lock.readLock().lock();
        try { return errorEvents.size(); }
        finally { lock.readLock().unlock(); }
    }

    public int getLogCount() {
        lock.readLock().lock();
        try { return logEntries.size(); }
        finally { lock.readLock().unlock(); }
    }
}
