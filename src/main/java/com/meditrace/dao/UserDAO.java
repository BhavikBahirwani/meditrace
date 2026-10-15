package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Role;
import com.meditrace.model.User;
import com.meditrace.model.UserFactory;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC access to the {@code users} table.
 * Rows are turned into the right subclass (Manufacturer / Distributor / Pharmacy) by
 * {@link UserFactory}.
 */
public class UserDAO extends AbstractDAO implements GenericDAO<User, Long> {

    private static final String COLUMNS =
            "user_id, username, email, password_hash, full_name, role, company, city, license_no, created_at";

    /** Maps one row to a User subclass chosen by the role column. */
    private static final RowMapper<User> MAPPER = rs -> {
        User u = UserFactory.create(Role.valueOf(rs.getString("role")));
        u.setUserId(rs.getLong("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setCompany(rs.getString("company"));
        u.setCity(rs.getString("city"));
        u.setLicenseNo(rs.getString("license_no"));
        u.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return u;
    };

    @Override
    public Optional<User> findById(Long id) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE user_id = ?", MAPPER, id);
    }

    @Override
    public List<User> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM users ORDER BY role, company", MAPPER);
    }

    @Override
    public Long save(User u) throws DataAccessException {
        long id = insertAndGetKey(
                "INSERT INTO users (username, email, password_hash, full_name, role, company, city, license_no) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                u.getUsername(), u.getEmail(), u.getPasswordHash(), u.getFullName(),
                u.getRole(), u.getCompany(), u.getCity(), u.getLicenseNo());
        u.setUserId(id);
        return id;
    }

    /** Profile update (password and role are not changed here). */
    @Override
    public boolean update(User u) throws DataAccessException {
        return executeUpdate(
                "UPDATE users SET email = ?, full_name = ?, company = ?, city = ?, license_no = ? WHERE user_id = ?",
                u.getEmail(), u.getFullName(), u.getCompany(), u.getCity(), u.getLicenseNo(), u.getUserId()) == 1;
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM users");
    }

    // ---------------------------------------------------------------- extra finders

    public Optional<User> findByUsername(String username) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE username = ?", MAPPER, username);
    }

    public boolean existsByUsername(String username) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM users WHERE username = ?", username) > 0;
    }

    public boolean existsByEmail(String email) throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    /**
     * Number of participants per role. {@link EnumMap} is the natural Map for enum keys:
     * compact, ordered by declaration and faster than a HashMap.
     */
    public Map<Role, Long> countByRole() throws DataAccessException {
        Map<Role, Long> counts = new EnumMap<>(Role.class);
        for (Role r : Role.values()) {
            counts.put(r, 0L);
        }
        queryList("SELECT role, COUNT(*) AS n FROM users GROUP BY role",
                rs -> counts.put(Role.valueOf(rs.getString("role")), rs.getLong("n")));
        return counts;
    }
}
