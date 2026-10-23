package com.devscope.web;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the UI page and a health check. Registered even when DevScope is
 * disabled (see DevScopeUiAutoConfiguration).
 */
@RestController
public class DevScopeUiController {
    private final ApplicationContext applicationContext;

    public DevScopeUiController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping(value = {"/dev-scope", "/dev-scope/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> ui() {
        ClassPathResource page = new ClassPathResource("META-INF/resources/dev-scope/index.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    // the UI calls this on load: 200 = active, 503 = disabled and it stops polling.
    // devScopeController only exists when devscope.enabled=true
    @GetMapping("/dev-scope/health")
    public ResponseEntity<Void> health() {
        boolean active = applicationContext.containsBean("devScopeController");
        return active
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(503).build();
    }
}
