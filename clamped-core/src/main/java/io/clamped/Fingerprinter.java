package io.clamped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates a stable SHA-256 fingerprint for deduplication.
 *
 * For exceptions: uses the exception class name + top N "at ..." stack frames
 * (ignores the exception message which often contains variable data like IDs or timestamps).
 *
 * For flags: uses "FLAG:" + tag + "|" + message.
 */
public final class Fingerprinter {

    private static final int TOP_FRAMES = 5;

    private Fingerprinter() {}

    public static String generate(String exceptionClass, String stacktrace, String message, String tag) {
        String input;
        if (exceptionClass != null && stacktrace != null) {
            StringBuilder sb = new StringBuilder(exceptionClass);
            int frameCount = 0;
            for (String line : stacktrace.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("at ") && frameCount < TOP_FRAMES) {
                    sb.append('\n').append(trimmed);
                    frameCount++;
                }
            }
            input = sb.toString();
        } else {
            input = "FLAG:" + (tag != null ? tag : "") + "|" + (message != null ? message : "");
        }
        return sha256(input);
    }

    // Cryptographic Hashing (taking a key and converting to a fixed representaiton)
    // SHA-256 used to compress stack traces into a stable 64-char identifier for deduplication in the DB.
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
