package com.meditrace.exception;

/**
 * Thrown when a logged-in user tries to open a page or perform an action that their
 * role is not allowed to (e.g. a Pharmacy trying to register a new batch).
 */
public class UnauthorizedException extends MediTraceException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
