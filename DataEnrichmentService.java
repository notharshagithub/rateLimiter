package ratelimiter.service;

import ratelimiter.gateway.ExternalResourceGateway;
import ratelimiter.gateway.RateLimitExceededException;

/**
 * Sample internal service — demonstrates the key design requirement:
 *
 *   "Not every API request consumes quota."
 *
 * The rate limiter is consulted ONLY when business logic determines
 * that an external call is actually needed. A request that can be
 * answered from cache never touches the rate limiter.
 */
public class DataEnrichmentService {

    private static final String POLICY = "openai";

    private final ExternalResourceGateway gateway;
    // In production this would be a real cache (Redis, Caffeine, etc.)
    private final java.util.Map<String, String> localCache = new java.util.HashMap<>();

    public DataEnrichmentService(ExternalResourceGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Main service entry point — called by an API controller.
     *
     * @param tenantId   identifies the caller
     * @param inputData  data that may or may not need external enrichment
     * @return           enriched (or cached) result
     */
    public String enrich(String tenantId, String inputData) {

        // ── Step 1: business logic ──────────────────────────────────────────
        if (localCache.containsKey(inputData)) {
            // Cache hit → NO external call → NO rate limit consumed
            return localCache.get(inputData);
        }

        boolean needsExternalEnrichment = requiresExternalCall(inputData);

        if (!needsExternalEnrichment) {
            // Business rule says no external call needed → quota untouched
            return applyLocalTransform(inputData);
        }

        // ── Step 2: rate limit check + external call ────────────────────────
        try {
            String result = gateway.call(POLICY, tenantId, inputData);
            localCache.put(inputData, result);    // populate cache
            return result;

        } catch (RateLimitExceededException e) {
            // Handle gracefully — return degraded response or propagate
            return handleRateLimitDenial(tenantId, e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private boolean requiresExternalCall(String data) {
        // Real business logic here — simplified for the design example
        return data.startsWith("ENRICH:");
    }

    private String applyLocalTransform(String data) {
        return "[local] " + data.toUpperCase();
    }

    private String handleRateLimitDenial(String tenantId, RateLimitExceededException e) {
        // Options: return stale data, throw HTTP 429, queue for retry, etc.
        System.err.println("Rate limit hit for tenant=" + tenantId + ": " + e.getMessage());
        return "[DEGRADED] Request could not be enriched at this time. Try again later.";
    }
}
