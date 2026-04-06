package ratelimiter.factory;

import ratelimiter.algorithms.FixedWindowRateLimiter;
import ratelimiter.algorithms.SlidingWindowRateLimiter;
import ratelimiter.core.RateLimitConfig;
import ratelimiter.core.RateLimiter;

/**
 * Factory Pattern — decouples callers from concrete algorithm classes.
 *
 * Callers request an algorithm by name (or enum); the factory handles
 * instantiation. Adding a new algorithm = one new case here + new class.
 * Nothing else changes.
 */
public final class RateLimiterFactory {

    public enum Algorithm {
        FIXED_WINDOW,
        SLIDING_WINDOW,
        TOKEN_BUCKET,     // future
        LEAKY_BUCKET,     // future
        SLIDING_LOG       // future
    }

    private RateLimiterFactory() {}   // static factory, not instantiable

    public static RateLimiter create(Algorithm algorithm, RateLimitConfig config) {
        switch (algorithm) {
            case FIXED_WINDOW:   return new FixedWindowRateLimiter(config);
            case SLIDING_WINDOW: return new SlidingWindowRateLimiter(config);
            // Uncomment as implementations are added:
            // case TOKEN_BUCKET:   return new TokenBucketRateLimiter(config);
            // case LEAKY_BUCKET:   return new LeakyBucketRateLimiter(config);
            // case SLIDING_LOG:    return new SlidingLogRateLimiter(config);
            default:
                throw new IllegalArgumentException("Algorithm not yet implemented: " + algorithm);
        }
    }
}
