package ratelimiter.algorithms;

import ratelimiter.core.RateLimitConfig;
import ratelimiter.core.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed Window Counter algorithm.
 *
 * How it works:
 *   - Time is divided into fixed windows (e.g. every 60 seconds).
 *   - Each key gets a counter that resets at the start of each window.
 *   - A request is allowed if counter < maxRequests.
 *
 * Trade-offs:
 *   PRO:  O(1) time and O(keys) space. Very fast.
 *   CON:  Boundary burst problem — a client can fire 2× quota around a window edge.
 *         e.g. 5 req at t=59s and 5 req at t=61s both pass, but they are 2s apart.
 *
 * Thread safety: ConcurrentHashMap + synchronized on per-key WindowState.
 */
public class FixedWindowRateLimiter implements RateLimiter {

    private final RateLimitConfig config;

    // key → current window state
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public boolean isAllowed(String key) {
        WindowState state = windows.computeIfAbsent(key, k -> new WindowState());
        synchronized (state) {
            long now = System.currentTimeMillis();
            state.evictIfExpired(now, config.getWindowMillis());
            if (state.count < config.getMaxRequests()) {
                state.count++;
                return true;
            }
            return false;
        }
    }

    @Override
    public long remainingQuota(String key) {
        WindowState state = windows.get(key);
        if (state == null) return config.getMaxRequests();
        synchronized (state) {
            long now = System.currentTimeMillis();
            state.evictIfExpired(now, config.getWindowMillis());
            return Math.max(0, config.getMaxRequests() - state.count);
        }
    }

    @Override
    public String algorithmName() { return "FixedWindowCounter"; }

    // ── Inner state ──────────────────────────────────────────────────────────

    private static final class WindowState {
        long windowStart = System.currentTimeMillis();
        long count       = 0;

        void evictIfExpired(long now, long windowMillis) {
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                count       = 0;
            }
        }
    }
}
