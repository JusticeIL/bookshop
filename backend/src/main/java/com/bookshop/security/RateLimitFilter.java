package com.bookshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding-window rate limiter for the credential endpoints, blunting online
 * password guessing and signup floods.
 *
 * <p>Deliberately in-process and dependency-free: one instance serves this
 * deployment, and the limiter must keep working when Redis is unavailable.
 * A distributed deployment would move the counters into Redis.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private static final Map<String, Deque<Long>> ATTEMPTS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean credentialEndpoint = "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/oauth2/token".equals(path)
                || "/login".equals(path);
        return !credentialEndpoint || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (exceedsLimit(clientKey(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "message", "Too many attempts. Please wait a minute and try again.",
                    "timestamp", Instant.now().toString()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean exceedsLimit(String key) {
        long now = Instant.now().getEpochSecond();
        // Cheap unbounded-growth guard: a flood from many spoofed sources
        // resets the table rather than exhausting the heap.
        if (ATTEMPTS.size() > MAX_TRACKED_CLIENTS) {
            ATTEMPTS.clear();
        }
        Deque<Long> window = ATTEMPTS.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_SECONDS) {
                window.pollFirst();
            }
            if (window.size() >= MAX_ATTEMPTS) {
                return true;
            }
            window.addLast(now);
            return false;
        }
    }

    /** Uses the proxy-forwarded client IP when present (Render sits behind one). */
    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
