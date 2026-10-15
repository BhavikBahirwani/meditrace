package com.meditrace.model;

/**
 * Kind of supply-chain event recorded in {@code transactions.tx_type}.
 */
public enum TxType {
    /** Manufacturer creates a batch and its packs. */
    REGISTER,
    /** Custody moves from one participant to the next. */
    HANDOVER
}
