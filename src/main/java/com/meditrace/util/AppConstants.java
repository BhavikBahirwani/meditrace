package com.meditrace.util;

/**
 * Names and settings shared across servlets, filters and the listener, kept in one place
 * so a typo in an attribute name cannot silently break the app.
 */
public final class AppConstants {

    private AppConstants() {
        // no instances
    }

    // ---- HttpSession attributes
    /** The logged-in {@link com.meditrace.model.User}. */
    public static final String SESSION_USER = "currentUser";
    /** One-time success / info message shown after a redirect. */
    public static final String SESSION_FLASH = "flash";

    // ---- ServletContext attributes (application-wide singletons created by the listener)
    public static final String CTX_BLOCKCHAIN = "meditrace.blockchain";
    public static final String CTX_MINER = "meditrace.miner";
    public static final String CTX_VALIDATOR = "meditrace.validator";
    public static final String CTX_STARTUP_ERROR = "meditrace.startupError";

    // ---- Blockchain settings
    public static final int POW_DIFFICULTY = 2;
    /** Full-chain audit interval: every 2 minutes. */
    public static final long VALIDATOR_INTERVAL_SECONDS = 120;

    // ---- Business limits
    public static final int MAX_PACKS_PER_BATCH = 500;
    public static final int SESSION_TIMEOUT_MINUTES = 30;

    // ---- JSP locations (inside WEB-INF so they cannot be opened directly)
    public static final String JSP_DIR = "/WEB-INF/jsp/";
}
