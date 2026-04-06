package ratelimiter.core;

/**
 * Rich result of a rate limit check.
 *
 * Preferred over a plain boolean — callers can inspect retry-after,
 * remaining quota, and the reason for denial without extra calls.
 */
public final class RateLimitResult {

    private final boolean allowed;
    private final long    remainingQuota;
    private final long    retryAfterMillis;   // 0 if allowed
    private final String  reason;

    private RateLimitResult(boolean allowed, long remainingQuota,
                            long retryAfterMillis, String reason) {
        this.allowed          = allowed;
        this.remainingQuota   = remainingQuota;
        this.retryAfterMillis = retryAfterMillis;
        this.reason           = reason;
    }

    public static RateLimitResult allowed(long remainingQuota) {
        return new RateLimitResult(true, remainingQuota, 0, "OK");
    }

    public static RateLimitResult denied(long retryAfterMillis) {
        return new RateLimitResult(false, 0, retryAfterMillis, "Rate limit exceeded");
    }

    public static RateLimitResult denied(long retryAfterMillis, String reason) {
        return new RateLimitResult(false, 0, retryAfterMillis, reason);
    }

    public boolean isAllowed()           { return allowed; }
    public long    getRemainingQuota()   { return remainingQuota; }
    public long    getRetryAfterMillis() { return retryAfterMillis; }
    public String  getReason()           { return reason; }

    @Override
    public String toString() {
        return allowed
            ? "ALLOWED (remaining=" + remainingQuota + ")"
            : "DENIED  (retryAfter=" + retryAfterMillis + "ms, reason=" + reason + ")";
    }
}
