package com.meditrace.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Input validation and normalisation shared by the servlets.
 * Server-side validation is mandatory: HTML {@code required} attributes can be bypassed.
 */
public final class ValidationUtil {

    /** 3-30 characters: letters, digits, dot, underscore, hyphen. */
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    /** e.g. MT-B1023-7F3A9C : prefix, batch number, 6 hex characters. */
    private static final Pattern PACK_CODE = Pattern.compile("^MT-[A-Z0-9]{2,20}-[0-9A-F]{6}$");
    /** e.g. B1023 */
    private static final Pattern BATCH_NO = Pattern.compile("^[A-Z0-9]{2,20}$");

    public static final int MIN_PASSWORD_LENGTH = 8;

    private ValidationUtil() {
        // no instances
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Trims, and turns blank into null. */
    public static String clean(String s) {
        return isBlank(s) ? null : s.trim();
    }

    public static boolean isValidUsername(String s) {
        return s != null && USERNAME.matcher(s).matches();
    }

    public static boolean isValidEmail(String s) {
        return s != null && s.length() <= 120 && EMAIL.matcher(s).matches();
    }

    public static boolean isValidPassword(String s) {
        return s != null && s.length() >= MIN_PASSWORD_LENGTH && s.length() <= 128;
    }

    /**
     * Normalises what a customer typed or scanned: trims, upper-cases, removes spaces.
     * "mt-b1023-7f3a9c " becomes "MT-B1023-7F3A9C".
     */
    public static String normalisePackCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValidPackCode(String s) {
        return s != null && PACK_CODE.matcher(s).matches();
    }

    public static boolean isValidBatchNo(String s) {
        return s != null && BATCH_NO.matcher(s).matches();
    }

    /** Cuts a string to a maximum length (for columns like user_agent). */
    public static String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }
}
