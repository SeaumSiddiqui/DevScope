package com.devscope.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the DevScope UI at /dev-scope and /dev-scope/.
 *
 * Exists as a separate controller from {@link DevScopeController} because the
 * API controller carries a class-level @RequestMapping("/dev-scope/api") which
 * cannot also handle the root /dev-scope route. Spring does not allow a single
 * class to own both paths cleanly.
 *
 * The HTML file lives at META-INF/resources/dev-scope/index.html inside the
 * library jar. Spring Boot serves files from META-INF/resources automatically
 * for library jars, but only resolves /dev-scope/index.html — not /dev-scope
 * or /dev-scope/ as a directory index. This controller bridges that gap.
 */
@RestController
public class DevScopeUiController {

    @GetMapping(value = {"/dev-scope", "/dev-scope/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> ui() {
        ClassPathResource page = new ClassPathResource("META-INF/resources/dev-scope/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(page);
    }
}
