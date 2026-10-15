package com.meditrace.util;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generates unique pack codes such as {@code MT-B1023-7F3A9C}.
 * <p>
 * {@link SecureRandom} (not {@code java.util.Random}) is used so codes cannot be predicted
 * by a counterfeiter who has seen a few genuine ones.
 */
public final class PackCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "MT-";

    private PackCodeGenerator() {
        // no instances
    }

    /** One random code for the given batch number. */
    public static String generate(String batchNo) {
        byte[] bytes = new byte[3];                       // 3 bytes = 6 hex characters
        RANDOM.nextBytes(bytes);
        return PREFIX + batchNo + "-" + HexFormat.of().withUpperCase().formatHex(bytes);
    }

    /**
     * {@code count} distinct codes for one batch. A {@link Set} guarantees no duplicates
     * inside the batch; the database UNIQUE constraint guards across batches.
     */
    public static Set<String> generateUnique(String batchNo, int count) {
        Set<String> codes = new LinkedHashSet<>();
        while (codes.size() < count) {
            codes.add(generate(batchNo));
        }
        return codes;
    }
}
