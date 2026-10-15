package com.meditrace.model;

import com.meditrace.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of a dashboard stock table: a batch plus how many of its packs are still in the
 * chain / sold, and whether it is under an active recall. Built by an aggregate
 * {@code GROUP BY} query in {@code BatchDAO}.
 */
public class StockLine {

    private final String batchNo;
    private final String productName;
    private final LocalDate expiryDate;
    private final BigDecimal mrp;
    private final long inChain;
    private final long sold;
    private final boolean recalled;

    public StockLine(String batchNo, String productName, LocalDate expiryDate, BigDecimal mrp,
                     long inChain, long sold, boolean recalled) {
        this.batchNo = batchNo;
        this.productName = productName;
        this.expiryDate = expiryDate;
        this.mrp = mrp;
        this.inChain = inChain;
        this.sold = sold;
        this.recalled = recalled;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public String getProductName() {
        return productName;
    }

    public String getExpiryText() {
        return DateUtil.formatMonthYear(expiryDate);
    }

    public String getMrpText() {
        return mrp == null ? "" : "₹" + mrp.setScale(2).toPlainString();
    }

    public long getInChain() {
        return inChain;
    }

    public long getSold() {
        return sold;
    }

    public long getTotal() {
        return inChain + sold;
    }

    public boolean isRecalled() {
        return recalled;
    }
}
