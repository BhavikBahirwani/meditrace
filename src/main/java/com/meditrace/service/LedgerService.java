package com.meditrace.service;

import com.meditrace.blockchain.Block;
import com.meditrace.blockchain.Blockchain;
import com.meditrace.blockchain.LedgerGateway;
import com.meditrace.blockchain.MiningTask;
import com.meditrace.blockchain.ValidationReport;
import com.meditrace.dao.BatchDAO;
import com.meditrace.dao.BlockDAO;
import com.meditrace.dao.PackDAO;
import com.meditrace.dao.TransactionDAO;
import com.meditrace.dao.UserDAO;
import com.meditrace.exception.DataAccessException;
import com.meditrace.exception.MediTraceException;
import com.meditrace.model.Batch;
import com.meditrace.model.Transaction;
import com.meditrace.model.TxType;
import com.meditrace.model.User;
import com.meditrace.util.DBConnection;
import com.meditrace.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Database side of the blockchain: implements {@link LedgerGateway} for the Miner and
 * ChainValidator, and loads / bootstraps the chain at start-up.
 */
public class LedgerService implements LedgerGateway {

    private static final Logger LOG = Logger.getLogger(LedgerService.class.getName());

    private final BlockDAO blockDAO = new BlockDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final PackDAO packDAO = new PackDAO();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Loads the chain from the {@code blocks} table, or - on first run - creates, mines and
     * saves the genesis block.
     */
    public Blockchain loadOrCreateChain(int difficulty) throws DataAccessException {
        List<Block> stored = blockDAO.findAll();
        if (stored.isEmpty()) {
            Block genesis = Blockchain.createGenesisBlock(difficulty);
            blockDAO.save(genesis);
            LOG.info("Blocks table was empty - genesis block created: " + genesis);
            return Blockchain.fromBlocks(List.of(genesis), difficulty);
        }
        Blockchain chain = Blockchain.fromBlocks(stored, difficulty);
        ValidationReport report = Blockchain.validate(stored, difficulty);
        if (report.isValid()) {
            LOG.info("Loaded blockchain with " + stored.size() + " blocks - " + report.getMessage());
        } else {
            LOG.warning("Loaded blockchain is NOT valid: " + report.getMessage());
        }
        return chain;
    }

    /** IDs of transactions that still need a block (e.g. rows inserted by seed.sql). */
    public List<Long> findUnanchoredTransactionIds() throws DataAccessException {
        return transactionDAO.findUnanchoredIds();
    }

    /**
     * Builds the block payload. Everything is re-read from the database first, so the
     * fingerprints are computed from exactly what MySQL stored (e.g. DATETIME rounded to
     * seconds), and later re-computations will match.
     */
    @Override
    public String buildPayload(MiningTask task) throws MediTraceException {
        Transaction tx = transactionDAO.findById(task.txId())
                .orElseThrow(() -> new DataAccessException("Transaction " + task.txId() + " not found"));
        Batch batch = batchDAO.findById(tx.getBatchId())
                .orElseThrow(() -> new DataAccessException("Batch " + tx.getBatchId() + " not found"));
        User from = userDAO.findById(tx.getFromUserId())
                .orElseThrow(() -> new DataAccessException("User " + tx.getFromUserId() + " not found"));
        User to = (tx.getToUserId() == null) ? null : userDAO.findById(tx.getToUserId()).orElse(null);

        JsonUtil.Builder json = JsonUtil.object()
                .put("type", tx.getTxType().name())
                .put("txId", tx.getTxId())
                .put("txHash", tx.computeHash())
                .put("batchNo", batch.getBatchNo())
                .put("product", batch.getProductName())
                .put("from", from.getDisplayName())
                .put("to", to == null ? null : to.getDisplayName())
                .put("quantity", tx.getQuantity())
                .put("time", String.valueOf(tx.getTxTime()));

        if (tx.getTxType() == TxType.REGISTER) {
            // Seal the batch details + every pack code: later edits to batches/packs are detectable.
            batch.setPackCodes(packDAO.findCodesByBatch(batch.getBatchId()));
            json.put("batchHash", batch.computeHash());
        }
        return json.toJson();
    }

    /**
     * Saves the block and links the transaction to it in ONE JDBC transaction:
     * either both rows change, or (on any error) neither does.
     */
    @Override
    public void persist(Block block, MiningTask task) throws MediTraceException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                blockDAO.insert(con, block);
                if (!transactionDAO.linkToBlock(con, task.txId(), block.getIndex())) {
                    throw new DataAccessException("Transaction " + task.txId() + " is already sealed");
                }
                con.commit();
            } catch (DataAccessException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not persist block " + block.getIndex(), e);
        }
    }

    @Override
    public List<Block> loadAllBlocks() throws MediTraceException {
        return blockDAO.findAll();
    }
}
