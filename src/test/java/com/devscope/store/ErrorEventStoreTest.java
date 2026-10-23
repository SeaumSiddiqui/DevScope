package com.devscope.store;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEventStoreTest {

    @Test
    void dropsOldestErrorWhenFull() {
        ErrorEventStore store = new ErrorEventStore(2, 10);

        store.addError(error("1", "thread-1", 1_000));
        store.addError(error("2", "thread-2", 2_000));
        store.addError(error("3", "thread-3", 3_000));

        assertThat(store.getErrors()).extracting(ErrorEvent::getId).containsExactly("3", "2");
    }

    @Test
    void dropsOldestLogWhenFull() {
        ErrorEventStore store = new ErrorEventStore(10, 2);

        store.addLogs(log("1", "INFO", "main"));
        store.addLogs(log("2", "INFO", "main"));
        store.addLogs(log("3", "INFO", "main"));

        assertThat(store.getLogs(null)).extracting(LogEntry::getId).containsExactly("3", "2");
    }

    @Test
    void rejectsSameErrorOnSameThreadWithin500ms() {
        ErrorEventStore store = new ErrorEventStore(10, 10);

        store.addError(error("1", "exec-1", 1_000));
        store.addError(error("2", "exec-1", 1_300));

        assertThat(store.getErrorCount()).isEqualTo(1);
    }

    @Test
    void keepsSameErrorFromAnotherThreadOrLater() {
        ErrorEventStore store = new ErrorEventStore(10, 10);

        store.addError(error("1", "exec-1", 1_000));
        store.addError(error("2", "exec-2", 1_000)); // other thread
        store.addError(error("3", "exec-1", 2_000)); // same thread, outside the window

        assertThat(store.getErrorCount()).isEqualTo(3);
    }

    @Test
    void filtersLogsByLevelIgnoringCase() {
        ErrorEventStore store = new ErrorEventStore(10, 10);
        store.addLogs(log("1", "INFO", "main"));
        store.addLogs(log("2", "ERROR", "main"));

        assertThat(store.getLogs("error")).extracting(LogEntry::getId).containsExactly("2");
        assertThat(store.getLogs(" ")).hasSize(2);
    }

    @Test
    void recentLogsByThreadKeepsTheLatestInTimelineOrder() {
        ErrorEventStore store = new ErrorEventStore(10, 10);
        store.addLogs(log("1", "INFO", "exec-1"));
        store.addLogs(log("2", "INFO", "exec-2"));
        store.addLogs(log("3", "INFO", "exec-1"));
        store.addLogs(log("4", "INFO", "exec-1"));

        List<LogEntry> logs = store.getRecentLogsByThread("exec-1", 2);

        assertThat(logs).extracting(LogEntry::getId).containsExactly("3", "4");
    }

    @Test
    void clearWipesEverything() {
        ErrorEventStore store = new ErrorEventStore(10, 10);
        store.addError(error("1", "exec-1", 1_000));
        store.addLogs(log("1", "INFO", "exec-1"));

        store.clear();

        assertThat(store.getErrorCount()).isZero();
        assertThat(store.getLogCount()).isZero();
    }

    @Test
    void neverGoesOverCapacityUnderConcurrentWrites() throws InterruptedException {
        ErrorEventStore store = new ErrorEventStore(10, 100);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(8 * 500);

        for (int i = 0; i < 8 * 500; i++) {
            String id = String.valueOf(i);
            pool.submit(() -> {
                store.addLogs(log(id, "INFO", Thread.currentThread().getName()));
                done.countDown();
            });
        }

        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        assertThat(store.getLogCount()).isEqualTo(100);
    }

    private ErrorEvent error(String id, String thread, long timestamp) {
        return new ErrorEvent(id, timestamp, thread, "java.lang.IllegalStateException", "boom",
                List.of(), List.of(), List.of(), null);
    }

    private LogEntry log(String id, String level, String thread) {
        return new LogEntry(id, System.currentTimeMillis(), level, "com.example.Test", "msg " + id, thread);
    }
}
