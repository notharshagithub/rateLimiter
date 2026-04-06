package ratelimiter.core;

import java.time.Duration;

/**
 * Immutable configuration for a rate limit policy.
 *
 * Examples:
 *   RateLimitConfig.of(100, Duration.ofMinutes(1))  → 100 req/min
 *   RateLimitConfig.of(1000, Duration.ofHours(1))   → 1000 req/hr
 */
public final class RateLimitConfig {

    private final long   maxRequests;
    private final Duration windowDuration;

    private RateLimitConfig(long maxRequests, Duration windowDuration) {
        if (maxRequests <= 0)         throw new IllegalArgumentException("maxRequests must be > 0");
        if (windowDuration.isZero() ||
            windowDuration.isNegative()) throw new IllegalArgumentException("window must be positive");

        this.maxRequests    = maxRequests;
        this.windowDuration = windowDuration;
    }

    public static RateLimitConfig of(long maxRequests, Duration windowDuration) {
        return new RateLimitConfig(maxRequests, windowDuration);
    }

    public long     getMaxRequests()    { return maxRequests; }
    public Duration getWindowDuration() { return windowDuration; }
    public long     getWindowMillis()   { return windowDuration.toMillis(); }

    @Override
    public String toString() {
        return maxRequests + " requests per " + windowDuration;
    }
}
