package ratelimiter.algorithms;

import ratelimiter.core.RateLimitConfig;
import ratelimiter.core.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sliding Window Counter algorithm.
 *
 * How it works:
 *   - Instead of resetting at a fixed boundary, the window "slides" with time.
 *   - Stores a queue of timestamps of past requests per key.
 *   - On each check: drop timestamps older than (now - windowDuration), then
 *     count remaining entries. Allow if count < maxRequests.
 *
 * Trade-offs:
 *   PRO:  Eliminates the boundary burst problem. Accurate rolling window.
 *   CON:  O(maxRequests) space per key (stores one timestamp per request).
 *         O(n) cleanup per call in the worst case, though amortized O(1).
 *
 * Alternative (approximate):
 *   A weighted blend of two fixed windows can approximate this in O(1) space.
 *   That variant is noted in SlidingWindowApproxRateLimiter (stub).
 *
 * Thread safety: per-key synchronization on the Deque wrapper.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final RateLimitConfig config;

    private final ConcurrentHashMap<String, TimestampWindow> windows = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public boolean isAllowed(String key) {
        TimestampWindow window = windows.computeIfAbsent(key, k -> new TimestampWindow());
        synchronized (window) {
            long now = System.currentTimeMillis();
            window.evictOldEntries(now, config.getWindowMillis());
            if (window.timestamps.size() < config.getMaxRequests()) {
                window.timestamps.addLast(now);
                return true;
            }
            return false;
        }
    }

    @Override
    public long remainingQuota(String key) {
        TimestampWindow window = windows.get(key);
        if (window == null) return config.getMaxRequests();
        synchronized (window) {
            long now = System.currentTimeMillis();
            window.evictOldEntries(now, config.getWindowMillis());
            return Math.max(0, config.getMaxRequests() - window.timestamps.size());
        }
    }

    @Override
    public String algorithmName() { return "SlidingWindowCounter"; }

    // ── Inner state ──────────────────────────────────────────────────────────

    private static final class TimestampWindow {
        // Deque keeps timestamps in insertion order (oldest at head).
        final Deque<Long> timestamps = new ArrayDeque<>();

        void evictOldEntries(long now, long windowMillis) {
            long cutoff = now - windowMillis;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
        }
    }
}
