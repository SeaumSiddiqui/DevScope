package com.devscope.web;

import com.devscope.model.ErrorEvent;
import com.devscope.model.LogEntry;
import com.devscope.store.ErrorEventStore;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API the UI polls every 3 seconds. Everything sits under
 * /dev-scope/api so it can't clash with the app's own routes.
 *
 *  GET    /dev-scope/api/errors          -> all errors, newest first
 *  GET    /dev-scope/api/errors/{id}     -> one error
 *  GET    /dev-scope/api/logs?level=X    -> logs, newest first, level optional
 *  GET    /dev-scope/api/stats           -> counts + active profiles
 *  DELETE /dev-scope/api/clear           -> wipe everything
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

    // 404 if the id is unknown or it was already dropped from the ring buffer
    @GetMapping("/errors/{id}")
    public ResponseEntity<ErrorEvent> getError(@PathVariable String id) {
        return store.getErrors()
                .stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
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

    // profiles go here too so the UI header updates with one fetch.
    // no active profile -> ["default"], same as Spring Boot itself
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
