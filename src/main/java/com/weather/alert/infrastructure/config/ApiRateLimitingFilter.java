package com.weather.alert.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiRateLimitingFilter extends OncePerRequestFilter {

    private static final int TOO_MANY_REQUESTS_STATUS = 429;
    private static final int MAX_CLIENT_IP_LENGTH = 64;
    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final boolean trustForwardedFor;
    private final Map<String, RateLimitWindow> requestWindows = new ConcurrentHashMap<>();
    private volatile long nextCleanupTimeMillis = 0;

    public ApiRateLimitingFilter(
            @Value("${app.rate-limit.max-requests:120}") int maxRequestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = windowSeconds * 1000;
        this.trustForwardedFor = trustForwardedFor;
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
        requestWindows.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                currentCountRef.set(1);
                return new RateLimitWindow(now, new AtomicInteger(1));
            }
            currentCountRef.set(existing.requestCount.incrementAndGet());
            return existing;
        });

        int currentCount = currentCountRef.get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerWindow));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequestsPerWindow - currentCount)));

        if (currentCount > maxRequestsPerWindow) {
            response.setStatus(TOO_MANY_REQUESTS_STATUS);
            response.setContentType("text/plain");
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientKey(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String clientIp = forwardedFor.split(",")[0].trim();
                if (!clientIp.isBlank() && clientIp.length() <= MAX_CLIENT_IP_LENGTH) {
                    return clientIp;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private void cleanupExpiredWindows(long now) {
        if (now < nextCleanupTimeMillis) {
            return;
        }
        requestWindows.entrySet().removeIf(entry -> now - entry.getValue().windowStartMillis >= windowMillis);
        nextCleanupTimeMillis = now + windowMillis;
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
