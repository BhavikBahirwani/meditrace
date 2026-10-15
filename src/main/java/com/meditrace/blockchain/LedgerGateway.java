package com.meditrace.blockchain;

import com.meditrace.exception.MediTraceException;

import java.util.List;

/**
 * The bridge between the pure-Java blockchain package and the database.
 * <p>
 * The {@link Miner} and {@link ChainValidator} only know this interface, not JDBC; the
 * real implementation is {@link com.meditrace.service.LedgerService}. Unit tests can plug
 * in a simple in-memory fake. (Dependency inversion: high-level code depends on an
 * abstraction, not on the DAO classes.)
 */
public interface LedgerGateway {

    /**
     * Builds the JSON payload that will be sealed into the block for this task.
     */
    String buildPayload(MiningTask task) throws MediTraceException;

    /**
     * Saves the mined block and links the transaction to it, atomically.
     */
    void persist(Block block, MiningTask task) throws MediTraceException;

    /**
     * Loads every stored block ordered by index (used by the periodic validator).
     */
    List<Block> loadAllBlocks() throws MediTraceException;
}
