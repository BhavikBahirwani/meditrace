package com.meditrace.model;

import com.meditrace.blockchain.Hashable;
import com.meditrace.util.DateUtil;
import com.meditrace.util.HashUtil;

import java.time.LocalDateTime;

/**
 * A supply-chain event (one row of {@code transactions}): a batch being registered, or
 * custody of a batch moving from one participant to another.
 * <p>
 * Every transaction is sealed into exactly one block; {@link #getBlockIndex()} is
 * {@code null} until the Miner thread has done so. Implements {@link Hashable} so the
 * row can later be checked against the fingerprint stored in its block.
 */
public class Transaction implements Hashable {

    private long txId;
    private long batchId;
    private TxType txType;
    private long fromUserId;
    private Long toUserId;            // null for REGISTER
    private int quantity;
    private LocalDateTime txTime;     // stored as DATETIME (whole seconds)
    private Integer blockIndex;       // null until mined
    private String remarks;

    /** {@inheritDoc} Fingerprint of the fields that must never change after sealing. */
    @Override
    public String computeHash() {
        return HashUtil.sha256Hex(String.join("|",
                String.valueOf(txId), String.valueOf(batchId), String.valueOf(txType),
                String.valueOf(fromUserId), String.valueOf(toUserId),
                String.valueOf(quantity), String.valueOf(txTime)));
    }

    public boolean isAnchored() {
        return blockIndex != null;
    }

    public String getTxTimeText() {
        return DateUtil.formatDateTime(txTime);
    }

    // ---------------------------------------------------------------- getters / setters

    public long getTxId() {
        return txId;
    }

    public void setTxId(long txId) {
        this.txId = txId;
    }

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public TxType getTxType() {
        return txType;
    }

    public void setTxType(TxType txType) {
        this.txType = txType;
    }

    public long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTxTime() {
        return txTime;
    }

    public void setTxTime(LocalDateTime txTime) {
        this.txTime = txTime;
    }

    public Integer getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(Integer blockIndex) {
        this.blockIndex = blockIndex;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return "Transaction{" + txId + ", " + txType + ", batch=" + batchId + ", block=" + blockIndex + "}";
    }
}
