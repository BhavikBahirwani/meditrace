package com.meditrace.exception;

/**
 * Base class of every application-specific exception in MediTrace.
 * <p>
 * It is a <b>checked</b> exception on purpose: callers (servlets, services) are forced by
 * the compiler to decide how to handle data, security and tamper problems instead of
 * silently letting them bubble up as generic runtime errors.
 * <p>
 * Hierarchy:
 * <pre>
 * MediTraceException
 *  ├── DataAccessException        (JDBC / SQL failures)
 *  ├── TamperDetectedException    (hash or chain-link mismatch)
 *  ├── ProductNotFoundException   (unknown pack code)
 *  ├── UnauthorizedException      (wrong role for a page / action)
 *  └── AuthenticationException    (bad login / registration data)
 * </pre>
 */
public class MediTraceException extends Exception {

    private static final long serialVersionUID = 1L;

    public MediTraceException(String message) {
        super(message);
    }

    public MediTraceException(String message, Throwable cause) {
        super(message, cause);
    }
}
