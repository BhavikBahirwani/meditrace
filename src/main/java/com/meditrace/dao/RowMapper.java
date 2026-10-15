package com.meditrace.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts the current row of a {@link ResultSet} into an object.
 * <p>
 * A <b>functional interface</b> (one abstract method), so each DAO can supply its mapping
 * as a lambda, e.g. {@code rs -> new Foo(rs.getLong("id"))}.
 *
 * @param <T> the type produced for each row
 */
@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
