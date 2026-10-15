package com.meditrace.model;

/**
 * Lifecycle of a single pack. Matches the {@code packs.status} ENUM column.
 */
public enum PackStatus {
    /** Still moving through manufacturer / distributor / pharmacy. */
    IN_CHAIN,
    /** Sold to a customer by a pharmacy. A later scan of the same code is suspicious. */
    SOLD
}
