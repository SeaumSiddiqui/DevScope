package com.devscope.appender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.devscope.store.ErrorEventStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;

/**
 * Attaches {@link DevScopeAppender} to Logback's root logger at startup.
 *
 * WHY THIS IS A SEPARATE CLASS:
 * Spring Boot 3 prohibits circular bean dependencies. A @PostConstruct on a
 * @Configuration class that references its own @Bean methods creates a
 * self-referential cycle Spring refuses to resolve. By moving the attachment
 * logic here — into a plain bean with no @Configuration concerns — Spring
 * constructs this object cleanly and calls @PostConstruct once it's ready.
 *
 * Lifecycle:
 *   1. Spring creates ErrorEventStore
 *   2. Spring creates this bean (store injected)
 *   3. Spring calls attach() via @PostConstruct
 *   4. All log events from every class in the app flow through DevScopeAppender
 */
public class DevScopeAppenderLifecycle {
    private final ErrorEventStore store;
    private final String basePackage;

    /**
     * @param store the singleton store the appender writes into
     * @param basePackage passed through to DevScopeAppender for consistency;
     *                    frame parsing that uses this value lives in
     *                    DevScopeRequestFilter
     */
    public DevScopeAppenderLifecycle(ErrorEventStore store, String basePackage) {
        this.store = store;
        this.basePackage = basePackage;
    }

    @PostConstruct
    public void attach() {
        DevScopeAppender appender = new DevScopeAppender(store, basePackage);

        // SLF4J is an abstraction — getILoggerFactory() returns the concrete
        // Logback LoggerContext at runtime, which we need to start the appender.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender.setContext(context);
        appender.start(); // mandatory — Logback silently drops events to unstarted appenders

        // Attaching to ROOT means every child logger in the app (every class
        // using LoggerFactory.getLogger) routes through this appender automatically.
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);

        LoggerFactory.getLogger(DevScopeAppenderLifecycle.class)
                .info("DevScope is active. UI available at /dev-scope");
    }
}