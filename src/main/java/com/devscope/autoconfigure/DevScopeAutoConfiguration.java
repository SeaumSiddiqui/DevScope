package com.devscope.autoconfigure;

import com.devscope.appender.DevScopeAppenderLifecycle;
import com.devscope.store.ErrorEventStore;
import com.devscope.web.DevScopeController;
import com.devscope.web.DevScopeRequestFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Wires everything up in a servlet web app unless devscope.enabled=false.
 * Picked up from META-INF/spring/...AutoConfiguration.imports, so the
 * consumer doesn't need any annotation.
 *
 * The UI controller is registered in DevScopeUiAutoConfiguration instead,
 * so /dev-scope still resolves when this config is switched off.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "devscope.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DevScopeProperties.class)
public class DevScopeAutoConfiguration {
    private final DevScopeProperties properties;

    public DevScopeAutoConfiguration(DevScopeProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ErrorEventStore errorEventStore() {
        return new ErrorEventStore(properties.getMaxErrors(), properties.getMaxLogs());
    }

    // catches exceptions before Spring strips the cause chain, order -1 so we sit before Spring's own filters
    @Bean
    public FilterRegistrationBean<DevScopeRequestFilter> devScopeRequestFilter(ErrorEventStore store) {
        FilterRegistrationBean<DevScopeRequestFilter> registration =
                new FilterRegistrationBean<>(new DevScopeRequestFilter(store, properties.getBasePackage()));
        registration.setOrder(-1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public DevScopeController devScopeController(ErrorEventStore store, Environment environment) {
        return new DevScopeController(store, environment);
    }

    // attaches the Logback appender, kept out of this class to avoid a self-referencing bean cycle
    @Bean
    public DevScopeAppenderLifecycle devScopeAppenderLifecycle(ErrorEventStore store) {
        return new DevScopeAppenderLifecycle(store);
    }
}
