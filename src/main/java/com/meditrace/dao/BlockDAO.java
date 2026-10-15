package com.meditrace.dao;

import com.meditrace.blockchain.Block;
import com.meditrace.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the {@code blocks} table - the persisted blockchain.
 * <p>
 * The table is <b>append-only</b>: {@code update} and {@code delete} are inherited from
 * {@link GenericDAO}'s default methods and throw {@link UnsupportedOperationException}.
 * The primary key is the block index ({@code Integer}), showing the second type parameter
 * of {@code GenericDAO<T, ID>} in use.
 */
public class BlockDAO extends AbstractDAO implements GenericDAO<Block, Integer> {

    private static final String COLUMNS = "block_index, prev_hash, hash, nonce, `timestamp`, data_json, signature";

    private static final String INSERT_SQL = "INSERT INTO blocks "
            + "(block_index, prev_hash, hash, nonce, `timestamp`, data_json, signature) VALUES (?, ?, ?, ?, ?, ?, ?)";

    /** Rebuilds the block exactly as stored, without re-hashing, so tampering stays visible. */
    private static final RowMapper<Block> MAPPER = rs -> new Block(
            rs.getInt("block_index"),
            rs.getString("prev_hash"),
            rs.getLong("timestamp"),
            rs.getString("data_json"),
            rs.getLong("nonce"),
            rs.getString("hash"),
            rs.getString("signature"));

    @Override
    public Optional<Block> findById(Integer index) throws DataAccessException {
        return queryOne("SELECT " + COLUMNS + " FROM blocks WHERE block_index = ?", MAPPER, index);
    }

    /** All blocks in chain order - used to rebuild and to validate the chain. */
    @Override
    public List<Block> findAll() throws DataAccessException {
        return queryList("SELECT " + COLUMNS + " FROM blocks ORDER BY block_index", MAPPER);
    }

    @Override
    public Integer save(Block block) throws DataAccessException {
        try (Connection con = open()) {
            insert(con, block);
            return block.getIndex();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save block " + block.getIndex(), e);
        }
    }

    /** Insert on a caller-supplied connection (part of the Miner's transaction). */
    public void insert(Connection con, Block b) throws DataAccessException {
        executeUpdate(con, INSERT_SQL, b.getIndex(), b.getPrevHash(), b.getHash(), b.getNonce(),
                b.getTimestamp(), b.getDataJson(), b.getSignature());
    }

    @Override
    public long count() throws DataAccessException {
        return queryLong("SELECT COUNT(*) FROM blocks");
    }
}
