package com.meditrace.blockchain;

import com.meditrace.exception.MediTraceException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background thread that turns supply-chain transactions into blocks.
 * <p>
 * <b>Producer-consumer pattern:</b> servlet threads (producers) call {@link #submit(MiningTask)}
 * which only puts a task on a {@link BlockingQueue} and returns immediately, so the web
 * request is never slowed down by proof-of-work. This single Miner thread (consumer)
 * {@code take()}s tasks one at a time, mines a block, saves it, then appends it to the
 * shared {@link Blockchain} (whose {@code addBlock} is {@code synchronized}).
 * <p>
 * Having exactly one consumer guarantees blocks are created strictly one after another,
 * so two blocks can never claim the same index.
 */
public class Miner implements Runnable {

    private static final Logger LOG = Logger.getLogger(Miner.class.getName());

    private final BlockingQueue<MiningTask> queue = new LinkedBlockingQueue<>();
    private final Blockchain blockchain;
    private final LedgerGateway gateway;
    private final AtomicLong blocksMined = new AtomicLong();

    private volatile boolean running;
    private Thread worker;

    public Miner(Blockchain blockchain, LedgerGateway gateway) {
        this.blockchain = blockchain;
        this.gateway = gateway;
    }

    /** Starts the background mining thread (daemon, so it never blocks JVM shutdown). */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(this, "meditrace-miner");
        worker.setDaemon(true);
        worker.start();
        LOG.info("Miner thread started");
    }

    /** Stops the thread: interrupting it wakes it up from {@code queue.take()}. */
    public synchronized void shutdown() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Miner thread stopped; " + queue.size() + " task(s) left in queue");
    }

    /**
     * Producer side: queue a transaction for sealing. Non-blocking for the caller.
     *
     * @return true if the task was accepted
     */
    public boolean submit(MiningTask task) {
        return queue.offer(task);
    }

    /** Consumer loop executed by the worker thread. */
    @Override
    public void run() {
        while (running) {
            try {
                MiningTask task = queue.take();     // blocks until a task arrives
                mineOne(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore flag and exit the loop
                break;
            } catch (RuntimeException e) {
                // Never let one bad task kill the miner thread.
                LOG.log(Level.SEVERE, "Unexpected error in miner", e);
            }
        }
    }

    /**
     * Mines and stores a single block. Package-visible so tests can call it synchronously.
     */
    void mineOne(MiningTask task) {
        try {
            String payload = gateway.buildPayload(task);
            Block block = blockchain.prepareNextBlock(payload);   // proof-of-work happens here
            gateway.persist(block, task);                          // DB first ...
            blockchain.addBlock(block);                            // ... then memory
            blocksMined.incrementAndGet();
            LOG.info(() -> "Mined " + block + " for transaction " + task.txId());
        } catch (MediTraceException e) {
            LOG.log(Level.SEVERE, "Could not mine block for transaction " + task.txId(), e);
        }
    }

    public int getPendingCount() {
        return queue.size();
    }

    public long getBlocksMined() {
        return blocksMined.get();
    }

    public boolean isRunning() {
        return running;
    }
}
