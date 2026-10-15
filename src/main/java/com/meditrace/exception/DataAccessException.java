package com.meditrace.exception;

/**
 * Thrown by the DAO layer when a database operation fails.
 * <p>
 * Wraps the low-level {@link java.sql.SQLException} so that upper layers (services,
 * servlets) never depend on JDBC classes directly - an example of exception translation.
 */
public class DataAccessException extends MediTraceException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
