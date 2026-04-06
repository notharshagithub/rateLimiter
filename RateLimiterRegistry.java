package ratelimiter.registry;

import ratelimiter.core.RateLimiter;
import ratelimiter.core.RateLimitResult;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry — holds one RateLimiter instance per named policy.
 *
 * Use this when different external providers, tenants, or API keys
 * need different limits. Example setup:
 *
 *   registry.register("openai",   fixedWindowLimiter);   // 100 req/min
 *   registry.register("stripe",   slidingWindowLimiter); // 1000 req/hr
 *   registry.register("T1",       perCustomerLimiter);   // 5 req/min
 *
 * The internal service does:
 *   registry.check("openai", tenantId)
 *
 * The key passed to check() is the sub-key within that policy
 * (e.g. tenantId so different tenants don't share quota).
 */
public class RateLimiterRegistry {

    // policyName → RateLimiter
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public void register(String policyName, RateLimiter limiter) {
        limiters.put(policyName, limiter);
    }

    /**
     * Performs the rate limit check for a named policy and sub-key.
     *
     * @param policyName  e.g. "openai" or "stripe"
     * @param subKey      e.g. tenantId or customerId
     * @return RateLimitResult with allow/deny decision and metadata
     * @throws IllegalArgumentException if policy is not registered
     */
    public RateLimitResult check(String policyName, String subKey) {
        RateLimiter limiter = limiters.get(policyName);
        if (limiter == null) {
            throw new IllegalArgumentException("No rate limiter registered for policy: " + policyName);
        }

        // Composite key = policyName + subKey so two tenants don't share buckets
        String compositeKey = policyName + ":" + subKey;

        boolean allowed = limiter.isAllowed(compositeKey);
        long remaining  = limiter.remainingQuota(compositeKey);

        return allowed
            ? RateLimitResult.allowed(remaining)
            : RateLimitResult.denied(0, "Quota exceeded for [" + policyName + "] key=" + subKey);
    }

    public boolean isRegistered(String policyName) {
        return limiters.containsKey(policyName);
    }
}
