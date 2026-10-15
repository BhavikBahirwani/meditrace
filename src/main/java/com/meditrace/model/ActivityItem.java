package com.meditrace.model;

import com.meditrace.util.DateUtil;

import java.time.LocalDateTime;

/**
 * Read-only view of one supply-chain transaction as seen by a particular user
 * ("Received from ABC Distributors, Delhi"). Built by {@code TransactionDAO} from a JOIN
 * query and shown in the dashboard's "Recent activity" list.
 */
public class ActivityItem {

    private final TxType type;
    private final String headline;
    private final String batchNo;
    private final String productName;
    private final int quantity;
    private final LocalDateTime time;
    private final Integer blockIndex;

    public ActivityItem(TxType type, String headline, String batchNo, String productName,
                        int quantity, LocalDateTime time, Integer blockIndex) {
        this.type = type;
        this.headline = headline;
        this.batchNo = batchNo;
        this.productName = productName;
        this.quantity = quantity;
        this.time = time;
        this.blockIndex = blockIndex;
    }

    public TxType getType() {
        return type;
    }

    public String getHeadline() {
        return headline;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getTimeText() {
        return DateUtil.formatDateTime(time);
    }

    /** Block number, or null while the Miner has not sealed it yet. */
    public Integer getBlockIndex() {
        return blockIndex;
    }

    public boolean isSealed() {
        return blockIndex != null;
    }
}
