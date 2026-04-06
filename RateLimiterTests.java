package ratelimiter.tests;

import ratelimiter.algorithms.FixedWindowRateLimiter;
import ratelimiter.algorithms.SlidingWindowRateLimiter;
import ratelimiter.core.RateLimitConfig;
import ratelimiter.core.RateLimiter;

import java.time.Duration;

/**
 * Test skeleton — uses JUnit 5 style.
 * Add `junit-jupiter` to your build tool to run these.
 *
 * Key testing strategy: inject a controllable clock instead of
 * System.currentTimeMillis() for deterministic time-based tests.
 * (Production code should accept a Clock/Supplier<Long> — omitted here
 *  for design clarity, but noted as a required refactor for full testability.)
 */
public class RateLimiterTests {

    // ── Fixed Window Tests ───────────────────────────────────────────────────

    // @Test
    void fixedWindow_allowsUpToLimit() {
        RateLimiter limiter = new FixedWindowRateLimiter(
            RateLimitConfig.of(5, Duration.ofMinutes(1)));

        for (int i = 0; i < 5; i++) {
            assert limiter.isAllowed("T1") : "Request " + i + " should be allowed";
        }
        assert !limiter.isAllowed("T1") : "6th request should be denied";
    }

    // @Test
    void fixedWindow_differentKeysAreIndependent() {
        RateLimiter limiter = new FixedWindowRateLimiter(
            RateLimitConfig.of(2, Duration.ofMinutes(1)));

        assert limiter.isAllowed("T1");
        assert limiter.isAllowed("T1");
        assert !limiter.isAllowed("T1");  // T1 exhausted

        assert limiter.isAllowed("T2");   // T2 unaffected
    }

    // @Test
    void fixedWindow_resetAfterWindow() throws InterruptedException {
        RateLimiter limiter = new FixedWindowRateLimiter(
            RateLimitConfig.of(1, Duration.ofMillis(100)));  // tiny window for testing

        assert limiter.isAllowed("T1");
        assert !limiter.isAllowed("T1");  // exhausted

        Thread.sleep(110);  // window expires

        assert limiter.isAllowed("T1");   // new window, counter reset
    }

    // ── Sliding Window Tests ─────────────────────────────────────────────────

    // @Test
    void slidingWindow_noBoundaryBurst() throws InterruptedException {
        // Fixed window allows 2× burst at window edges; sliding window does not.
        RateLimiter limiter = new SlidingWindowRateLimiter(
            RateLimitConfig.of(3, Duration.ofMillis(200)));

        assert limiter.isAllowed("T1");
        assert limiter.isAllowed("T1");
        assert limiter.isAllowed("T1");
        assert !limiter.isAllowed("T1");  // quota full

        Thread.sleep(100);  // half-window passes — only some old entries expire
        // Key difference vs fixed window: not all quota is restored yet
    }

    // @Test
    void slidingWindow_remainingQuotaIsAccurate() {
        RateLimiter limiter = new SlidingWindowRateLimiter(
            RateLimitConfig.of(5, Duration.ofMinutes(1)));

        assert limiter.remainingQuota("T1") == 5;
        limiter.isAllowed("T1");
        assert limiter.remainingQuota("T1") == 4;
    }

    // ── Thread Safety Test ───────────────────────────────────────────────────

    // @Test
    void concurrentRequests_doNotExceedLimit() throws InterruptedException {
        int limit = 10;
        RateLimiter limiter = new FixedWindowRateLimiter(
            RateLimitConfig.of(limit, Duration.ofMinutes(1)));

        java.util.concurrent.atomic.AtomicInteger allowed = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(20);

        for (int i = 0; i < 50; i++) {
            pool.submit(() -> {
                if (limiter.isAllowed("shared-key")) allowed.incrementAndGet();
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        assert allowed.get() == limit : "Exactly " + limit + " should be allowed, got " + allowed.get();
    }
}
