package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Report;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code reports} table (customer fake-medicine complaints).
 */
public class ReportDAO extends AbstractDAO implements GenericDAO<Report, Long> {

    private static final String COLUMNS = "report_id, pack_code, scan_id, reporter_name, reporter_phone, city, "
            + "description, status, created_at";

    private static final RowMapper<Report> MAPPER = rs -> {
        Report r = new Report();
        r.setReportId(rs.getLong("report_id"));
        r.setPackCode(rs.getString("pack_code"));
        r.setScanId(getNullableLong(rs, "scan_id"));
        r.setReporterName(rs.getString("reporter_name"));
        r.setReporterPhone(rs.getString("reporter_phone"));
        r.setCity(rs.getString("city"));
        r.setDescription(rs.getString("description"));
        r.setStatus(Report.Status.valueOf(rs.getString("status")));
        r.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return r;
    };

    @Override
    public Optional<Report> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM reports WHERE report_id = ?", MAPPER, id);
    }

    @Override
    public List<Report> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM reports ORDER BY created_at DESC", MAPPER);
    }

    @Override
    public Long save(Report r) throws DataAccessException {
        long id = insertAndGetKey("INSERT INTO reports (pack_code, scan_id, reporter_name, reporter_phone, city, "
                        + "description, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                r.getPackCode(), r.getScanId(), r.getReporterName(), r.getReporterPhone(), r.getCity(),
                r.getDescription(), r.getStatus());
        r.setReportId(id);
        return id;
    }

    /** Only the workflow status of a report may change. */
    @Override
    public boolean update(Report r) throws DataAccessException {
        return executeUpdate("UPDATE reports SET status = ? WHERE report_id = ?", r.getStatus(), r.getReportId()) == 1;
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM reports");
    }

    /** Open + investigating reports about packs from this manufacturer's batches. */
    public long countOpenForManufacturer(long manufacturerId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM reports r JOIN packs p ON p.pack_code = r.pack_code "
                + "JOIN batches b ON b.batch_id = p.batch_id "
                + "WHERE b.manufacturer_id = ? AND r.status <> 'CLOSED'", manufacturerId);
    }

    public List<Report> findByPackCode(String packCode) throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM reports WHERE pack_code = ? ORDER BY created_at DESC",
                MAPPER, packCode);
    }
}
