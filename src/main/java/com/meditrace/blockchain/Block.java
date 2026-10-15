package com.meditrace.blockchain;

import com.meditrace.util.HashUtil;

import java.util.Objects;

/**
 * One block of the MediTrace ledger.
 * <p>
 * The block hash is {@code SHA-256(index + prevHash + timestamp + dataJson + nonce)}.
 * Because each block stores the hash of the previous one ({@code prevHash}), changing any
 * old block changes its hash and breaks every link after it - that is what makes the
 * ledger tamper-evident.
 * <p>
 * A light proof-of-work is applied: the nonce is incremented until the hash starts with
 * {@code difficulty} zeros (2 by default), which keeps mining fast on a laptop while still
 * demonstrating the concept.
 */
public class Block implements Hashable, Verifiable {

    private final int index;
    private final String prevHash;
    private final long timestamp;      // epoch milliseconds
    private final String dataJson;     // payload: what this block records
    private long nonce;
    private String hash;
    private String signature;          // reserved for digital signatures (roadmap)

    /**
     * Creates a new, not-yet-mined block. Call {@link #mine(int)} before adding it to a chain.
     */
    public Block(int index, String prevHash, long timestamp, String dataJson) {
        this.index = index;
        this.prevHash = Objects.requireNonNull(prevHash, "prevHash");
        this.timestamp = timestamp;
        this.dataJson = Objects.requireNonNull(dataJson, "dataJson");
        this.nonce = 0;
        this.hash = computeHash();
    }

    /**
     * Rebuilds a block exactly as it was stored in the database (no re-hashing), so that
     * {@link #verify()} can tell whether the stored values were modified.
     */
    public Block(int index, String prevHash, long timestamp, String dataJson,
                 long nonce, String hash, String signature) {
        this.index = index;
        this.prevHash = prevHash;
        this.timestamp = timestamp;
        this.dataJson = dataJson;
        this.nonce = nonce;
        this.hash = hash;
        this.signature = signature;
    }

    /** {@inheritDoc} SHA-256 over index, previous hash, timestamp, data and nonce. */
    @Override
    public String computeHash() {
        return HashUtil.sha256Hex(index + prevHash + timestamp + dataJson + nonce);
    }

    /**
     * Proof-of-work: increments the nonce until the hash has {@code difficulty} leading zeros.
     *
     * @param difficulty number of leading '0' hex characters required
     */
    public void mine(int difficulty) {
        String target = "0".repeat(Math.max(0, difficulty));
        hash = computeHash();
        while (!hash.startsWith(target)) {
            nonce++;
            hash = computeHash();
        }
    }

    /** @return true if the hash meets the proof-of-work target for this difficulty */
    public boolean meetsDifficulty(int difficulty) {
        return hash != null && hash.startsWith("0".repeat(Math.max(0, difficulty)));
    }

    /** {@inheritDoc} A block is intact when its stored hash equals a fresh recomputation. */
    @Override
    public boolean verify() {
        return hash != null && hash.equals(computeHash());
    }

    // ---------------------------------------------------------------- getters

    public int getIndex() {
        return index;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDataJson() {
        return dataJson;
    }

    public long getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /** First 12 characters of the hash, handy for compact display in the UI. */
    public String getShortHash() {
        return hash == null ? "" : hash.substring(0, Math.min(12, hash.length()));
    }

    @Override
    public String toString() {
        return "Block#" + index + "[hash=" + getShortHash() + "…, nonce=" + nonce + "]";
    }
}
