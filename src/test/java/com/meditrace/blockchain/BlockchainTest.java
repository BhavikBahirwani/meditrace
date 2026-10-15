package com.meditrace.blockchain;

import com.meditrace.exception.TamperDetectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Blockchain}: a valid chain passes; edited data and broken links
 * are detected, and the exact broken block is reported.
 */
class BlockchainTest {

    private static final int DIFFICULTY = Blockchain.DEFAULT_DIFFICULTY;
    private Blockchain chain;

    /** Builds genesis + 3 blocks before every test. */
    @BeforeEach
    void setUp() throws TamperDetectedException {
        chain = new Blockchain(DIFFICULTY);
        chain.addBlock(chain.prepareNextBlock("{\"type\":\"REGISTER\",\"batchNo\":\"B1023\"}"));
        chain.addBlock(chain.prepareNextBlock("{\"type\":\"HANDOVER\",\"to\":\"ABC Distributors\"}"));
        chain.addBlock(chain.prepareNextBlock("{\"type\":\"HANDOVER\",\"to\":\"Sharma Medicals\"}"));
    }

    @Test
    @DisplayName("New chain starts with a valid genesis block")
    void genesisBlock() {
        Blockchain fresh = new Blockchain(DIFFICULTY);
        Block genesis = fresh.getLatestBlock();
        assertEquals(1, fresh.size());
        assertEquals(0, genesis.getIndex());
        assertEquals(Blockchain.ZERO_HASH, genesis.getPrevHash());
        assertTrue(fresh.verify());
    }

    @Test
    @DisplayName("A valid chain passes verification")
    void validChainPasses() {
        assertEquals(4, chain.size());
        assertTrue(chain.verify());
        assertDoesNotThrow(() -> chain.validate());
        ValidationReport report = Blockchain.validate(chain.getChainSnapshot(), DIFFICULTY);
        assertTrue(report.isValid());
        assertEquals(4, report.getBlocksChecked());
    }

    @Test
    @DisplayName("Edited block data is detected at the right index")
    void tamperedDataDetected() {
        List<Block> blocks = new ArrayList<>(chain.getChainSnapshot());
        Block b2 = blocks.get(2);
        // Someone changes the distributor name in the stored block, keeping the old hash.
        blocks.set(2, new Block(b2.getIndex(), b2.getPrevHash(), b2.getTimestamp(),
                b2.getDataJson().replace("ABC Distributors", "Fake Traders"),
                b2.getNonce(), b2.getHash(), null));

        ValidationReport report = Blockchain.validate(blocks, DIFFICULTY);
        assertFalse(report.isValid());
        assertEquals(2, report.getBrokenAtIndex());
    }

    @Test
    @DisplayName("A tampered block that is re-mined still breaks the link to the next block")
    void remineBreaksNextLink() {
        List<Block> blocks = new ArrayList<>(chain.getChainSnapshot());
        Block b1 = blocks.get(1);
        // Attacker edits block 1 AND re-mines it so it looks valid on its own ...
        Block forged = new Block(1, b1.getPrevHash(), b1.getTimestamp(), "{\"type\":\"REGISTER\",\"batchNo\":\"FAKE\"}");
        forged.mine(DIFFICULTY);
        assertTrue(forged.verify());
        blocks.set(1, forged);

        // ... but block 2 still stores the OLD hash of block 1, so the link is broken.
        ValidationReport report = Blockchain.validate(blocks, DIFFICULTY);
        assertFalse(report.isValid());
        assertEquals(2, report.getBrokenAtIndex());
        assertTrue(report.getMessage().contains("Broken link"));
    }

    @Test
    @DisplayName("A broken prevHash link is detected")
    void brokenLinkDetected() {
        List<Block> blocks = new ArrayList<>(chain.getChainSnapshot());
        Block b3 = blocks.get(3);
        Block relinked = new Block(3, "f".repeat(64), b3.getTimestamp(), b3.getDataJson());
        relinked.mine(DIFFICULTY);
        blocks.set(3, relinked);

        ValidationReport report = Blockchain.validate(blocks, DIFFICULTY);
        assertFalse(report.isValid());
        assertEquals(3, report.getBrokenAtIndex());
    }

    @Test
    @DisplayName("A missing block (gap in indexes) is detected")
    void missingBlockDetected() {
        List<Block> blocks = new ArrayList<>(chain.getChainSnapshot());
        blocks.remove(2);
        assertFalse(Blockchain.validate(blocks, DIFFICULTY).isValid());
    }

    @Test
    @DisplayName("addBlock rejects a block that is not linked to the tip")
    void addBlockRejectsUnlinkedBlock() {
        Block stray = new Block(chain.size(), "a".repeat(64), System.currentTimeMillis(), "{}");
        stray.mine(DIFFICULTY);
        TamperDetectedException ex = assertThrows(TamperDetectedException.class, () -> chain.addBlock(stray));
        assertEquals(stray.getIndex(), ex.getBlockIndex());
    }

    @Test
    @DisplayName("addBlock rejects a block without proof-of-work")
    void addBlockRejectsUnminedBlock() {
        Block tip = chain.getLatestBlock();
        Block unmined = new Block(tip.getIndex() + 1, tip.getHash(), System.currentTimeMillis(), "{}");
        // Find a nonce whose hash does NOT start with "00" so the test is deterministic.
        while (unmined.meetsDifficulty(DIFFICULTY)) {
            unmined = new Block(tip.getIndex() + 1, tip.getHash(), System.currentTimeMillis() + 1, "{}");
        }
        Block candidate = unmined;
        assertThrows(TamperDetectedException.class, () -> chain.addBlock(candidate));
    }

    @Test
    @DisplayName("A chain rebuilt from stored blocks verifies and supports lookup by hash")
    void rebuildFromStoredBlocks() {
        Blockchain rebuilt = Blockchain.fromBlocks(chain.getChainSnapshot(), DIFFICULTY);
        assertTrue(rebuilt.verify());
        Block tip = chain.getLatestBlock();
        assertEquals(tip.getIndex(), rebuilt.findByHash(tip.getHash()).orElseThrow().getIndex());
    }

    @Test
    @DisplayName("validate() throws TamperDetectedException with the broken index")
    void validateThrows() {
        List<Block> blocks = new ArrayList<>(chain.getChainSnapshot());
        Block b1 = blocks.get(1);
        blocks.set(1, new Block(1, b1.getPrevHash(), b1.getTimestamp() + 1, b1.getDataJson(),
                b1.getNonce(), b1.getHash(), null));
        Blockchain tampered = Blockchain.fromBlocks(blocks, DIFFICULTY);
        TamperDetectedException ex = assertThrows(TamperDetectedException.class, tampered::validate);
        assertEquals(1, ex.getBlockIndex());
    }
}
