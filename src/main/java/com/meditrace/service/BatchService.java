package com.meditrace.service;

import com.meditrace.blockchain.Miner;
import com.meditrace.blockchain.MiningTask;
import com.meditrace.dao.BatchDAO;
import com.meditrace.dao.PackDAO;
import com.meditrace.dao.TransactionDAO;
import com.meditrace.exception.DataAccessException;
import com.meditrace.exception.MediTraceException;
import com.meditrace.exception.UnauthorizedException;
import com.meditrace.model.Batch;
import com.meditrace.model.Pack;
import com.meditrace.model.Transaction;
import com.meditrace.model.TxType;
import com.meditrace.model.User;
import com.meditrace.util.AppConstants;
import com.meditrace.util.DBConnection;
import com.meditrace.util.DateUtil;
import com.meditrace.util.PackCodeGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Batch registration - the main example of a <b>JDBC transaction</b> in MediTrace.
 */
public class BatchService {

    private static final Logger LOG = Logger.getLogger(BatchService.class.getName());

    private final BatchDAO batchDAO = new BatchDAO();
    private final PackDAO packDAO = new PackDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final Miner miner;

    public BatchService(Miner miner) {
        this.miner = miner;
    }

    /**
     * Registers a batch, generates one unique code per pack and records a REGISTER
     * transaction - all inside ONE database transaction:
     * <pre>
     *   setAutoCommit(false)
     *   INSERT batches            (1 row)
     *   INSERT packs              (N rows, JDBC batch)
     *   INSERT transactions       (1 row)
     *   commit()   -- or rollback() if anything fails, so no half-registered batch can exist
     * </pre>
     * After the commit, the transaction is queued for the Miner, which seals it into a block
     * in the background.
     *
     * @throws UnauthorizedException if the user is not a manufacturer
     */
    public Batch registerBatch(User user, Batch batch) throws MediTraceException {
        if (!user.canRegisterBatch()) {               // polymorphic permission check
            throw new UnauthorizedException("Only manufacturers can register batches.");
        }
        validate(batch);
        if (batchDAO.existsByBatchNo(batch.getBatchNo())) {
            throw new MediTraceException("Batch number " + batch.getBatchNo() + " already exists.");
        }
        batch.setManufacturerId(user.getUserId());

        List<Pack> packs = new ArrayList<>();
        for (String code : PackCodeGenerator.generateUnique(batch.getBatchNo(), batch.getPackCount())) {
            packs.add(new Pack(code, 0, user.getUserId()));
        }

        Transaction register = new Transaction();
        register.setTxType(TxType.REGISTER);
        register.setFromUserId(user.getUserId());
        register.setQuantity(batch.getPackCount());
        // MySQL DATETIME keeps whole seconds; truncate so Java and DB agree on the value.
        register.setTxTime(LocalDateTime.now(DateUtil.IST).truncatedTo(ChronoUnit.SECONDS));
        register.setRemarks("Batch registered at " + user.getCity() + " plant");

        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);                 // start the transaction
            try {
                long batchId = batchDAO.insert(con, batch);
                for (Pack p : packs) {
                    p.setBatchId(batchId);
                }
                packDAO.insertAll(con, packs);
                register.setBatchId(batchId);
                transactionDAO.insert(con, register);
                con.commit();                         // all three inserts become permanent together
            } catch (DataAccessException | RuntimeException e) {
                con.rollback();                       // undo everything done on this connection
                LOG.warning("Batch " + batch.getBatchNo() + " rolled back: " + e.getMessage());
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not register batch " + batch.getBatchNo(), e);
        }

        if (miner != null) {
            miner.submit(MiningTask.of(register.getTxId()));   // producer side of the queue
        }
        batch.setPackCodes(packs.stream().map(Pack::getPackCode).toList());
        LOG.info("Registered " + batch + " (tx " + register.getTxId() + ")");
        return batch;
    }

    /** Business rules for a new batch. */
    private void validate(Batch b) throws MediTraceException {
        if (b.getBatchNo() == null || !b.getBatchNo().matches("^[A-Z0-9]{2,20}$")) {
            throw new MediTraceException("Batch number must be 2-20 capital letters or digits, e.g. B1030.");
        }
        if (isBlank(b.getMedicineName()) || isBlank(b.getStrength()) || isBlank(b.getDosageForm())) {
            throw new MediTraceException("Medicine name, strength and dosage form are required.");
        }
        if (b.getMfgDate() == null || b.getExpiryDate() == null || !b.getExpiryDate().isAfter(b.getMfgDate())) {
            throw new MediTraceException("Expiry date must be after the manufacturing date.");
        }
        if (b.getMfgDate().isAfter(java.time.LocalDate.now(DateUtil.IST))) {
            throw new MediTraceException("Manufacturing date cannot be in the future.");
        }
        BigDecimal mrp = b.getMrp();
        if (mrp == null || mrp.signum() <= 0 || mrp.scale() > 2 || mrp.compareTo(new BigDecimal("100000")) >= 0) {
            throw new MediTraceException("MRP must be a positive amount in rupees with at most 2 decimals.");
        }
        if (b.getPackCount() < 1 || b.getPackCount() > AppConstants.MAX_PACKS_PER_BATCH) {
            throw new MediTraceException("Number of packs must be between 1 and " + AppConstants.MAX_PACKS_PER_BATCH + ".");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Batch plus its pack codes, for the batch detail page. Only the owning manufacturer may see it. */
    public Optional<Batch> findOwnBatch(User user, String batchNo) throws DataAccessException {
        Optional<Batch> batch = batchDAO.findByBatchNo(batchNo);
        if (batch.isEmpty() || batch.get().getManufacturerId() != user.getUserId()) {
            return Optional.empty();
        }
        batch.get().setPackCodes(packDAO.findCodesByBatch(batch.get().getBatchId()));
        return batch;
    }
}
