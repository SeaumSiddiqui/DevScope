package com.devscope.web;

import com.devscope.store.ErrorEventStore;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevScopeRequestFilterTest {
    private final ErrorEventStore store = new ErrorEventStore(10, 10);
    private final DevScopeRequestFilter filter = new DevScopeRequestFilter(new ErrorCapture(store, "com.example"));

    @Test
    void rethrowsTheOriginalExceptionAfterStoringIt() {
        ServletException thrown = new ServletException("wrapped", new IllegalStateException("boom"));

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (req, res) -> { throw thrown; }))
                .isSameAs(thrown);

        assertThat(store.getErrorCount()).isEqualTo(1);
    }

    @Test
    void storesNothingWhenTheRequestSucceeds() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {});

        assertThat(store.getErrorCount()).isZero();
    }
}
