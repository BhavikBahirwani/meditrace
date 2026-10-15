package com.meditrace.model;

import com.meditrace.blockchain.ValidationReport;

import java.util.List;

/**
 * Everything one role dashboard shows, assembled by {@code DashboardService}.
 */
public class DashboardView {

    private final List<StatCard> stats;
    private final List<ActivityItem> activity;
    private final List<StockLine> stock;
    private final List<Scan> recentScans;
    private final ValidationReport chainReport;
    private final int chainLength;
    private final int pendingBlocks;

    public DashboardView(List<StatCard> stats, List<ActivityItem> activity, List<StockLine> stock,
                         List<Scan> recentScans, ValidationReport chainReport, int chainLength, int pendingBlocks) {
        this.stats = stats;
        this.activity = activity;
        this.stock = stock;
        this.recentScans = recentScans;
        this.chainReport = chainReport;
        this.chainLength = chainLength;
        this.pendingBlocks = pendingBlocks;
    }

    public List<StatCard> getStats() {
        return stats;
    }

    public List<ActivityItem> getActivity() {
        return activity;
    }

    public List<StockLine> getStock() {
        return stock;
    }

    public List<Scan> getRecentScans() {
        return recentScans;
    }

    /** Latest periodic validation result, or null if the first check has not finished. */
    public ValidationReport getChainReport() {
        return chainReport;
    }

    public int getChainLength() {
        return chainLength;
    }

    /** Transactions still waiting in the Miner's queue. */
    public int getPendingBlocks() {
        return pendingBlocks;
    }
}
