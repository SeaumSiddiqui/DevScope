package com.devscope.autoconfigure;

import com.devscope.store.ErrorEventStore;
import com.devscope.web.DevScopeController;
import com.devscope.web.DevScopeExceptionResolver;
import com.devscope.web.DevScopeUiController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DevScopeAutoConfigurationTest {
    private final AutoConfigurations devScope =
            AutoConfigurations.of(DevScopeAutoConfiguration.class, DevScopeUiAutoConfiguration.class);

    private final WebApplicationContextRunner webApp = new WebApplicationContextRunner()
            .withConfiguration(devScope);

    @Test
    void offByDefaultButUiControllerStillThere() {
        webApp.run(context -> {
            assertThat(context).doesNotHaveBean(ErrorEventStore.class);
            assertThat(context).doesNotHaveBean(DevScopeController.class);
            assertThat(context).hasSingleBean(DevScopeUiController.class);
        });
    }

    @Test
    void everythingRegisteredWhenEnabled() {
        webApp.withPropertyValues("devscope.enabled=true", "devscope.max-errors=5")
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorEventStore.class);
                    assertThat(context).hasSingleBean(DevScopeController.class);
                    assertThat(context).hasSingleBean(DevScopeExceptionResolver.class);
                    assertThat(context).hasBean("devScopeRequestFilter");
                    assertThat(context.getBean(DevScopeProperties.class).getMaxErrors()).isEqualTo(5);
                });
    }

    @Test
    void notActiveOutsideAWebApp() {
        new ApplicationContextRunner()
                .withConfiguration(devScope)
                .withPropertyValues("devscope.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ErrorEventStore.class);
                    assertThat(context).doesNotHaveBean(DevScopeUiController.class);
                });
    }
}
