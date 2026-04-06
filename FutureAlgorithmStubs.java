package ratelimiter.algorithms;

import ratelimiter.core.RateLimitConfig;
import ratelimiter.core.RateLimiter;

/**
 * Stubs for future algorithms — shows how the Strategy pattern makes
 * plugging in a new algorithm a matter of implementing RateLimiter only.
 * No existing code needs to change (Open/Closed Principle).
 */

// ── Token Bucket ─────────────────────────────────────────────────────────────
class TokenBucketRateLimiter implements RateLimiter {
    /*
     * Idea: bucket holds up to `maxRequests` tokens.
     * Tokens refill at a constant rate (maxRequests / windowMillis per ms).
     * Each call consumes one token. Allowed if tokens >= 1.
     *
     * PRO:  Handles bursts gracefully — a quiet period accumulates tokens.
     * CON:  Slightly more complex state (double for token count + last-refill time).
     */
    private final RateLimitConfig config;
    TokenBucketRateLimiter(RateLimitConfig config) { this.config = config; }

    @Override public boolean isAllowed(String key)     { throw new UnsupportedOperationException("TODO"); }
    @Override public long    remainingQuota(String key){ throw new UnsupportedOperationException("TODO"); }
    @Override public String  algorithmName()           { return "TokenBucket"; }
}

// ── Leaky Bucket ─────────────────────────────────────────────────────────────
class LeakyBucketRateLimiter implements RateLimiter {
    /*
     * Idea: requests are queued (the bucket). A background "leak" drains
     * the queue at a fixed rate. Excess requests overflow and are denied.
     *
     * PRO:  Enforces a perfectly smooth outgoing rate — great for protecting
     *       external APIs against bursts regardless of incoming pattern.
     * CON:  Queue management adds latency; requires a background thread or
     *       virtual-time approximation.
     */
    private final RateLimitConfig config;
    LeakyBucketRateLimiter(RateLimitConfig config) { this.config = config; }

    @Override public boolean isAllowed(String key)     { throw new UnsupportedOperationException("TODO"); }
    @Override public long    remainingQuota(String key){ throw new UnsupportedOperationException("TODO"); }
    @Override public String  algorithmName()           { return "LeakyBucket"; }
}

// ── Sliding Log (exact) ───────────────────────────────────────────────────────
class SlidingLogRateLimiter implements RateLimiter {
    /*
     * Idea: identical to SlidingWindowRateLimiter but backed by a persistent
     * sorted log (Redis ZSET or a DB) so state survives restarts and scales
     * across multiple instances.
     *
     * PRO:  Exact accuracy + distributed.
     * CON:  External dependency; higher latency per check.
     */
    private final RateLimitConfig config;
    SlidingLogRateLimiter(RateLimitConfig config) { this.config = config; }

    @Override public boolean isAllowed(String key)     { throw new UnsupportedOperationException("TODO"); }
    @Override public long    remainingQuota(String key){ throw new UnsupportedOperationException("TODO"); }
    @Override public String  algorithmName()           { return "SlidingLog"; }
}
