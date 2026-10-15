package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Batch;
import com.meditrace.model.StockLine;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code batches} table, plus the stock summaries for the dashboards.
 */
public class BatchDAO extends AbstractDAO implements GenericDAO<Batch, Long> {

    private static final String COLUMNS = "batch_id, batch_no, medicine_name, strength, dosage_form, "
            + "manufacturer_id, mfg_date, expiry_date, mrp, pack_count, created_at";

    private static final String INSERT_SQL = "INSERT INTO batches (batch_no, medicine_name, strength, dosage_form, "
            + "manufacturer_id, mfg_date, expiry_date, mrp, pack_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final RowMapper<Batch> MAPPER = rs -> {
        Batch b = new Batch();
        b.setBatchId(rs.getLong("batch_id"));
        b.setBatchNo(rs.getString("batch_no"));
        b.setMedicineName(rs.getString("medicine_name"));
        b.setStrength(rs.getString("strength"));
        b.setDosageForm(rs.getString("dosage_form"));
        b.setManufacturerId(rs.getLong("manufacturer_id"));
        b.setMfgDate(rs.getObject("mfg_date", LocalDate.class));
        b.setExpiryDate(rs.getObject("expiry_date", LocalDate.class));
        b.setMrp(rs.getBigDecimal("mrp"));
        b.setPackCount(rs.getInt("pack_count"));
        b.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return b;
    };

    /** Aggregates packs per batch: in-chain vs sold, and whether an active recall exists. */
    private static final RowMapper<StockLine> STOCK_MAPPER = rs -> new StockLine(
            rs.getString("batch_no"),
            rs.getString("medicine_name") + " " + rs.getString("strength"),
            rs.getObject("expiry_date", LocalDate.class),
            rs.getBigDecimal("mrp"),
            rs.getLong("in_chain"),
            rs.getLong("sold"),
            rs.getBoolean("recalled"));

    private static final String STOCK_SELECT = "SELECT b.batch_no, b.medicine_name, b.strength, b.expiry_date, b.mrp, "
            + "SUM(p.status = 'IN_CHAIN') AS in_chain, SUM(p.status = 'SOLD') AS sold, "
            + "EXISTS (SELECT 1 FROM recalls r WHERE r.batch_id = b.batch_id AND r.active) AS recalled "
            + "FROM packs p JOIN batches b ON b.batch_id = p.batch_id ";

    @Override
    public Optional<Batch> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM batches WHERE batch_id = ?", MAPPER, id);
    }

    @Override
    public List<Batch> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM batches ORDER BY created_at DESC", MAPPER);
    }

    /** Stand-alone insert (auto-commit). Batch registration normally uses {@link #insert(Connection, Batch)}. */
    @Override
    public Long save(Batch b) throws DataAccessException {
        try (Connection con = open()) {
            return insert(con, b);
        } catch (java.sql.SQLException e) {
            throw new DataAccessException("Could not save batch " + b.getBatchNo(), e);
        }
    }

    /** Insert on a caller-supplied connection, so it can be part of a JDBC transaction. */
    public long insert(Connection con, Batch b) throws DataAccessException {
        long id = insertAndGetKey(con, INSERT_SQL,
                b.getBatchNo(), b.getMedicineName(), b.getStrength(), b.getDosageForm(),
                b.getManufacturerId(), b.getMfgDate(), b.getExpiryDate(), b.getMrp(), b.getPackCount());
        b.setBatchId(id);
        return id;
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM batches");
    }

    // ---------------------------------------------------------------- extra finders

    public Optional<Batch> findByBatchNo(String batchNo) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM batches WHERE batch_no = ?", MAPPER, batchNo);
    }

    public boolean existsByBatchNo(String batchNo) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM batches WHERE batch_no = ?", batchNo) > 0;
    }

    public long countByManufacturer(long manufacturerId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM batches WHERE manufacturer_id = ?", manufacturerId);
    }

    /** Every batch made by this manufacturer, wherever its packs are now. */
    public List<StockLine> findSummaryByManufacturer(long manufacturerId, int limit) throws DataAccessException {
        return queryList(STOCK_SELECT + "WHERE b.manufacturer_id = ? "
                + "GROUP BY b.batch_id ORDER BY b.created_at DESC LIMIT ?", STOCK_MAPPER, manufacturerId, limit);
    }

    /** Batches whose packs are currently held by this distributor / pharmacy. */
    public List<StockLine> findStockByHolder(long holderId, int limit) throws DataAccessException {
        return queryList(STOCK_SELECT + "WHERE p.current_holder_id = ? "
                + "GROUP BY b.batch_id ORDER BY b.expiry_date ASC LIMIT ?", STOCK_MAPPER, holderId, limit);
    }
}
