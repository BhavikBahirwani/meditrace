package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Scan;
import com.meditrace.model.VerificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code scans} table (customer verification log). Append-only.
 */
public class ScanDAO extends AbstractDAO implements GenericDAO<Scan, Long> {

    private static final String COLUMNS = "s.scan_id, s.pack_code, s.pack_id, s.result, s.city, "
            + "s.ip_address, s.user_agent, s.scanned_at";

    private static final RowMapper<Scan> MAPPER = rs -> {
        Scan s = new Scan();
        s.setScanId(rs.getLong("scan_id"));
        s.setPackCode(rs.getString("pack_code"));
        s.setPackId(getNullableLong(rs, "pack_id"));
        s.setResult(VerificationStatus.valueOf(rs.getString("result")));
        s.setCity(rs.getString("city"));
        s.setIpAddress(rs.getString("ip_address"));
        s.setUserAgent(rs.getString("user_agent"));
        s.setScannedAt(rs.getObject("scanned_at", LocalDateTime.class));
        return s;
    };

    /** Scans of packs whose batch was made by a given manufacturer. */
    private static final String FOR_MANUFACTURER = "FROM scans s JOIN packs p ON p.pack_id = s.pack_id "
            + "JOIN batches b ON b.batch_id = p.batch_id WHERE b.manufacturer_id = ? ";

    /** Scans of packs currently held (or sold) by a given pharmacy / distributor. */
    private static final String FOR_HOLDER = "FROM scans s JOIN packs p ON p.pack_id = s.pack_id "
            + "WHERE p.current_holder_id = ? ";

    @Override
    public Optional<Scan> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM scans s WHERE s.scan_id = ?", MAPPER, id);
    }

    @Override
    public List<Scan> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM scans s ORDER BY s.scanned_at DESC", MAPPER);
    }

    @Override
    public Long save(Scan s) throws DataAccessException {
        long id = insertAndGetKey(
                "INSERT INTO scans (pack_code, pack_id, result, city, ip_address, user_agent) VALUES (?, ?, ?, ?, ?, ?)",
                s.getPackCode(), s.getPackId(), s.getResult(), s.getCity(), s.getIpAddress(), s.getUserAgent());
        s.setScanId(id);
        return id;
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM scans");
    }

    // ---------------------------------------------------------------- extra queries

    /** How many times this exact code was checked before. */
    public long countByPackCode(String packCode) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM scans WHERE pack_code = ?", packCode);
    }

    public long countForManufacturer(long manufacturerId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) " + FOR_MANUFACTURER, manufacturerId);
    }

    /** Scans that came back Tampered or Possible fake - the early-warning number. */
    public long countSuspiciousForManufacturer(long manufacturerId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) " + FOR_MANUFACTURER + "AND s.result IN ('TAMPERED','POSSIBLE_FAKE')",
                manufacturerId);
    }

    public long countForHolder(long holderId) throws DataAccessException {
        return queryLong("SELECT COUNT(*) " + FOR_HOLDER, holderId);
    }

    public List<Scan> findRecentForManufacturer(long manufacturerId, int limit) throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " " + FOR_MANUFACTURER + "ORDER BY s.scanned_at DESC LIMIT ?",
                MAPPER, manufacturerId, limit);
    }

    public List<Scan> findRecentForHolder(long holderId, int limit) throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " " + FOR_HOLDER + "ORDER BY s.scanned_at DESC LIMIT ?",
                MAPPER, holderId, limit);
    }
}
