package com.devscope.autoconfigure;

import com.devscope.web.DevScopeUiController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link DevScopeUiController} unconditionally for all web apps.
 *
 * Separate from {@link DevScopeAutoConfiguration} by design. The core config
 * is gated behind devscope.enabled — but the UI controller must always be
 * present so /dev-scope never goes unmapped. An unmapped /dev-scope would
 * throw NoResourceFoundException, which consumer GlobalExceptionHandlers
 * log as an ERROR. A library must never produce side effects in its host
 * application. This split is what prevents that.
 */
@Configuration
@ConditionalOnWebApplication
public class DevScopeUiAutoConfiguration {
    @Bean
    public DevScopeUiController devScopeUiController(ApplicationContext context) {
        return new DevScopeUiController(context);
    }
}
