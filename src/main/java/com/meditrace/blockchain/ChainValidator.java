package com.meditrace.blockchain;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodic integrity audit of the ledger, run on a {@link ScheduledExecutorService}
 * (every 2 minutes by default).
 * <p>
 * Each run re-reads every block from MySQL and checks it two ways:
 * <ol>
 *   <li>{@link Blockchain#validate(List, int)} - hashes, links and proof-of-work;</li>
 *   <li>a comparison with the in-memory chain - catches someone who edits a block in the
 *       database AND re-mines it so that it looks valid on its own.</li>
 * </ol>
 * The latest {@link ValidationReport} is kept in a {@code volatile} field so servlet threads
 * always see the newest result without locking.
 */
public class ChainValidator implements Runnable {

    private static final Logger LOG = Logger.getLogger(ChainValidator.class.getName());

    private final Blockchain blockchain;
    private final LedgerGateway gateway;
    private final long intervalSeconds;

    private volatile ValidationReport lastReport;
    private ScheduledExecutorService scheduler;

    public ChainValidator(Blockchain blockchain, LedgerGateway gateway, long intervalSeconds) {
        this.blockchain = blockchain;
        this.gateway = gateway;
        this.intervalSeconds = intervalSeconds;
    }

    /** Schedules {@link #run()} immediately and then every {@code intervalSeconds}. */
    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "meditrace-chain-validator");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this, 0, intervalSeconds, TimeUnit.SECONDS);
        LOG.info("Chain validator scheduled every " + intervalSeconds + "s");
    }

    /** Stops the scheduler and waits briefly for a running check to finish. */
    public synchronized void shutdown() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler = null;
        LOG.info("Chain validator stopped");
    }

    /**
     * One validation pass. Any exception is caught: an uncaught exception would make
     * {@code scheduleAtFixedRate} silently cancel all future runs.
     */
    @Override
    public void run() {
        try {
            lastReport = validateNow();
            if (lastReport.isValid()) {
                LOG.fine(lastReport::toString);
            } else {
                LOG.warning("TAMPER ALERT - " + lastReport.getMessage());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Chain validation could not run", e);
            lastReport = ValidationReport.broken(0, -1, "Validation could not run: " + e.getMessage());
        }
    }

    /** Performs a full check right now and returns the report. */
    public ValidationReport validateNow() throws Exception {
        List<Block> stored = gateway.loadAllBlocks();
        ValidationReport report = Blockchain.validate(stored, blockchain.getDifficulty());
        if (!report.isValid()) {
            return report;
        }
        // Cross-check: the database copy must match the trusted in-memory copy block by block.
        List<Block> memory = blockchain.getChainSnapshot();
        int common = Math.min(stored.size(), memory.size());
        for (int i = 0; i < common; i++) {
            if (!stored.get(i).getHash().equals(memory.get(i).getHash())) {
                return ValidationReport.broken(i, i, "Block " + i
                        + " in the database differs from the trusted in-memory ledger.");
            }
        }
        return report;
    }

    /** @return the latest report, or {@code null} if no check has finished yet */
    public ValidationReport getLastReport() {
        return lastReport;
    }
}
