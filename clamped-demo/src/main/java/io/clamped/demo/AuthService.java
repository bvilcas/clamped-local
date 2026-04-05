package io.clamped.demo;

import io.clamped.Clamped;
import io.clamped.Severity;

public class AuthService {

    public static void login(String userId, String token) {
        try {
            validateToken(token);
        } catch (Exception e) {
            Clamped.add(e, ctx -> ctx
                .tag("auth")
                .severity(Severity.CRITICAL)
                .meta("userId", userId)
                .meta("ip", "203.0.113.42"));
        }
    }

    private static void validateToken(String token) {
        if (token == null) {
            throw new NullPointerException("Token must not be null");
        }
        throw new SecurityException("JWT token expired: " + token);
    }
}
