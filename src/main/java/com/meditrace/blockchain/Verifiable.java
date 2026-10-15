package com.meditrace.blockchain;

/**
 * Anything that can check its own integrity.
 * <p>
 * A {@link Block} verifies that its stored hash still matches its contents; a
 * {@link Blockchain} verifies every block plus every link between consecutive blocks.
 */
public interface Verifiable {

    /**
     * @return {@code true} if the object is intact, {@code false} if it was tampered with
     */
    boolean verify();
}
