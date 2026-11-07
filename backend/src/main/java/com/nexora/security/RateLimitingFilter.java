package com.nexora.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight in-memory rate limiting filter.
 * Throttles auth endpoints (/api/auth/**) to prevent credential brute-forcing
 * and excessive room-code guessing attacks.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 60;
    private final Map<String, RequestTracker> ipTracker = new ConcurrentHashMap<>();

    private static class RequestTracker {
        long windowStartMs;
        AtomicInteger count = new AtomicInteger(0);

        RequestTracker(long startMs) {
            this.windowStartMs = startMs;
            this.count.set(1);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            long now = System.currentTimeMillis();

            RequestTracker tracker = ipTracker.compute(clientIp, (ip, current) -> {
                if (current == null || (now - current.windowStartMs) > 60_000) {
                    return new RequestTracker(now);
                }
                current.count.incrementAndGet();
                return current;
            });

            if (tracker.count.get() > MAX_AUTH_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Rate limit exceeded. Try again in 60 seconds.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
