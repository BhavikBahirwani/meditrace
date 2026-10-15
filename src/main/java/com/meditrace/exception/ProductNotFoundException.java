package com.meditrace.exception;

/**
 * Thrown when a scanned / typed pack code does not exist in the MediTrace registry.
 */
public class ProductNotFoundException extends MediTraceException {

    private static final long serialVersionUID = 1L;

    private final String packCode;

    public ProductNotFoundException(String packCode) {
        super("No medicine pack is registered with code " + packCode);
        this.packCode = packCode;
    }

    public String getPackCode() {
        return packCode;
    }
}
