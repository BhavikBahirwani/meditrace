package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.ActivityItem;
import com.meditrace.model.Transaction;
import com.meditrace.model.TxType;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code transactions} table (supply-chain events).
 * Append-only: rows are never updated except to record which block sealed them.
 */
public class TransactionDAO extends AbstractDAO implements GenericDAO<Transaction, Long> {

    private static final String COLUMNS =
            "tx_id, batch_id, tx_type, from_user_id, to_user_id, quantity, tx_time, block_index, remarks";

    private static final String INSERT_SQL = "INSERT INTO transactions "
            + "(batch_id, tx_type, from_user_id, to_user_id, quantity, tx_time, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final RowMapper<Transaction> MAPPER = rs -> {
        Transaction t = new Transaction();
        t.setTxId(rs.getLong("tx_id"));
        t.setBatchId(rs.getLong("batch_id"));
        t.setTxType(TxType.valueOf(rs.getString("tx_type")));
        t.setFromUserId(rs.getLong("from_user_id"));
        t.setToUserId(getNullableLong(rs, "to_user_id"));
        t.setQuantity(rs.getInt("quantity"));
        t.setTxTime(rs.getObject("tx_time", LocalDateTime.class));
        t.setBlockIndex(getNullableInt(rs, "block_index"));
        t.setRemarks(rs.getString("remarks"));
        return t;
    };

    @Override
    public Optional<Transaction> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM transactions WHERE tx_id = ?", MAPPER, id);
    }

    @Override
    public List<Transaction> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM transactions ORDER BY tx_time, tx_id", MAPPER);
    }

    @Override
    public Long save(Transaction t) throws DataAccessException {
        try (Connection con = open()) {
            return insert(con, t);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save transaction", e);
        }
    }

    /** Insert on a caller-supplied connection (part of a JDBC transaction). */
    public long insert(Connection con, Transaction t) throws DataAccessException {
        long id = insertAndGetKey(con, INSERT_SQL, t.getBatchId(), t.getTxType(), t.getFromUserId(),
                t.getToUserId(), t.getQuantity(), t.getTxTime(), t.getRemarks());
        t.setTxId(id);
        return id;
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM transactions");
    }

    // ---------------------------------------------------------------- ledger support

    /** Records which block sealed the transaction. Only allowed once (block_index IS NULL guard). */
    public boolean linkToBlock(Connection con, long txId, int blockIndex) throws DataAccessException {
        return executeUpdate(con, "UPDATE transactions SET block_index = ? WHERE tx_id = ? AND block_index IS NULL",
                blockIndex, txId) == 1;
    }

    /** Transactions waiting for the Miner, oldest first. */
    public List<Long> findUnanchoredIds() throws DataAccessException {
        return queryList("SELECT tx_id FROM transactions WHERE block_index IS NULL ORDER BY tx_time, tx_id",
                rs -> rs.getLong(1));
    }

    /** Full custody history of a batch, in time order (the verify-page journey). */
    public List<Transaction> findByBatch(long batchId) throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM transactions WHERE batch_id = ? ORDER BY tx_time, tx_id",
                MAPPER, batchId);
    }

    public long countHandoversTo(long userId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM transactions WHERE tx_type = 'HANDOVER' AND to_user_id = ?", userId);
    }

    public long countHandoversFrom(long userId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM transactions WHERE tx_type = 'HANDOVER' AND from_user_id = ?", userId);
    }

    /**
     * Latest events involving a user, phrased from that user's point of view.
     * The lambda captures {@code userId} (a closure) to decide "Sent to" vs "Received from".
     */
    public List<ActivityItem> findRecentActivity(long userId, int limit) throws DataAccessException {
        String sql = "SELECT t.tx_type, t.from_user_id, t.quantity, t.tx_time, t.block_index, "
                + "b.batch_no, b.medicine_name, b.strength, "
                + "fu.company AS from_company, fu.city AS from_city, tu.company AS to_company, tu.city AS to_city "
                + "FROM transactions t "
                + "JOIN batches b ON b.batch_id = t.batch_id "
                + "JOIN users fu ON fu.user_id = t.from_user_id "
                + "LEFT JOIN users tu ON tu.user_id = t.to_user_id "
                + "WHERE t.from_user_id = ? OR t.to_user_id = ? "
                + "ORDER BY t.tx_time DESC, t.tx_id DESC LIMIT ?";
        return queryList(sql, rs -> {
            TxType type = TxType.valueOf(rs.getString("tx_type"));
            String headline;
            if (type == TxType.REGISTER) {
                headline = "Batch registered & sealed";
            } else if (rs.getLong("from_user_id") == userId) {
                headline = "Sent to " + rs.getString("to_company") + ", " + rs.getString("to_city");
            } else {
                headline = "Received from " + rs.getString("from_company") + ", " + rs.getString("from_city");
            }
            return new ActivityItem(type, headline, rs.getString("batch_no"),
                    rs.getString("medicine_name") + " " + rs.getString("strength"),
                    rs.getInt("quantity"), rs.getObject("tx_time", LocalDateTime.class),
                    getNullableInt(rs, "block_index"));
        }, userId, userId, limit);
    }
}
