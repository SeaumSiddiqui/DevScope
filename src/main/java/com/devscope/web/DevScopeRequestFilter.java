package com.devscope.web;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;
import com.devscope.model.StackFrame;
import com.devscope.store.ErrorEventStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Intercepts every HTTP request and captures exceptions in their full original
 * form — before Spring's error handling discards the wrapper chain.
 *
 * WHY A FILTER AND NOT THE LOGBACK APPENDER:
 * When a controller throws a wrapped exception (e.g. OrderServiceException
 * wrapping IOException), Spring's DispatcherServlet logs the ROOT CAUSE as the
 * attached throwable — not the exception your code threw. The Logback appender
 * would therefore always capture the wrong exception as the primary entry.
 *
 * This filter sits above Spring's error handling. It catches the raw exception
 * first, walks the full cause chain, and stores the correct outermost exception.
 * It then rethrows so Spring can still produce the normal HTTP error response.
 *
 * Registered at order -1 via FilterRegistrationBean so it runs before Spring's
 * own filters and sees the unmodified exception.
 */
public class DevScopeRequestFilter extends OncePerRequestFilter {
    private final ErrorEventStore store;
    private final String basePackage;

    public DevScopeRequestFilter(ErrorEventStore store, String basePackage) {
        this.store = store;
        this.basePackage = basePackage;
    }

    /**
     * Guards against double-capture using weak references to exception instances.
     *
     * The servlet container sometimes re-dispatches error requests through the
     * filter chain (e.g. when forwarding to /error). WeakHashMap keys on object
     * identity — the same exception object reference caught a second time returns
     * false from add(), skipping the store write. Weak references ensure captured
     * exceptions are garbage collected normally once the request is done.
     */
    private final Set<Throwable> capturedExceptions = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            Throwable actual = unwrap(e);
            if (capturedExceptions.add(actual)) {
                store.addError(buildFromThrowable(actual, Thread.currentThread().getName()));
            }
            throw e; // Always rethrow — DevScope never changes the HTTP response
        }
    }

    /**
     * Strips known servlet and Spring wrapper types to get to the exception
     * the application actually threw.
     *
     * Spring Boot 3 wraps controller exceptions in ServletException before they
     * reach the filter chain. We unwrap until we find a non-wrapper type.
     */
    private Throwable unwrap(Throwable t) {
        while (t != null) {
            String name = t.getClass().getName();
            if (name.equals("jakarta.servlet.ServletException")
                    || name.equals("org.springframework.web.util.NestedServletException")
                    || name.equals("java.lang.reflect.InvocationTargetException")) {
                if (t.getCause() != null) {
                    t = t.getCause();
                    continue;
                }
            }
            break;
        }
        return t;
    }

    private ErrorEvent buildFromThrowable(Throwable t, String threadName) {
        List<StackFrame> frames         = parseFrames(t.getStackTrace());
        List<ErrorEvent.CausedBy> chain = parseCauseChain(t.getCause());

        // The Logback appender has already stored the log lines that ran on
        // this thread before the exception reached the filter. We fetch them
        // here to attach as correlated context on the ErrorEvent.
        List<LogEntry> correlatedLogs = store.getRecentLogsByThread(threadName, 20);

        return new ErrorEvent(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                threadName,
                t.getClass().getName(),
                t.getMessage(),
                frames,
                chain,
                correlatedLogs,
                null // triggerLogId — not available at filter level
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
