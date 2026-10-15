package com.meditrace.listener;

import com.meditrace.blockchain.Blockchain;
import com.meditrace.blockchain.ChainValidator;
import com.meditrace.blockchain.Miner;
import com.meditrace.blockchain.MiningTask;
import com.meditrace.exception.MediTraceException;
import com.meditrace.service.LedgerService;
import com.meditrace.util.AppConstants;
import com.meditrace.util.DBConnection;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application life-cycle hook: runs once when Tomcat deploys MediTrace and once when it
 * stops it.
 * <p>
 * <b>Start-up</b>
 * <ol>
 *   <li>checks the database connection ({@code db.properties});</li>
 *   <li>loads the blockchain from the {@code blocks} table, creating the genesis block if
 *       the table is empty;</li>
 *   <li>starts the {@link Miner} thread and queues every transaction not yet sealed
 *       (e.g. rows inserted by {@code seed.sql});</li>
 *   <li>schedules the {@link ChainValidator} every 2 minutes;</li>
 *   <li>publishes these objects as ServletContext attributes for the servlets.</li>
 * </ol>
 * If the database is unreachable the app still starts; pages then show a clear
 * "database not connected" message instead of crashing.
 * <p>
 * <b>Shutdown</b> stops both background threads and deregisters the JDBC driver, so
 * redeploying in Eclipse does not leak threads or classloaders.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        try {
            if (!DBConnection.getInstance().testConnection()) {
                throw new MediTraceException("Cannot connect to MySQL. Check db.url, db.username and db.password "
                        + "in src/main/resources/db.properties and that the MySQL service is running.");
            }
            LedgerService ledger = new LedgerService();
            Blockchain blockchain = ledger.loadOrCreateChain(AppConstants.POW_DIFFICULTY);

            Miner miner = new Miner(blockchain, ledger);
            miner.start();
            List<Long> pending = ledger.findUnanchoredTransactionIds();
            for (Long txId : pending) {
                miner.submit(MiningTask.of(txId));
            }
            LOG.info("Queued " + pending.size() + " unsealed transaction(s) for mining");

            ChainValidator validator = new ChainValidator(blockchain, ledger,
                    AppConstants.VALIDATOR_INTERVAL_SECONDS);
            validator.start();

            ctx.setAttribute(AppConstants.CTX_BLOCKCHAIN, blockchain);
            ctx.setAttribute(AppConstants.CTX_MINER, miner);
            ctx.setAttribute(AppConstants.CTX_VALIDATOR, validator);
            LOG.info("MediTrace started: chain length " + blockchain.size());
        } catch (MediTraceException | RuntimeException e) {
            LOG.log(Level.SEVERE, "MediTrace could not start its blockchain services", e);
            ctx.setAttribute(AppConstants.CTX_STARTUP_ERROR, e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        if (ctx.getAttribute(AppConstants.CTX_VALIDATOR) instanceof ChainValidator validator) {
            validator.shutdown();
        }
        if (ctx.getAttribute(AppConstants.CTX_MINER) instanceof Miner miner) {
            miner.shutdown();
        }
        deregisterJdbcDrivers();
        LOG.info("MediTrace stopped");
    }

    /** Removes drivers loaded by this web app's classloader and stops MySQL's cleanup thread. */
    private void deregisterJdbcDrivers() {
        ClassLoader webappLoader = Thread.currentThread().getContextClassLoader();
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getClassLoader() == webappLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Could not deregister JDBC driver " + driver, e);
                }
            }
        }
        AbandonedConnectionCleanupThread.checkedShutdown();
    }
}
