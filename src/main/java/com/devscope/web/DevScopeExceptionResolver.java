package com.devscope.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Sees every exception a controller throws, before any @ExceptionHandler runs.
 *
 * Most apps have a @ControllerAdvice that turns exceptions into a nice JSON
 * response. Those exceptions never reach the filter, so without this they'd
 * never show up in DevScope.
 *
 * Runs first and always returns null, which means "not handled by me", so
 * Spring carries on with the app's own handlers exactly as before.
 * (Spring Boot's DefaultErrorAttributes uses the same trick.)
 */
public class DevScopeExceptionResolver implements HandlerExceptionResolver, Ordered {
    private final ErrorCapture capture;

    public DevScopeExceptionResolver(ErrorCapture capture) {
        this.capture = capture;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        capture.capture(ex);
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
