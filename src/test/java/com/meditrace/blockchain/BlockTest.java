package com.meditrace.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for a single {@link Block}: hashing, proof-of-work and tamper detection.
 */
class BlockTest {

    private static final String DATA = "{\"type\":\"REGISTER\",\"batchNo\":\"B1023\"}";

    @Test
    @DisplayName("Hash is deterministic and 64 hex characters long")
    void hashIsDeterministic() {
        Block a = new Block(1, Blockchain.ZERO_HASH, 1_700_000_000_000L, DATA);
        Block b = new Block(1, Blockchain.ZERO_HASH, 1_700_000_000_000L, DATA);
        assertEquals(a.getHash(), b.getHash());
        assertEquals(64, a.getHash().length());
        assertTrue(a.getHash().matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("Changing any input changes the hash")
    void differentInputGivesDifferentHash() {
        Block a = new Block(1, Blockchain.ZERO_HASH, 1_700_000_000_000L, DATA);
        Block b = new Block(1, Blockchain.ZERO_HASH, 1_700_000_000_000L, DATA.replace("B1023", "B1024"));
        assertNotEquals(a.getHash(), b.getHash());
    }

    @Test
    @DisplayName("Mining finds a hash with the required leading zeros")
    void miningMeetsDifficulty() {
        Block block = new Block(1, Blockchain.ZERO_HASH, System.currentTimeMillis(), DATA);
        block.mine(2);
        assertTrue(block.getHash().startsWith("00"));
        assertTrue(block.meetsDifficulty(2));
        assertTrue(block.verify(), "a freshly mined block must verify");
    }

    @Test
    @DisplayName("A block rebuilt from DB with edited data fails verification")
    void tamperedDataIsDetected() {
        Block original = new Block(3, Blockchain.ZERO_HASH, System.currentTimeMillis(), DATA);
        original.mine(2);

        // Simulate someone editing data_json in MySQL but leaving the stored hash unchanged.
        Block fromDb = new Block(original.getIndex(), original.getPrevHash(), original.getTimestamp(),
                DATA.replace("B1023", "B9999"), original.getNonce(), original.getHash(), null);

        assertFalse(fromDb.verify());
    }

    @Test
    @DisplayName("A block rebuilt from DB with identical values verifies")
    void untouchedStoredBlockVerifies() {
        Block original = new Block(3, Blockchain.ZERO_HASH, System.currentTimeMillis(), DATA);
        original.mine(2);
        Block fromDb = new Block(original.getIndex(), original.getPrevHash(), original.getTimestamp(),
                original.getDataJson(), original.getNonce(), original.getHash(), null);
        assertTrue(fromDb.verify());
    }
}
