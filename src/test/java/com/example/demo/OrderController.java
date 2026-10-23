package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/orders/unhandled")
    public String unhandled() {
        log.info("placing order 42");
        throw new IllegalStateException("could not process order");
    }

    // GlobalExceptionHandler turns this one into a 400
    @GetMapping("/orders/handled")
    public String handled() {
        throw new IllegalArgumentException("bad order id");
    }
}
