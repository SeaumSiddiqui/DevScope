package com.devscope.autoconfigure;

import com.devscope.web.DevScopeUiController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Always registers the UI controller, even when DevScope is disabled.
 * Otherwise /dev-scope is unmapped, Spring throws NoResourceFoundException
 * and the consumer's GlobalExceptionHandler logs it as an ERROR. A library
 * shouldn't leave noise like that in someone else's logs.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DevScopeUiAutoConfiguration {
    @Bean
    public DevScopeUiController devScopeUiController(ApplicationContext context) {
        return new DevScopeUiController(context);
    }
}
