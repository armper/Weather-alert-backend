package com.weather.alert.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.weather.alert.infrastructure.web.ClientIpResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApiRateLimitingFilter extends OncePerRequestFilter {

    private static final int TOO_MANY_REQUESTS_STATUS = 429;
    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;
    private final Map<String, RateLimitWindow> requestWindows = new ConcurrentHashMap<>();
    private volatile long nextCleanupTimeMillis = 0;

    public ApiRateLimitingFilter(
            @Value("${app.rate-limit.max-requests:120}") int maxRequestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            ClientIpResolver clientIpResolver,
            ObjectMapper objectMapper) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = windowSeconds * 1000;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        cleanupExpiredWindows(now);
        String clientKey = extractClientKey(request);

        AtomicInteger currentCountRef = new AtomicInteger();
        AtomicLong windowStartMillisRef = new AtomicLong(now);
        requestWindows.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                currentCountRef.set(1);
                windowStartMillisRef.set(now);
                return new RateLimitWindow(now, new AtomicInteger(1));
            }
            currentCountRef.set(existing.requestCount.incrementAndGet());
            windowStartMillisRef.set(existing.windowStartMillis);
            return existing;
        });

        int currentCount = currentCountRef.get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerWindow));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequestsPerWindow - currentCount)));

        if (currentCount > maxRequestsPerWindow) {
            long retryAfterSeconds = Math.max(1, ((windowStartMillisRef.get() + windowMillis) - now + 999) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            writeRateLimitExceededResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientKey(HttpServletRequest request) {
        String clientIp = clientIpResolver.resolve(request);
        return (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
    }

    private void cleanupExpiredWindows(long now) {
        if (now < nextCleanupTimeMillis) {
            return;
        }
        requestWindows.entrySet().removeIf(entry -> now - entry.getValue().windowStartMillis >= windowMillis);
        nextCleanupTimeMillis = now + windowMillis;
    }

    private void writeRateLimitExceededResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        problem.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        problem.setType(URI.create("https://weather-alert-backend/errors/rate_limit_exceeded"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("errorCode", "RATE_LIMIT_EXCEEDED");
        problem.setProperty("path", request.getRequestURI());

        response.setStatus(TOO_MANY_REQUESTS_STATUS);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private static class RateLimitWindow {
        private final long windowStartMillis;
        private final AtomicInteger requestCount;

        private RateLimitWindow(long windowStartMillis, AtomicInteger requestCount) {
            this.windowStartMillis = windowStartMillis;
            this.requestCount = requestCount;
        }
    }
}
