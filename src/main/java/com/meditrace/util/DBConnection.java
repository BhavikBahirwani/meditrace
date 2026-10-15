package com.meditrace.util;

import com.meditrace.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thread-safe <b>Singleton</b> that owns the JDBC settings and hands out connections.
 * <p>
 * Settings are read once from {@code db.properties} on the classpath
 * ({@code src/main/resources/db.properties}). That file is git-ignored; the committed
 * {@code db.properties.example} shows its format. No credentials are hard-coded here.
 * <p>
 * Each call to {@link #getConnection()} opens a new connection which the caller must close,
 * always with try-with-resources:
 * <pre>
 * try (Connection con = DBConnection.getInstance().getConnection();
 *      PreparedStatement ps = con.prepareStatement(sql)) { ... }
 * </pre>
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    /** volatile + double-checked locking = lazy, thread-safe singleton. */
    private static volatile DBConnection instance;

    private final String url;
    private final String username;
    private final String password;

    private DBConnection() throws DataAccessException {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new DataAccessException(PROPERTIES_FILE + " not found on the classpath. Copy "
                        + "src/main/resources/db.properties.example to db.properties and set your MySQL password.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + PROPERTIES_FILE, e);
        }

        this.url = require(props, "db.url");
        this.username = require(props, "db.username");
        this.password = props.getProperty("db.password", "");
        String driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        try {
            // Explicit load: inside Tomcat, JDBC auto-discovery does not always see WEB-INF/lib drivers.
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new DataAccessException("JDBC driver not found: " + driver, e);
        }
    }

    private static String require(Properties props, String key) throws DataAccessException {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DataAccessException("Missing '" + key + "' in " + PROPERTIES_FILE);
        }
        return value.trim();
    }

    /** @return the single shared instance, created on first use */
    public static DBConnection getInstance() throws DataAccessException {
        DBConnection local = instance;
        if (local == null) {
            synchronized (DBConnection.class) {
                local = instance;
                if (local == null) {
                    instance = local = new DBConnection();
                }
            }
        }
        return local;
    }

    /** Opens a new JDBC connection. The caller is responsible for closing it. */
    public Connection getConnection() throws DataAccessException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DataAccessException("Cannot connect to MySQL at " + url + " - " + e.getMessage(), e);
        }
    }

    /** Quick health check used at start-up. */
    public boolean testConnection() {
        try (Connection con = getConnection()) {
            return con.isValid(2);
        } catch (DataAccessException | SQLException e) {
            return false;
        }
    }
}
