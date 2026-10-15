package com.meditrace.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the producer-consumer {@link Miner} thread with an in-memory {@link LedgerGateway},
 * so no database is needed.
 */
class MinerTest {

    /** Fake gateway that records persisted blocks and counts down a latch. */
    private static class InMemoryGateway implements LedgerGateway {
        final List<Block> saved = new CopyOnWriteArrayList<>();
        final CountDownLatch latch;

        InMemoryGateway(int expected) {
            latch = new CountDownLatch(expected);
        }

        @Override
        public String buildPayload(MiningTask task) {
            return "{\"txId\":" + task.txId() + "}";
        }

        @Override
        public void persist(Block block, MiningTask task) {
            saved.add(block);
            latch.countDown();
        }

        @Override
        public List<Block> loadAllBlocks() {
            return List.copyOf(saved);
        }
    }

    @Test
    @DisplayName("Tasks submitted from many threads become consecutive, valid blocks")
    void minesQueuedTasksInOrder() throws Exception {
        int producers = 4;
        int tasksEach = 5;
        Blockchain chain = new Blockchain(Blockchain.DEFAULT_DIFFICULTY);
        InMemoryGateway gateway = new InMemoryGateway(producers * tasksEach);
        Miner miner = new Miner(chain, gateway);
        miner.start();

        // Several producer threads submit at the same time, like concurrent web requests.
        Thread[] threads = new Thread[producers];
        for (int p = 0; p < producers; p++) {
            final int base = p * 100;
            threads[p] = new Thread(() -> {
                for (int i = 0; i < tasksEach; i++) {
                    miner.submit(MiningTask.of(base + i));
                }
            });
            threads[p].start();
        }
        for (Thread t : threads) {
            t.join();
        }

        assertTrue(gateway.latch.await(30, TimeUnit.SECONDS), "miner did not finish in time");
        miner.shutdown();

        assertEquals(1 + producers * tasksEach, chain.size());
        assertEquals(producers * tasksEach, miner.getBlocksMined());
        assertTrue(chain.verify(), "chain built by the miner thread must be valid");
    }

    @Test
    @DisplayName("Validator reports a valid chain when DB and memory agree")
    void validatorAgreesWithMemory() throws Exception {
        Blockchain chain = new Blockchain(Blockchain.DEFAULT_DIFFICULTY);
        InMemoryGateway gateway = new InMemoryGateway(0) {
            @Override
            public List<Block> loadAllBlocks() {
                return chain.getChainSnapshot();
            }
        };
        Miner miner = new Miner(chain, gateway);
        miner.mineOne(MiningTask.of(1));
        miner.mineOne(MiningTask.of(2));

        ChainValidator validator = new ChainValidator(chain, gateway, 120);
        ValidationReport report = validator.validateNow();
        assertTrue(report.isValid(), report.getMessage());
        assertEquals(3, report.getBlocksChecked());
    }

    @Test
    @DisplayName("Validator flags a blocks table that was emptied while the app was running")
    void validatorDetectsDeletedBlocks() throws Exception {
        Blockchain chain = new Blockchain(Blockchain.DEFAULT_DIFFICULTY);
        Miner miner = new Miner(chain, new InMemoryGateway(0));
        miner.mineOne(MiningTask.of(1));

        // The database now returns no blocks at all (e.g. schema.sql was re-run).
        ChainValidator validator = new ChainValidator(chain, new InMemoryGateway(0), 120);
        ValidationReport report = validator.validateNow();
        assertFalse(report.isValid());
        assertTrue(report.getMessage().contains("running ledger has 2"), report.getMessage());
    }
}
