package com.meditrace.service;

import com.meditrace.blockchain.Blockchain;
import com.meditrace.blockchain.ChainValidator;
import com.meditrace.blockchain.Miner;
import com.meditrace.dao.BatchDAO;
import com.meditrace.dao.PackDAO;
import com.meditrace.dao.RecallDAO;
import com.meditrace.dao.ReportDAO;
import com.meditrace.dao.ScanDAO;
import com.meditrace.dao.TransactionDAO;
import com.meditrace.exception.DataAccessException;
import com.meditrace.model.ActivityItem;
import com.meditrace.model.DashboardView;
import com.meditrace.model.PackStatus;
import com.meditrace.model.Scan;
import com.meditrace.model.StatCard;
import com.meditrace.model.StockLine;
import com.meditrace.model.User;

import java.util.List;

/**
 * Collects the numbers and lists for each role's dashboard from the database.
 */
public class DashboardService {

    private static final int RECENT_LIMIT = 8;

    private final BatchDAO batchDAO = new BatchDAO();
    private final PackDAO packDAO = new PackDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final ScanDAO scanDAO = new ScanDAO();
    private final RecallDAO recallDAO = new RecallDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    private final Blockchain blockchain;
    private final Miner miner;
    private final ChainValidator validator;

    public DashboardService(Blockchain blockchain, Miner miner, ChainValidator validator) {
        this.blockchain = blockchain;
        this.miner = miner;
        this.validator = validator;
    }

    public DashboardView build(User user) throws DataAccessException {
        long id = user.getUserId();
        List<StatCard> stats;
        List<StockLine> stock;
        List<Scan> scans;

        // Switch on the role enum: each participant sees the numbers that matter to them.
        switch (user.getRole()) {
            case MANUFACTURER -> {
                long suspicious = scanDAO.countSuspiciousForManufacturer(id);
                stats = List.of(
                        new StatCard("Batches registered", batchDAO.countByManufacturer(id), "sealed on the chain"),
                        new StatCard("Packs produced", packDAO.countByManufacturer(id), "unique codes issued"),
                        new StatCard("Packs sold", packDAO.countByManufacturerAndStatus(id, PackStatus.SOLD),
                                "reached a customer"),
                        new StatCard("Suspicious scans", suspicious, "tampered or copied codes",
                                suspicious > 0 ? "alert" : ""),
                        new StatCard("Open reports", reportDAO.countOpenForManufacturer(id), "from customers"),
                        new StatCard("Active recalls", recallDAO.countActiveForManufacturer(id), "batches withdrawn"));
                stock = batchDAO.findSummaryByManufacturer(id, RECENT_LIMIT);
                scans = scanDAO.findRecentForManufacturer(id, RECENT_LIMIT);
            }
            case DISTRIBUTOR -> {
                long recalled = recallDAO.countActiveForHolder(id);
                stats = List.of(
                        new StatCard("Packs in stock", packDAO.countByHolderAndStatus(id, PackStatus.IN_CHAIN),
                                "in your warehouse"),
                        new StatCard("Consignments received", transactionDAO.countHandoversTo(id), "from manufacturers"),
                        new StatCard("Consignments dispatched", transactionDAO.countHandoversFrom(id), "to pharmacies"),
                        new StatCard("Recalled batches held", recalled, "must be returned",
                                recalled > 0 ? "alert" : ""));
                stock = batchDAO.findStockByHolder(id, RECENT_LIMIT);
                scans = List.of();
            }
            case PHARMACY -> {
                long recalled = recallDAO.countActiveForHolder(id);
                stats = List.of(
                        new StatCard("Packs in stock", packDAO.countByHolderAndStatus(id, PackStatus.IN_CHAIN),
                                "on your shelves"),
                        new StatCard("Packs sold", packDAO.countByHolderAndStatus(id, PackStatus.SOLD), "to customers"),
                        new StatCard("Customer scans", scanDAO.countForHolder(id), "of packs from your store"),
                        new StatCard("Recalled batches held", recalled, "remove from shelf",
                                recalled > 0 ? "alert" : ""));
                stock = batchDAO.findStockByHolder(id, RECENT_LIMIT);
                scans = scanDAO.findRecentForHolder(id, RECENT_LIMIT);
            }
            default -> throw new IllegalStateException("Unknown role " + user.getRole());
        }

        List<ActivityItem> activity = transactionDAO.findRecentActivity(id, RECENT_LIMIT);
        return new DashboardView(stats, activity, stock, scans,
                validator == null ? null : validator.getLastReport(),
                blockchain == null ? 0 : blockchain.size(),
                miner == null ? 0 : miner.getPendingCount());
    }
}
