package com.meditrace.exception;

/**
 * Thrown when a block's stored hash no longer matches its contents, when two blocks are
 * no longer linked by {@code prevHash}, or when a database row no longer matches the
 * fingerprint that was sealed into the blockchain.
 */
public class TamperDetectedException extends MediTraceException {

    private static final long serialVersionUID = 1L;

    /** Index of the first block found to be invalid, or -1 if not tied to one block. */
    private final int blockIndex;

    public TamperDetectedException(String message, int blockIndex) {
        super(message);
        this.blockIndex = blockIndex;
    }

    public int getBlockIndex() {
        return blockIndex;
    }
}
