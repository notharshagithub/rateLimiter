package ratelimiter.core;

/**
 * Core abstraction for all rate limiting algorithms.
 *
 * Strategy Pattern: each algorithm implements this interface.
 * Internal services call isAllowed() before making any external resource call.
 * The caller never knows which algorithm is running underneath.
 */
public interface RateLimiter {

    /**
     * Checks whether the caller identified by `key` is within quota.
     *
     * @param key   the rate limiting key (tenant ID, API key, customer ID, etc.)
     * @return true if the call is allowed; false if quota is exhausted
     */
    boolean isAllowed(String key);

    /**
     * Returns remaining quota for the key in the current window.
     * Useful for adding X-RateLimit-Remaining headers or dashboards.
     */
    long remainingQuota(String key);

    /**
     * Returns the algorithm name — used for logging/monitoring.
     */
    String algorithmName();
}
