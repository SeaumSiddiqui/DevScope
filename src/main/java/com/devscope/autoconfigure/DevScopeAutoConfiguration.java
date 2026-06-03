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
 * Core autoConfiguration — wires all DevScope beans when active.
 *
 * Discovered via META-INF/spring/org.springframework.boot.autoconfigure
 * .AutoConfiguration.imports. No consumer-side annotation needed.
 *
 * Active only when:
 *   - The application has an embedded web server (@ConditionalOnWebApplication)
 *   - devscope.enabled is true or absent (@ConditionalOnProperty matchIfMissing=true)
 *
 * The UI controller is intentionally NOT registered here. It lives in
 * {@link DevScopeUiAutoConfiguration} so /dev-scope always resolves even
 * when this configuration is skipped due to devscope.enabled=false.
 *
 * WHY NO @PostConstruct HERE:
 * Spring Boot 3 prohibits self-referential bean cycles. Logback attachment
 * is delegated to {@link DevScopeAppenderLifecycle} — a plain bean with no
 * @Configuration restrictions — which handles it via its own @PostConstruct.
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
     * Intercepts raw exceptions before Spring's error handling strips the
     * cause chain. Registered at order -1 to run before Spring's own filters.
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
     * The singleton ring buffer shared across all DevScope components.
     * The filter writes errors into it; the appender writes log lines;
     * the controller reads both.
     */
    @Bean
    public ErrorEventStore errorEventStore() {
        return new ErrorEventStore(properties.getMaxErrors(), properties.getMaxLogs());
    }

    /** REST API — registers /dev-scope/api/** endpoints consumed by the UI. */
    @Bean
    public DevScopeController devScopeController(ErrorEventStore store, Environment environment) {
        return new DevScopeController(store, environment);
    }

    /** Attaches the Logback appender to the root logger via @PostConstruct. */
    @Bean
    public DevScopeAppenderLifecycle devScopeAppenderLifecycle(ErrorEventStore store) {
        return new DevScopeAppenderLifecycle(store, properties.getBasePackage());
    }
}