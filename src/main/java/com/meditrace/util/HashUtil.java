package com.meditrace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 helper used for block hashes and record fingerprints.
 * <p>
 * Utility class: {@code final} with a private constructor, only static methods.
 */
public final class HashUtil {

    private HashUtil() {
        // no instances
    }

    /**
     * @param input any text (encoded as UTF-8 before hashing)
     * @return lowercase hex SHA-256 digest, always 64 characters
     */
    public static String sha256Hex(String input) {
        try {
            // MessageDigest is not thread-safe, so a new instance is created per call.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform is required to support SHA-256, so this cannot happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
