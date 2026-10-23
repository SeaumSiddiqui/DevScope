package com.devscope.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Catches exceptions that nothing in the app handled.
 *
 * Why a filter and not the Logback appender: when Spring logs an unhandled
 * exception it attaches the root cause, not the exception your code threw.
 * The filter sits above all of that and sees the original.
 */
public class DevScopeRequestFilter extends OncePerRequestFilter {
    private final ErrorCapture capture;

    public DevScopeRequestFilter(ErrorCapture capture) {
        this.capture = capture;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            capture.capture(e);
            throw e; // always rethrow, DevScope never changes the response the client gets
        }
    }
}
