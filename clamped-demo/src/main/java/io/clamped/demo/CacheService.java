package io.clamped.demo;

import io.clamped.Clamped;
import io.clamped.Severity;

public class CacheService {

    public static void warmUp() {
        Clamped.flag("Redis cache miss on startup — falling back to DB", ctx -> ctx
            .tag("cache")
            .severity(Severity.LOW)
            .meta("key", "user-session-store")
            .meta("fallback", true)
            .meta("host", "redis-primary.internal"));
    }
}
