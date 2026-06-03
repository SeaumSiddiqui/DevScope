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
 * Captures exceptions thrown by the application before Spring's error
 * handling discards the cause wrapper chain.
 *
 * WHY A FILTER AND NOT THE LOGBACK APPENDER:
 * Spring's DispatcherServlet logs the ROOT CAUSE as the throwable on the
 * log event — not the exception your code threw. A filter sits above that
 * entire process, intercepts the raw exception first, and stores it with
 * the full wrapper chain intact. It then rethrows so Spring handles the
 * HTTP error response normally. DevScope never changes what the client sees.
 *
 * Registered at order -1 so it runs before Spring's own filters.
 */
public class DevScopeRequestFilter extends OncePerRequestFilter {
    private final ErrorEventStore store;
    private final String basePackage;

    public DevScopeRequestFilter(ErrorEventStore store, String basePackage) {
        this.store = store;
        this.basePackage = basePackage;
    }

    /**
     * Tracks captured exception instances by reference using weak keys.
     * The servlet container sometimes re-dispatches error requests through
     * the filter chain, which would cause the same exception to be stored
     * twice. WeakHashMap identity-keying prevents that without leaking memory
     * — exceptions are garbage collected normally once the request is done.
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
            if (!isInfrastructureException(actual) && capturedExceptions.add(actual)) {
                store.addError(buildFromThrowable(actual, Thread.currentThread().getName()));
            }
            throw e; // Always rethrow: DevScope never changes the HTTP response
        }
    }

    /**
     * Strips servlet and Spring wrapper types to reach the exception the
     * application actually threw. Spring Boot 3 wraps controller exceptions
     * in ServletException before they reach the filter chain.
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

    /**
     * Returns true for Spring infrastructure exceptions that are not
     * application bugs — missing resources, unsupported methods, etc.
     * These would be noise in the error feed and are excluded from capture.
     */
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

    private ErrorEvent buildFromThrowable(Throwable t, String threadName) {
        List<StackFrame> frames         = parseFrames(t.getStackTrace());
        List<ErrorEvent.CausedBy> chain = parseCauseChain(t.getCause());

        // Log lines from this thread are already in the store — the Logback
        // appender captured them before the exception reached this filter.
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
                null // triggerLogId not available at filter level
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
