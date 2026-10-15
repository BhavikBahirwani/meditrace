package com.meditrace.exception;

/**
 * Thrown when login fails (wrong username / password, too many attempts) or when
 * registration data is invalid. The message is safe to show to the user.
 */
public class AuthenticationException extends MediTraceException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
