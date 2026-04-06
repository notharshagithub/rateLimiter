package ratelimiter.gateway;

/**
 * Abstraction over the actual external HTTP/SDK client.
 * Inject a mock in tests; inject the real client in production.
 */
public interface ExternalClient {
    String execute(String request);
}


// ── RateLimitExceededException ────────────────────────────────────────────────
// Separate file in a real project; kept here for brevity.

class RateLimitExceededException extends Exception {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
