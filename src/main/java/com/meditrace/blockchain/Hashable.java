package com.meditrace.blockchain;

/**
 * Anything that can produce a deterministic SHA-256 fingerprint of its own contents.
 * <p>
 * Implemented by {@link Block} (block hash), and by the domain objects
 * {@link com.meditrace.model.Batch} and {@link com.meditrace.model.Transaction}, whose
 * fingerprints are sealed inside blocks so that later edits to their database rows can be
 * detected.
 */
public interface Hashable {

    /**
     * Recomputes the hash from the object's current field values.
     *
     * @return 64-character lowercase hex SHA-256 digest
     */
    String computeHash();
}
