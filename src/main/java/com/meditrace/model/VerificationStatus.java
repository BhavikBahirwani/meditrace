package com.meditrace.model;

/**
 * Outcome of a customer scan. Also stored in {@code scans.result}.
 * Each constant carries the text and CSS class the verify page uses.
 */
public enum VerificationStatus {

    GENUINE("Genuine", "genuine",
            "This pack is registered and its records match the blockchain."),
    TAMPERED("Tampered", "tampered",
            "The records for this pack were changed after they were sealed on the blockchain."),
    POSSIBLE_FAKE("Possible fake", "warning",
            "This code was already sold to a customer. The QR on your pack may have been copied."),
    NOT_FOUND("Not found", "notfound",
            "No medicine pack is registered with this code.");

    private final String label;
    private final String cssClass;
    private final String defaultMessage;

    VerificationStatus(String label, String cssClass, String defaultMessage) {
        this.label = label;
        this.cssClass = cssClass;
        this.defaultMessage = defaultMessage;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /** Needed so JSP EL can compare, e.g. {@code ${result.status.name == 'GENUINE'}}. */
    public String getName() {
        return name();
    }
}
