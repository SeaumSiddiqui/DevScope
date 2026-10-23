package com.devscope.web;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;
import com.devscope.model.StackFrame;
import com.devscope.store.ErrorEventStore;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCaptureTest {
    private final ErrorEventStore store = new ErrorEventStore(10, 10);
    private final ErrorCapture capture = new ErrorCapture(store, "com.devscope");

    @Test
    void storesTheExceptionTheAppThrewNotTheServletWrapper() {
        IllegalStateException thrown = new IllegalStateException("order failed");

        capture.capture(new ServletException("Request processing failed", thrown));

        ErrorEvent event = store.getErrors().get(0);
        assertThat(event.getExceptionClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(event.getExceptionMessage()).isEqualTo("order failed");
    }

    @Test
    void keepsTheWholeCauseChain() {
        IOException root = new IOException("socket timed out");
        RuntimeException thrown = new RuntimeException("could not process order",
                new IllegalStateException("repository failed", root));

        capture.capture(thrown);

        assertThat(store.getErrors().get(0).getCauseChain())
                .extracting(ErrorEvent.CausedBy::getExceptionClass)
                .containsExactly(IllegalStateException.class.getName(), IOException.class.getName());
    }

    @Test
    void marksFramesFromTheBasePackageAsAppFrames() {
        capture.capture(new RuntimeException("boom")); // thrown from this test class, so inside com.devscope

        assertThat(store.getErrors().get(0).getFrames())
                .filteredOn(StackFrame::isAppFrame)
                .isNotEmpty()
                .allMatch(f -> f.getClassName().startsWith("com.devscope"));
    }

    @Test
    void sameExceptionInstanceIsStoredOnce() {
        RuntimeException thrown = new RuntimeException("boom");

        capture.capture(thrown);                                      // resolver saw it
        capture.capture(new ServletException("wrapped", thrown));     // then the filter

        assertThat(store.getErrorCount()).isEqualTo(1);
    }

    @Test
    void ignoresSpringInfrastructureExceptions() {
        capture.capture(new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(store.getErrorCount()).isZero();
    }

    @Test
    void attachesLogLinesFromTheSameThread() {
        String thread = Thread.currentThread().getName();
        store.addLogs(new LogEntry("l1", 1L, "INFO", "com.example.OrderService", "placing order", thread));
        store.addLogs(new LogEntry("l2", 2L, "INFO", "com.example.Other", "other thread", "somewhere-else"));

        capture.capture(new RuntimeException("boom"));

        assertThat(store.getErrors().get(0).getCorrelatedLogs())
                .extracting(LogEntry::getId)
                .containsExactly("l1");
    }
}
