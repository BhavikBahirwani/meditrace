package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Pack;
import com.meditrace.model.PackStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code packs} table (one row per physical pack).
 */
public class PackDAO extends AbstractDAO implements GenericDAO<Pack, Long> {

    private static final String COLUMNS =
            "pack_id, pack_code, batch_id, status, current_holder_id, sold_city, sold_at, created_at";

    private static final String INSERT_SQL =
            "INSERT INTO packs (pack_code, batch_id, status, current_holder_id) VALUES (?, ?, ?, ?)";

    private static final RowMapper<Pack> MAPPER = rs -> {
        Pack p = new Pack();
        p.setPackId(rs.getLong("pack_id"));
        p.setPackCode(rs.getString("pack_code"));
        p.setBatchId(rs.getLong("batch_id"));
        p.setStatus(PackStatus.valueOf(rs.getString("status")));
        p.setCurrentHolderId(getNullableLong(rs, "current_holder_id"));
        p.setSoldCity(rs.getString("sold_city"));
        p.setSoldAt(rs.getObject("sold_at", LocalDateTime.class));
        p.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return p;
    };

    @Override
    public Optional<Pack> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM packs WHERE pack_id = ?", MAPPER, id);
    }

    @Override
    public List<Pack> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM packs ORDER BY pack_id", MAPPER);
    }

    @Override
    public Long save(Pack p) throws DataAccessException {
        long id = insertAndGetKey(INSERT_SQL, p.getPackCode(), p.getBatchId(), p.getStatus(), p.getCurrentHolderId());
        p.setPackId(id);
        return id;
    }

    /**
     * Inserts many packs in one round-trip using JDBC <b>batch</b> updates
     * ({@code addBatch} / {@code executeBatch}). Runs on the caller's connection so it joins
     * the surrounding transaction.
     */
    public void insertAll(Connection con, List<Pack> packs) throws DataAccessException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            for (Pack p : packs) {
                bind(ps, p.getPackCode(), p.getBatchId(), p.getStatus(), p.getCurrentHolderId());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert packs: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM packs");
    }

    // ---------------------------------------------------------------- extra finders

    /** The lookup behind every customer scan. pack_code has a UNIQUE index, so this is fast. */
    public Optional<Pack> findByCode(String packCode) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM packs WHERE pack_code = ?", MAPPER, packCode);
    }

    public List<Pack> findByBatch(long batchId) throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM packs WHERE batch_id = ? ORDER BY pack_id", MAPPER, batchId);
    }

    /** Just the codes of a batch - input to the batch fingerprint. */
    public List<String> findCodesByBatch(long batchId) throws DataAccessException {
        return queryList("SELECT pack_code FROM packs WHERE batch_id = ?", rs -> rs.getString(1), batchId);
    }

    public long countByHolderAndStatus(long holderId, PackStatus status) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM packs WHERE current_holder_id = ? AND status = ?", holderId, status);
    }

    public long countByManufacturer(long manufacturerId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM packs p JOIN batches b ON b.batch_id = p.batch_id "
                + "WHERE b.manufacturer_id = ?", manufacturerId);
    }

    public long countByManufacturerAndStatus(long manufacturerId, PackStatus status) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM packs p JOIN batches b ON b.batch_id = p.batch_id "
                + "WHERE b.manufacturer_id = ? AND p.status = ?", manufacturerId, status);
    }
}
