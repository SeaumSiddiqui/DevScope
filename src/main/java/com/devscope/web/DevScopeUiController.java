package com.devscope.web;

import com.devscope.autoconfigure.DevScopeUiAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the DevScope UI and exposes a health endpoint for boot detection.
 *
 * Registered unconditionally via {@link DevScopeUiAutoConfiguration} so that
 * /dev-scope always resolves regardless of devscope.enabled. If only
 * DevScopeAutoConfiguration registered this controller, disabling DevScope
 * would leave /dev-scope unmapped — causing NoResourceFoundException to bubble
 * up to the consumer's GlobalExceptionHandler, which is unacceptable for a
 * library that must be transparent to its host application.
 *
 * The /dev-scope/health endpoint lets the UI detect the disabled state cleanly
 * without generating any 404s or exceptions in the consumer's logs.
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

    /**
     * Called by the UI on boot to determine whether DevScope is active.
     *
     * Checks whether devScopeController registered as a bean — it only does
     * when devscope.enabled=true (or unset). Returns 200 when active, 503 when
     * disabled. The UI stops polling on anything other than 200, producing no
     * further requests and no noise in the host application's logs.
     */
    @GetMapping("/dev-scope/health")
    public ResponseEntity<Void> health() {
        boolean active = applicationContext.containsBean("devScopeController");
        return active
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(503).build();
    }
}
