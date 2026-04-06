package ratelimiter;

import ratelimiter.core.RateLimitConfig;
import ratelimiter.core.RateLimiter;
import ratelimiter.factory.RateLimiterFactory;
import ratelimiter.factory.RateLimiterFactory.Algorithm;
import ratelimiter.gateway.ExternalClient;
import ratelimiter.gateway.ExternalResourceGateway;
import ratelimiter.registry.RateLimiterRegistry;
import ratelimiter.service.DataEnrichmentService;

import java.time.Duration;

/**
 * AppWiring — shows full system assembly and algorithm switching.
 *
 * This is where you change algorithms without touching any business logic.
 * Swap FIXED_WINDOW ↔ SLIDING_WINDOW in one line; everything else stays.
 */
public class AppWiring {

    public static void main(String[] args) {

        // ── 1. Configuration ────────────────────────────────────────────────
        RateLimitConfig config = RateLimitConfig.of(5, Duration.ofMinutes(1)); // 5 req/min

        // ── 2. Choose algorithm — swap this one line to change behavior ─────
        Algorithm chosenAlgorithm = Algorithm.SLIDING_WINDOW;  // or FIXED_WINDOW

        RateLimiter limiter = RateLimiterFactory.create(chosenAlgorithm, config);
        System.out.println("Using algorithm: " + limiter.algorithmName());

        // ── 3. Register policies ────────────────────────────────────────────
        RateLimiterRegistry registry = new RateLimiterRegistry();
        registry.register("openai", limiter);
        // registry.register("stripe", RateLimiterFactory.create(FIXED_WINDOW, stripeConfig));

        // ── 4. Wire gateway with a mock external client ─────────────────────
        ExternalClient mockClient = request -> "[EXTERNAL RESPONSE for: " + request + "]";
        ExternalResourceGateway gateway = new ExternalResourceGateway(registry, mockClient);

        // ── 5. Wire the service ─────────────────────────────────────────────
        DataEnrichmentService service = new DataEnrichmentService(gateway);

        // ── 6. Simulate requests for tenant T1 ─────────────────────────────
        String tenant = "T1";
        String[] requests = {
            "plain-data",            // no external call — local transform
            "ENRICH:data-1",         // needs external call → allowed (1/5)
            "ENRICH:data-2",         // allowed (2/5)
            "ENRICH:data-3",         // allowed (3/5)
            "ENRICH:data-4",         // allowed (4/5)
            "ENRICH:data-5",         // allowed (5/5) — quota full
            "ENRICH:data-6",         // DENIED — rate limit exceeded
            "ENRICH:data-1",         // cache hit — no quota consumed
        };

        for (String req : requests) {
            String result = service.enrich(tenant, req);
            System.out.println("Request [" + req + "] → " + result);
        }
    }
}
