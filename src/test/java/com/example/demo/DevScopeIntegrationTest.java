package com.example.demo;

import com.devscope.model.ErrorEvent;
import com.devscope.store.ErrorEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A tiny app with DevScope on the classpath, like a real consumer would have it.
 */
@SpringBootTest(properties = {
        "devscope.enabled=true",
        "devscope.base-package=com.example.demo"
})
@AutoConfigureMockMvc
class DevScopeIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ErrorEventStore store;

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    @Test
    void capturesUnhandledExceptionAndStillLetsItThrough() {
        assertThatThrownBy(() -> mockMvc.perform(get("/orders/unhandled")))
                .hasRootCauseInstanceOf(IllegalStateException.class);

        assertThat(store.getErrors()).hasSize(1);
        ErrorEvent event = store.getErrors().get(0);
        assertThat(event.getExceptionClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(event.getFrames()).anyMatch(f -> f.isAppFrame() && f.getMethodName().equals("unhandled"));
        assertThat(event.getCorrelatedLogs()).anyMatch(l -> l.getMessage().equals("placing order 42"));
    }

    @Test
    void capturesExceptionHandledByControllerAdviceWithoutChangingTheResponse() throws Exception {
        mockMvc.perform(get("/orders/handled"))
                .andExpect(status().isBadRequest());

        assertThat(store.getErrors())
                .extracting(ErrorEvent::getExceptionClass)
                .containsExactly(IllegalArgumentException.class.getName());
    }

    @Test
    void apiReturnsCapturedErrors() throws Exception {
        mockMvc.perform(get("/orders/handled"));

        mockMvc.perform(get("/dev-scope/api/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exceptionMessage").value("bad order id"));

        mockMvc.perform(get("/dev-scope/health"))
                .andExpect(status().isOk());
    }
}
