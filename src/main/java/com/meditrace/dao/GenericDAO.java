package com.meditrace.dao;

import com.meditrace.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object contract, implemented once per table.
 * <p>
 * <b>Generics:</b> {@code T} is the entity type (e.g. {@code Batch}) and {@code ID} the
 * primary-key type (e.g. {@code Long}, or {@code Integer} for blocks), so the compiler
 * stops anyone saving a {@code Pack} through the {@code UserDAO}.
 * <p>
 * {@code update} and {@code delete} are <b>default methods</b>: tables that are append-only
 * by design (blocks, transactions, scans) simply do not override them, and calling them
 * throws {@link UnsupportedOperationException}.
 *
 * @param <T>  entity type
 * @param <ID> primary key type
 */
public interface GenericDAO<T, ID> {

    /** @return the row with this primary key, or {@link Optional#empty()} */
    Optional<T> findById(ID id) throws DataAccessException;

    /** @return all rows (tables in this project are small; add paging before production) */
    List<T> findAll() throws DataAccessException;

    /** Inserts a new row. @return the generated (or given) primary key */
    ID save(T entity) throws DataAccessException;

    /** @return number of rows in the table */
    long count() throws DataAccessException;

    /** Updates an existing row. Not supported by append-only tables. */
    default boolean update(T entity) throws DataAccessException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is append-only: update not allowed");
    }

    /** Deletes a row. Not supported by append-only tables. */
    default boolean delete(ID id) throws DataAccessException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is append-only: delete not allowed");
    }
}
