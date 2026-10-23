package com.devscope.web;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;
import com.devscope.model.StackFrame;
import com.devscope.store.ErrorEventStore;

import java.util.*;

/**
 * Turns a thrown exception into an ErrorEvent and stores it.
 *
 * Shared by the filter (unhandled exceptions) and the exception resolver
 * (exceptions an @ExceptionHandler takes care of), so an exception that
 * passes through both is still stored only once.
 */
public class ErrorCapture {
    private static final int CORRELATED_LOG_LIMIT = 20;

    private final ErrorEventStore store;
    private final String basePackage;

    // Throwable doesn't override equals/hashCode, so this is an identity set.
    // Weak keys: once the request is done the exception can be GC'd normally.
    private final Set<Throwable> captured = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );

    public ErrorCapture(ErrorEventStore store, String basePackage) {
        this.store = store;
        this.basePackage = basePackage;
    }

    public void capture(Throwable thrown) {
        Throwable actual = unwrap(thrown);

        if (actual == null || isInfrastructureException(actual)) return;
        if (!captured.add(actual)) return; // already stored by the other entry point

        store.addError(buildErrorEvent(actual, Thread.currentThread().getName()));
    }

    // Spring wraps controller exceptions in ServletException, we want the one the app threw
    private Throwable unwrap(Throwable t) {
        while (t != null && t.getCause() != null && isWrapper(t)) {
            t = t.getCause();
        }
        return t;
    }

    private boolean isWrapper(Throwable t) {
        String name = t.getClass().getName();
        return name.equals("jakarta.servlet.ServletException")
                || name.equals("org.springframework.web.util.NestedServletException")
                || name.equals("java.lang.reflect.InvocationTargetException");
    }

    // 404s, wrong method, bad params etc. are not bugs in the app, they'd just be noise in the feed
    private boolean isInfrastructureException(Throwable t) {
        String name = t.getClass().getName();
        return name.startsWith("org.springframework.web.servlet.resource.")
                || name.equals("org.springframework.web.servlet.NoHandlerFoundException")
                || name.equals("org.springframework.security.access.AccessDeniedException")
                || name.equals("org.springframework.web.HttpRequestMethodNotSupportedException")
                || name.equals("org.springframework.web.HttpMediaTypeNotSupportedException")
                || name.equals("org.springframework.web.HttpMediaTypeNotAcceptableException")
                || name.equals("org.springframework.web.bind.MissingServletRequestParameterException")
                || name.equals("org.springframework.web.bind.MethodArgumentNotValidException");
    }

    private ErrorEvent buildErrorEvent(Throwable t, String threadName) {
        List<StackFrame> frames = parseFrames(t.getStackTrace());
        List<ErrorEvent.CausedBy> causeChain = parseCauseChain(t.getCause());

        // the appender already stored this thread's log lines before the exception got here
        List<LogEntry> correlatedLogs = store.getRecentLogsByThread(threadName, CORRELATED_LOG_LIMIT);

        return new ErrorEvent(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                threadName,
                t.getClass().getName(),
                t.getMessage(),
                frames,
                causeChain,
                correlatedLogs,
                null // no trigger log line when we capture outside of Logback
        );
    }

    private List<StackFrame> parseFrames(StackTraceElement[] elements) {
        List<StackFrame> frames = new ArrayList<>();

        if (elements == null) return frames;

        for (StackTraceElement el : elements) {
            boolean isAppFrame = basePackage != null
                    && !basePackage.isBlank()
                    && el.getClassName().startsWith(basePackage);

            frames.add(new StackFrame(
                    el.getClassName(),
                    el.getMethodName(),
                    el.getFileName(),
                    el.getLineNumber(),
                    isAppFrame
            ));
        }
        return frames;
    }

    // walked with a loop, not recursion, so a very deep chain can't blow the stack
    private List<ErrorEvent.CausedBy> parseCauseChain(Throwable cause) {
        List<ErrorEvent.CausedBy> chain = new ArrayList<>();

        while (cause != null) {
            chain.add(new ErrorEvent.CausedBy(
                    cause.getClass().getName(),
                    cause.getMessage(),
                    parseFrames(cause.getStackTrace())
            ));
            cause = cause.getCause();
        }
        return chain;
    }
}
