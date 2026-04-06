package ratelimiter.gateway;

import ratelimiter.core.RateLimitResult;
import ratelimiter.registry.RateLimiterRegistry;

/**
 * ExternalResourceGateway — the single choke-point for external calls.
 *
 * This is the facade internal services use. It:
 *   1. Consults the rate limiter registry.
 *   2. If allowed, delegates to the actual external client.
 *   3. If denied, throws RateLimitExceededException.
 *
 * Internal services NEVER call ExternalClient directly — they go through here.
 * This is the only place rate limiting logic is enforced.
 *
 * Design pattern: Facade + Guard Clause.
 */
public class ExternalResourceGateway {

    private final RateLimiterRegistry registry;
    private final ExternalClient       externalClient;

    public ExternalResourceGateway(RateLimiterRegistry registry,
                                   ExternalClient externalClient) {
        this.registry       = registry;
        this.externalClient = externalClient;
    }

    /**
     * Attempts an external call on behalf of the given tenant/key.
     *
     * @param policyName   which rate limit policy applies (e.g. "openai")
     * @param tenantId     who is making the call (used as the sub-key)
     * @param request      opaque request payload
     * @return             response from the external resource
     * @throws RateLimitExceededException if quota is exhausted
     */
    public String call(String policyName, String tenantId, String request)
            throws RateLimitExceededException {

        RateLimitResult result = registry.check(policyName, tenantId);

        if (!result.isAllowed()) {
            throw new RateLimitExceededException(
                "Rate limit exceeded for tenant=" + tenantId
                + " policy=" + policyName
                + ". Reason: " + result.getReason()
            );
        }

        // Quota OK — proceed with the actual external call
        return externalClient.execute(request);
    }
}
