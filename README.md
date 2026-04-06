# Pluggable Rate Limiting System

A backend rate limiting module that controls access to paid external resources. Rate limiting is applied **only at the point of external call** — not on every incoming API request.

---



## Overview

```
Client API Request
      │
      ▼
DataEnrichmentService   ← business logic runs first
      │
      │  only if external call is needed
      ▼
ExternalResourceGateway ← single choke-point
      │
      ▼
RateLimiterRegistry     ← looks up policy by name
      │
      ▼
RateLimiter             ← algorithm decides allow/deny
      │
      ▼
ExternalClient          ← paid external API
```

---

## Class Diagram

```mermaid
classDiagram

  %% ── Core interface ─────────────────────────────────────────────
  class RateLimiter {
    <<interface>>
    + isAllowed(key: String) boolean
    + remainingQuota(key: String) long
    + algorithmName() String
  }

  %% ── Algorithm implementations ──────────────────────────────────
  class FixedWindowRateLimiter {
    - config: RateLimitConfig
    - windows: ConcurrentHashMap
    + isAllowed(key: String) boolean
    + remainingQuota(key: String) long
    + algorithmName() String
  }

  class SlidingWindowRateLimiter {
    - config: RateLimitConfig
    - windows: ConcurrentHashMap
    + isAllowed(key: String) boolean
    + remainingQuota(key: String) long
    + algorithmName() String
  }

  %% ── Value objects ───────────────────────────────────────────────
  class RateLimitConfig {
    - maxRequests: long
    - windowDuration: Duration
    + of(max: long, duration: Duration) RateLimitConfig$
    + getMaxRequests() long
    + getWindowMillis() long
    + getWindowDuration() Duration
  }

  class RateLimitResult {
    - allowed: boolean
    - remainingQuota: long
    - retryAfterMillis: long
    - reason: String
    + allowed(remaining: long) RateLimitResult$
    + denied(retryAfter: long) RateLimitResult$
    + isAllowed() boolean
    + getRemainingQuota() long
    + getRetryAfterMillis() long
    + getReason() String
  }

  %% ── Factory ─────────────────────────────────────────────────────
  class RateLimiterFactory {
    <<static>>
    + create(algo: Algorithm, config: RateLimitConfig) RateLimiter$
  }

  class Algorithm {
    <<enumeration>>
    FIXED_WINDOW
    SLIDING_WINDOW
    TOKEN_BUCKET
    LEAKY_BUCKET
    SLIDING_LOG
  }

  %% ── Registry ────────────────────────────────────────────────────
  class RateLimiterRegistry {
    - limiters: ConcurrentHashMap~String, RateLimiter~
    + register(policyName: String, limiter: RateLimiter) void
    + check(policyName: String, subKey: String) RateLimitResult
    + isRegistered(policyName: String) boolean
  }

  %% ── Gateway ─────────────────────────────────────────────────────
  class ExternalResourceGateway {
    - registry: RateLimiterRegistry
    - externalClient: ExternalClient
    + call(policy: String, tenantId: String, request: String) String
  }

  class ExternalClient {
    <<interface>>
    + execute(request: String) String
  }

  class RateLimitExceededException {
    + RateLimitExceededException(message: String)
  }

  %% ── Service ─────────────────────────────────────────────────────
  class DataEnrichmentService {
    - gateway: ExternalResourceGateway
    - localCache: Map
    + enrich(tenantId: String, inputData: String) String
    - requiresExternalCall(data: String) boolean
    - applyLocalTransform(data: String) String
    - handleRateLimitDenial(tenantId: String, e: Exception) String
  }

  %% ── Relationships ───────────────────────────────────────────────
  RateLimiter <|.. FixedWindowRateLimiter        : implements
  RateLimiter <|.. SlidingWindowRateLimiter      : implements

  FixedWindowRateLimiter   --> RateLimitConfig   : uses
  SlidingWindowRateLimiter --> RateLimitConfig   : uses

  RateLimiterFactory ..> RateLimiter             : creates
  RateLimiterFactory ..> RateLimitConfig         : uses
  RateLimiterFactory --> Algorithm               : uses

  RateLimiterRegistry --> RateLimiter            : stores
  RateLimiterRegistry ..> RateLimitResult        : returns

  ExternalResourceGateway --> RateLimiterRegistry : uses
  ExternalResourceGateway --> ExternalClient      : calls
  ExternalResourceGateway ..> RateLimitExceededException : throws

  DataEnrichmentService --> ExternalResourceGateway : uses
```

> **Render this diagram** in any Mermaid-compatible viewer: GitHub, GitLab, VS Code (Markdown Preview Mermaid Support), or [mermaid.live](https://mermaid.live).

