package com.devscope.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.devscope.model.LogEntry;
import com.devscope.store.ErrorEventStore;

import java.util.UUID;

/**
 * Custom Logback appender that captures every log line into the DevScope store.
 *
 * Attached programmatically to Logback's root logger by
 * {@link DevScopeAppenderLifecycle} at startup — no logback.xml changes needed.
 *
 * SCOPE: this appender handles log lines only. Exception capture is handled
 * separately by {@link com.devscope.web.DevScopeRequestFilter}, which intercepts
 * the raw exception before Spring's error handling strips the wrapper chain.
 * Keeping these concerns separate means the appender stays fast — it does nothing
 * but build a plain object and write it to a concurrent collection.
 *
 * PERFORMANCE: append() runs synchronously on the application thread for every
 * log call in the entire application. Keep it fast — no I/O, no parsing, no
 * blocking operations.
 */
public class DevScopeAppender extends AppenderBase<ILoggingEvent> {
    private final ErrorEventStore store;

    // basePackage is accepted to keep the constructor signature consistent with
    // DevScopeAppenderLifecycle, which holds it for potential future use.
    // Frame parsing (which needs basePackage) lives in DevScopeRequestFilter.
    public DevScopeAppender(ErrorEventStore store, String basePackage) {
        this.store = store;
        setName("DEVSCOPE");
    }

    @Override
    protected void append(ILoggingEvent event) {
        store.addLogs(buildLogEntry(event));
    }

    private LogEntry buildLogEntry(ILoggingEvent event) {
        return new LogEntry(
                UUID.randomUUID().toString(),
                event.getTimeStamp(),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getFormattedMessage(),
                event.getThreadName()
        );
    }
}