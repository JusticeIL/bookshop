package com.bookshop.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Cheap unauthenticated health endpoint. Doubles as the keep-warm target so
 * Render's free tier never spins the service down during the review window.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "timestamp", OffsetDateTime.now().toString());
    }
}
