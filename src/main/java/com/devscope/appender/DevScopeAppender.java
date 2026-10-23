package com.devscope.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.devscope.model.LogEntry;
import com.devscope.store.ErrorEventStore;

import java.util.UUID;

/**
 * Logback appender that copies every log line into the store.
 *
 * Only handles log lines. Exceptions are captured by ErrorCapture, because
 * by the time Spring logs an exception it has already swapped it for the
 * root cause.
 *
 * append() runs on the app thread for every single log call, so it has to
 * stay cheap: build a POJO, write it to the store, done. No I/O.
 */
public class DevScopeAppender extends AppenderBase<ILoggingEvent> {
    private final ErrorEventStore store;

    public DevScopeAppender(ErrorEventStore store) {
        this.store = store;
        setName("DEVSCOPE"); // how Logback identifies this appender
    }

    @Override
    protected void append(ILoggingEvent event) {
        store.addLogs(buildLogEntry(event));
    }

    private LogEntry buildLogEntry(ILoggingEvent event) {
        return new LogEntry(
                UUID.randomUUID().toString(),
                event.getTimeStamp(),
                event.getLevel().toString(), // "ERROR", "WARN", "INFO", "DEBUG"
                event.getLoggerName(),
                event.getFormattedMessage(), // message with args substituted
                event.getThreadName() // e.g. "http-nio-8080-exec-3"
        );
    }
}
