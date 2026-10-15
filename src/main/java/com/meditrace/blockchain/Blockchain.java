package com.meditrace.blockchain;

import com.meditrace.exception.TamperDetectedException;
import com.meditrace.util.JsonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The in-memory MediTrace ledger: an ordered {@code List<Block>} starting with a genesis block.
 * <p>
 * This class is pure Java (no JDBC) so it can be unit-tested on its own. Persistence is
 * done by {@link Miner} through a {@link LedgerGateway}; at start-up the chain is rebuilt
 * from the {@code blocks} table with {@link #fromBlocks(List, int)}.
 * <p>
 * <b>Thread safety:</b> the Miner thread appends blocks while servlet threads and the
 * {@link ChainValidator} read them, so every method that touches the list is
 * {@code synchronized} on this object, and readers receive immutable snapshots.
 */
public class Blockchain implements Verifiable {

    /** prevHash of the genesis block: 64 zeros. */
    public static final String ZERO_HASH = "0".repeat(64);

    /** Default proof-of-work difficulty (leading zeros). */
    public static final int DEFAULT_DIFFICULTY = 2;

    private final List<Block> chain = new ArrayList<>();
    /** Fast lookup of a block by its hash (Map cache over the same blocks). */
    private final Map<String, Block> blocksByHash = new HashMap<>();
    private final int difficulty;

    /** Creates a brand-new chain containing only a freshly mined genesis block. */
    public Blockchain(int difficulty) {
        this.difficulty = difficulty;
        append(createGenesisBlock(difficulty));
    }

    /** Private constructor used by {@link #fromBlocks(List, int)}. */
    private Blockchain(int difficulty, List<Block> existing) {
        this.difficulty = difficulty;
        for (Block block : existing) {
            append(block);
        }
    }

    /**
     * Rebuilds a chain from blocks loaded from the database (ordered by index).
     * The blocks are taken as-is; call {@link #validate()} to check them.
     *
     * @throws IllegalArgumentException if the list is empty
     */
    public static Blockchain fromBlocks(List<Block> blocks, int difficulty) {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("Cannot rebuild a chain from zero blocks");
        }
        return new Blockchain(difficulty, blocks);
    }

    /** Builds and mines block #0, the fixed starting point of every chain. */
    public static Block createGenesisBlock(int difficulty) {
        String data = JsonUtil.object()
                .put("type", "GENESIS")
                .put("network", "MediTrace India")
                .put("note", "Scan. Verify. Trust your medicine.")
                .toJson();
        Block genesis = new Block(0, ZERO_HASH, System.currentTimeMillis(), data);
        genesis.mine(difficulty);
        return genesis;
    }

    // ---------------------------------------------------------------- writing

    /**
     * Creates and mines the next block on top of the current tip, WITHOUT adding it.
     * The Miner first saves the block to the database and only then calls
     * {@link #addBlock(Block)}, so memory and database never disagree.
     */
    public synchronized Block prepareNextBlock(String dataJson) {
        Block tip = getLatestBlock();
        Block next = new Block(tip.getIndex() + 1, tip.getHash(), System.currentTimeMillis(), dataJson);
        next.mine(difficulty);
        return next;
    }

    /**
     * Appends a mined block after checking that it is valid and correctly linked to the tip.
     *
     * @throws TamperDetectedException if the block's hash, proof-of-work or link is wrong
     */
    public synchronized void addBlock(Block block) throws TamperDetectedException {
        Block tip = getLatestBlock();
        if (block.getIndex() != tip.getIndex() + 1) {
            throw new TamperDetectedException("Block index " + block.getIndex()
                    + " does not follow tip index " + tip.getIndex(), block.getIndex());
        }
        if (!block.getPrevHash().equals(tip.getHash())) {
            throw new TamperDetectedException("Block " + block.getIndex()
                    + " is not linked to the current tip (prevHash mismatch)", block.getIndex());
        }
        if (!block.verify() || !block.meetsDifficulty(difficulty)) {
            throw new TamperDetectedException("Block " + block.getIndex()
                    + " has an invalid hash or proof-of-work", block.getIndex());
        }
        append(block);
    }

    private void append(Block block) {
        chain.add(block);
        blocksByHash.put(block.getHash(), block);
    }

    // ---------------------------------------------------------------- verification

    /** {@inheritDoc} Verifies every hash, every link and the proof-of-work of the whole chain. */
    @Override
    public synchronized boolean verify() {
        return validate(chain, difficulty).isValid();
    }

    /**
     * Same as {@link #verify()} but throws an exception that says exactly where the chain broke.
     *
     * @throws TamperDetectedException at the first invalid block
     */
    public synchronized void validate() throws TamperDetectedException {
        ValidationReport report = validate(chain, difficulty);
        if (!report.isValid()) {
            throw new TamperDetectedException(report.getMessage(), report.getBrokenAtIndex());
        }
    }

    /**
     * Core validation algorithm, usable on any list of blocks (e.g. freshly read from MySQL):
     * <ol>
     *   <li>block indexes must be 0, 1, 2, ... with no gaps;</li>
     *   <li>each stored hash must equal a recomputed hash (detects edited data / nonce / time);</li>
     *   <li>each hash must meet the proof-of-work difficulty;</li>
     *   <li>each block's prevHash must equal the previous block's hash (detects broken links);</li>
     *   <li>the genesis block must point to {@link #ZERO_HASH}.</li>
     * </ol>
     */
    public static ValidationReport validate(List<Block> blocks, int difficulty) {
        if (blocks == null || blocks.isEmpty()) {
            return ValidationReport.broken(0, 0, "Chain is empty - genesis block missing.");
        }
        for (int i = 0; i < blocks.size(); i++) {
            Block current = blocks.get(i);
            if (current.getIndex() != i) {
                return ValidationReport.broken(i, i, "Block order broken: expected index " + i
                        + " but found " + current.getIndex() + ".");
            }
            if (!current.verify()) {
                return ValidationReport.broken(i, i, "Block " + i
                        + " was modified: stored hash does not match its contents.");
            }
            if (!current.meetsDifficulty(difficulty)) {
                return ValidationReport.broken(i, i, "Block " + i + " does not meet proof-of-work.");
            }
            String expectedPrev = (i == 0) ? ZERO_HASH : blocks.get(i - 1).getHash();
            if (!current.getPrevHash().equals(expectedPrev)) {
                return ValidationReport.broken(i, i, "Broken link: block " + i
                        + " does not point to block " + (i - 1) + ".");
            }
        }
        return ValidationReport.ok(blocks.size());
    }

    // ---------------------------------------------------------------- reading

    public synchronized Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public synchronized Optional<Block> getBlock(int index) {
        return (index >= 0 && index < chain.size()) ? Optional.of(chain.get(index)) : Optional.empty();
    }

    public synchronized Optional<Block> findByHash(String hash) {
        return Optional.ofNullable(blocksByHash.get(hash));
    }

    public synchronized int size() {
        return chain.size();
    }

    /** @return an unmodifiable copy, safe to iterate while the Miner keeps appending */
    public synchronized List<Block> getChainSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    public int getDifficulty() {
        return difficulty;
    }
}
