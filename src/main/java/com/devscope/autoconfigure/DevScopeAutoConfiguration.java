package com.devscope.autoconfigure;

import com.devscope.appender.DevScopeAppenderLifecycle;
import com.devscope.store.ErrorEventStore;
import com.devscope.web.DevScopeController;
import com.devscope.web.DevScopeRequestFilter;
import com.devscope.web.DevScopeUiController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Boot autoConfiguration entry point for DevScope.
 *
 * Discovered via META-INF/spring/org.springframework.boot.autoconfigure
 * .AutoConfiguration.imports — no @Import or @EnableDevScope annotation needed
 * in the consumer application.
 *
 * Active only when both conditions pass:
 *   - @ConditionalOnWebApplication — skipped for batch jobs, CLI apps, etc.
 *   - @ConditionalOnProperty(devscope.enabled, matchIfMissing=true) — on by
 *     default; set devscope.enabled=false in a profile to disable.
 *
 * WHY NO @PostConstruct HERE:
 * Spring Boot 3 prohibits self-referential bean cycles. A @PostConstruct on
 * this class that called its own @Bean methods would trigger that check.
 * The Logback appender attachment is delegated to DevScopeAppenderLifecycle,
 * a plain bean that carries no @Configuration restrictions.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "devscope.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DevScopeProperties.class)
public class DevScopeAutoConfiguration {
    private final DevScopeProperties properties;

    public DevScopeAutoConfiguration(DevScopeProperties properties) {
        this.properties = properties;
    }

    /**
     * The servlet filter that captures raw exceptions before Spring's error
     * handling strips the cause wrapper.
     *
     * Order -1 places it before Spring's own filters so it sees the unmodified
     * exception. Mapped to /* to cover all application routes.
     */
    @Bean
    public FilterRegistrationBean<DevScopeRequestFilter> devScopeRequestFilter(ErrorEventStore store) {
        DevScopeRequestFilter filter = new DevScopeRequestFilter(store, properties.getBasePackage());
        FilterRegistrationBean<DevScopeRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * The singleton ring buffer shared by the filter (writes errors), the
     * appender (writes logs), and the controller (reads both).
     */
    @Bean
    public ErrorEventStore errorEventStore() {
        return new ErrorEventStore(properties.getMaxErrors(), properties.getMaxLogs());
    }

    /** REST API controller — registers /dev-scope/api/** endpoints. */
    @Bean
    public DevScopeController devScopeController(ErrorEventStore store, Environment environment) {
        return new DevScopeController(store, environment);
    }

    /** Attaches the Logback appender to the root logger via @PostConstruct. */
    @Bean
    public DevScopeAppenderLifecycle devScopeAppenderLifecycle(ErrorEventStore store) {
        return new DevScopeAppenderLifecycle(store, properties.getBasePackage());
    }

    /** Serves the HTML UI at /dev-scope and /dev-scope/. */
    @Bean
    public DevScopeUiController devScopeUiController() {
        return new DevScopeUiController();
    }
}