package ph.chargemap.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ph.chargemap.common.error.ErrorResponse;
import ph.chargemap.config.ChargeMapProperties;

import java.io.IOException;
import java.time.Duration;

/**
 * Redis-backed fixed-window rate limiter for abuse-prone endpoints (product spec §37):
 * all write requests (POST/PUT/DELETE) and the geo/search reads. Keyed by client IP +
 * authenticated user + minute window. Fails open if Redis is unavailable so a cache
 * outage never blocks legitimate traffic.
 *
 * <p>Redis is OPTIONAL: when no {@code StringRedisTemplate} bean is present (e.g. a
 * free-tier deploy with no Redis), rate limiting is simply disabled — the app runs fine
 * without it.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final StringRedisTemplate redisTemplate; // may be null when Redis isn't configured
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int limit;

    public RateLimitFilter(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                           ObjectMapper objectMapper, ChargeMapProperties props) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        // Only active when explicitly enabled AND a Redis template is available.
        this.enabled = props.getRateLimit().isEnabled() && this.redisTemplate != null;
        this.limit = props.getRateLimit().getRequestsPerMinute();
        if (props.getRateLimit().isEnabled() && this.redisTemplate == null) {
            log.warn("Rate limiting requested but no Redis is configured — disabling it.");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean isWrite = !"GET".equals(method) && !"OPTIONS".equals(method);
        boolean isGeoOrSearch = path.startsWith("/api/stations/nearby")
                || path.startsWith("/api/stations/cheapest")
                || path.startsWith("/api/stations/available")
                || path.startsWith("/api/stations/search");
        return !(isWrite || isGeoOrSearch);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String key = buildKey(request);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            if (count != null && count > limit) {
                writeTooManyRequests(response);
                return;
            }
        } catch (Exception ex) {
            // Fail open: never block traffic because the limiter backend is down.
            log.warn("Rate limiter unavailable, allowing request: {}", ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String buildKey(HttpServletRequest request) {
        long window = System.currentTimeMillis() / 60_000L;
        String ip = request.getRemoteAddr();
        String route = request.getMethod() + ":" + request.getRequestURI();
        return "rl:" + ip + ":" + route + ":" + window;
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of("RATE_LIMITED", "Too many requests, slow down");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
