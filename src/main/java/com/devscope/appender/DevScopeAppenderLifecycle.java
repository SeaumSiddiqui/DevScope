package com.devscope.appender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.devscope.store.ErrorEventStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;

/**
 * Attaches DevScopeAppender to Logback's root logger on startup.
 *
 * This lives outside DevScopeAutoConfiguration on purpose. A @PostConstruct
 * on a @Configuration class that uses its own @Bean methods is a
 * self-referencing cycle, and Spring Boot 3 refuses to start with it.
 */
public class DevScopeAppenderLifecycle {
    private final ErrorEventStore store;

    public DevScopeAppenderLifecycle(ErrorEventStore store) {
        this.store = store;
    }

    @PostConstruct
    public void attach() {
        DevScopeAppender appender = new DevScopeAppender(store);

        // SLF4J is just the facade, at runtime the factory is Logback's LoggerContext
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender.setContext(context);
        appender.start(); // Logback silently drops events sent to an appender that isn't started

        // every logger in the app is a child of ROOT, so this one line catches all of them
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);

        LoggerFactory.getLogger(DevScopeAppenderLifecycle.class)
                .info("DevScope is active. UI available at /dev-scope");
    }
}
