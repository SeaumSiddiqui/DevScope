package com.devscope.store;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * In-memory store for everything DevScope captures.
 *
 * Two fixed-size ring buffers, one for errors and one for log lines. When
 * one is full the oldest entry is dropped, so memory stays flat no matter
 * how long the app runs.
 *
 * Many request threads log at the same time, so access goes through a
 * ReentrantReadWriteLock: reads can run together, writes are exclusive.
 */
public class ErrorEventStore {
    // same exception on the same thread within this window is treated as a duplicate
    private static final long DUPLICATE_WINDOW_MS = 500;

    private final int maxErrors;
    private final int maxLogs;

    // ArrayDeque: O(1) add to tail and remove from head, which is all a ring buffer needs
    private final Deque<ErrorEvent> errorEvents;
    private final Deque<LogEntry> logEntries;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ErrorEventStore(int maxErrors, int maxLogs) {
        this.maxErrors = maxErrors;
        this.maxLogs = maxLogs;
        this.errorEvents = new ArrayDeque<>(maxErrors);
        this.logEntries = new ArrayDeque<>(maxLogs);
    }

    // Write operations

    /**
     * Rejects the event if the same exception class + message on the same
     * thread was stored less than 500ms ago. The servlet container can send
     * an error request through the filter chain a second time.
     */
    public void addError(ErrorEvent event) {
        lock.writeLock().lock();

        try {
            boolean isDuplicate = errorEvents.stream()
                    .anyMatch(existing -> existing.getThreadName().equals(event.getThreadName())
                            && existing.getExceptionClass().equals(event.getExceptionClass())
                            && Objects.equals(existing.getExceptionMessage(), event.getExceptionMessage())
                            && Math.abs(existing.getTimestamp() - event.getTimestamp()) < DUPLICATE_WINDOW_MS);

            if (isDuplicate) return;

            if (errorEvents.size() >= maxErrors) errorEvents.pollFirst(); // remove oldest
            errorEvents.addLast(event);
        } finally {
            // always unlock in finally, otherwise one failed write blocks every write after it
            lock.writeLock().unlock();
        }
    }

    public void addLogs(LogEntry entry) {
        lock.writeLock().lock();

        try {
            if (logEntries.size() >= maxLogs) logEntries.pollFirst();
            logEntries.addLast(entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Read operations

    // stored oldest first, returned newest first so the UI shows the latest on top
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
     * Newest first. level is optional ("ERROR", "WARN", ...), case-insensitive,
     * null or blank returns everything.
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
     * The last `limit` log lines from one thread, oldest to newest.
     * Used to attach the lines that ran right before an exception.
     */
    public List<LogEntry> getRecentLogsByThread(String threadName, int limit) {
        lock.readLock().lock();

        try {
            List<LogEntry> list = new ArrayList<>(logEntries);
            Collections.reverse(list); // newest first so limit() keeps the latest ones

            List<LogEntry> result = list.stream()
                    .filter(e -> threadName.equals(e.getThreadName()))
                    .limit(limit)
                    .collect(Collectors.toList());

            Collections.reverse(result); // back to timeline order
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

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

        try {
            return errorEvents.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getLogCount() {
        lock.readLock().lock();

        try {
            return logEntries.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
