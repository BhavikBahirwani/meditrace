package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Recall;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code recalls} table.
 */
public class RecallDAO extends AbstractDAO implements GenericDAO<Recall, Long> {

    private static final String COLUMNS = "recall_id, batch_id, issued_by, reason, severity, recall_date, active";

    private static final RowMapper<Recall> MAPPER = rs -> {
        Recall r = new Recall();
        r.setRecallId(rs.getLong("recall_id"));
        r.setBatchId(rs.getLong("batch_id"));
        r.setIssuedBy(rs.getLong("issued_by"));
        r.setReason(rs.getString("reason"));
        r.setSeverity(Recall.Severity.valueOf(rs.getString("severity")));
        r.setRecallDate(rs.getObject("recall_date", LocalDate.class));
        r.setActive(rs.getBoolean("active"));
        return r;
    };

    @Override
    public Optional<Recall> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM recalls WHERE recall_id = ?", MAPPER, id);
    }

    @Override
    public List<Recall> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM recalls ORDER BY recall_date DESC", MAPPER);
    }

    @Override
    public Long save(Recall r) throws DataAccessException {
        long id = insertAndGetKey("INSERT INTO recalls (batch_id, issued_by, reason, severity, recall_date, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                r.getBatchId(), r.getIssuedBy(), r.getReason(), r.getSeverity(), r.getRecallDate(), r.isActive());
        r.setRecallId(id);
        return id;
    }

    /** A recall can be lifted (deactivated) but never erased. */
    @Override
    public boolean update(Recall r) throws DataAccessException {
        return executeUpdate("UPDATE recalls SET active = ? WHERE recall_id = ?", r.isActive(), r.getRecallId()) == 1;
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM recalls");
    }

    public Optional<Recall> findActiveByBatch(long batchId) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM recalls WHERE batch_id = ? AND active "
                + "ORDER BY recall_date DESC LIMIT 1", MAPPER, batchId);
    }

    public long countActiveForManufacturer(long manufacturerId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM recalls r JOIN batches b ON b.batch_id = r.batch_id "
                + "WHERE b.manufacturer_id = ? AND r.active", manufacturerId);
    }

    /** Recalled batches that still have packs at this distributor / pharmacy. */
    public long countActiveForHolder(long holderId) throws DataAccessException {
        return queryLong("SELECT COUNT(DISTINCT r.batch_id) FROM recalls r "
                + "JOIN packs p ON p.batch_id = r.batch_id "
                + "WHERE r.active AND p.current_holder_id = ? AND p.status = 'IN_CHAIN'", holderId);
    }
}
