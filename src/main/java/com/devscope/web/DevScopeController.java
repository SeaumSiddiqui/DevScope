package com.devscope.web;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;
import com.devscope.store.ErrorEventStore;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API consumed by the DevScope UI.
 *
 * All endpoints live under /dev-scope/api to avoid clashing with the
 * application's own routes. The UI polls /errors, /logs, and /stats every 3
 * seconds via fetch(). All other endpoints are available for external tooling.
 *
 * Endpoints:
 *   GET /dev-scope/api/errors all -> captured errors, newest first
 *   GET /dev-scope/api/errors/{id} -> single error by ID
 *   GET /dev-scope/api/logs -> all captured logs, newest first
 *   GET /dev-scope/api/logs?level=X -> filtered by level (ERROR, WARN, INFO, DEBUG)
 *   GET /dev-scope/api/stats -> counts and active Spring profiles
 *   DELETE /dev-scope/api/clear -> wipe all stored data
 */
@RequestMapping("/dev-scope/api")
@RestController
public class DevScopeController {
    private final ErrorEventStore store;
    private final Environment environment;

    public DevScopeController(ErrorEventStore store, Environment environment) {
        this.store = store;
        this.environment = environment;
    }

    @GetMapping("/errors")
    public List<ErrorEvent> getErrors() {
        return store.getErrors();
    }

    @GetMapping("/errors/{id}")
    public ResponseEntity<ErrorEvent> getError(@PathVariable String id) {
        Optional<ErrorEvent> found = store.getErrors()
                .stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();

        return found
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/logs")
    public List<LogEntry> getLogs(@RequestParam(required = false) String level) {
        return store.getLogs(level);
    }

    @DeleteMapping("/clear")
    public Map<String, String> clearAll() {
        store.clear();
        return Map.of(
                "message",
                "DevScope store cleared successfully."
        );
    }

    /**
     * Returns counts and active Spring profiles for the UI header.
     *
     * Profiles are included here rather than as a separate endpoint so the UI
     * can update all header values in one fetch. Falls back to ["default"] when
     * no profiles are explicitly active — matching Spring Boot's own behaviour.
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        String[] profiles = environment.getActiveProfiles();

        return Map.of(
                "errorCount", store.getErrorCount(),
                "logCount", store.getLogCount(),
                "activeProfiles", profiles.length > 0 ? profiles : new String[]{"default"}
        );
    }
}
