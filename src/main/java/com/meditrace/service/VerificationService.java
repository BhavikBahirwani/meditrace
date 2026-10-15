package com.meditrace.service;

import com.meditrace.blockchain.Block;
import com.meditrace.blockchain.Blockchain;
import com.meditrace.dao.BatchDAO;
import com.meditrace.dao.BlockDAO;
import com.meditrace.dao.PackDAO;
import com.meditrace.dao.RecallDAO;
import com.meditrace.dao.ScanDAO;
import com.meditrace.dao.TransactionDAO;
import com.meditrace.dao.UserDAO;
import com.meditrace.exception.DataAccessException;
import com.meditrace.exception.ProductNotFoundException;
import com.meditrace.exception.TamperDetectedException;
import com.meditrace.model.Batch;
import com.meditrace.model.JourneyStep;
import com.meditrace.model.Pack;
import com.meditrace.model.Scan;
import com.meditrace.model.Transaction;
import com.meditrace.model.TxType;
import com.meditrace.model.User;
import com.meditrace.model.VerificationResult;
import com.meditrace.model.VerificationStatus;
import com.meditrace.util.JsonUtil;
import com.meditrace.util.ValidationUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Answers the customer's question: "Is this pack genuine?"
 * <p>
 * Decision order:
 * <ol>
 *   <li>Code unknown (or malformed) - <b>Not found</b>.</li>
 *   <li>Any record of the pack's batch fails its blockchain check - <b>Tampered</b>.
 *       See {@link #checkIntegrity}.</li>
 *   <li>Pack already marked SOLD - <b>Possible fake</b>: a second physical pack with the
 *       same code means the QR was probably copied.</li>
 *   <li>Otherwise - <b>Genuine</b>.</li>
 * </ol>
 * Every check is logged to the {@code scans} table.
 */
public class VerificationService {

    private static final Logger LOG = Logger.getLogger(VerificationService.class.getName());

    private final Blockchain blockchain;
    private final PackDAO packDAO = new PackDAO();
    private final BatchDAO batchDAO = new BatchDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final BlockDAO blockDAO = new BlockDAO();
    private final UserDAO userDAO = new UserDAO();
    private final RecallDAO recallDAO = new RecallDAO();
    private final ScanDAO scanDAO = new ScanDAO();

    public VerificationService(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    /**
     * Verifies a pack code and logs the scan.
     *
     * @param rawCode   what the customer typed or the QR contained
     * @param ipAddress client IP (for the scan log)
     * @param userAgent browser string (for the scan log)
     */
    public VerificationResult verify(String rawCode, String ipAddress, String userAgent) throws DataAccessException {
        String code = ValidationUtil.normalisePackCode(rawCode);
        VerificationResult result;
        try {
            result = evaluate(code);
        } catch (ProductNotFoundException e) {
            result = new VerificationResult(code, VerificationStatus.NOT_FOUND);
        }
        logScan(result, ipAddress, userAgent);
        return result;
    }

    /** Core logic, separated from logging so it reads top to bottom. */
    private VerificationResult evaluate(String code) throws DataAccessException, ProductNotFoundException {
        if (!ValidationUtil.isValidPackCode(code)) {
            throw new ProductNotFoundException(code);
        }
        Pack pack = packDAO.findByCode(code).orElseThrow(() -> new ProductNotFoundException(code));
        Batch batch = batchDAO.findById(pack.getBatchId()).orElseThrow(() -> new ProductNotFoundException(code));
        batch.setPackCodes(packDAO.findCodesByBatch(batch.getBatchId()));
        List<Transaction> history = transactionDAO.findByBatch(batch.getBatchId());

        // Small per-request cache so each participant is loaded from the DB only once.
        Map<Long, User> users = new HashMap<>();

        VerificationResult result = new VerificationResult(code, VerificationStatus.GENUINE);
        result.setPack(pack);
        result.setBatch(batch);
        result.setManufacturer(lookupUser(users, batch.getManufacturerId()));
        if (pack.getCurrentHolderId() != null) {
            result.setCurrentHolder(lookupUser(users, pack.getCurrentHolderId()));
        }
        recallDAO.findActiveByBatch(batch.getBatchId()).ifPresent(result::setRecall);
        result.setPreviousScans(scanDAO.countByPackCode(code));

        // ---- 1. integrity: do the DB rows still match what the blockchain sealed?
        try {
            checkIntegrity(batch, history);
        } catch (TamperDetectedException e) {
            result.setStatus(VerificationStatus.TAMPERED);
            result.setDetail(e.getMessage());
        }

        // ---- 2. journey timeline (built for every status, so the customer sees the facts)
        buildJourney(result, pack, history, users);

        // ---- 3. copied QR?
        if (result.getStatus() == VerificationStatus.GENUINE && pack.isSold()) {
            result.setStatus(VerificationStatus.POSSIBLE_FAKE);
            result.setDetail("This code was already sold to a customer in " + pack.getSoldCity() + " on "
                    + pack.getSoldAtText() + ". If you did not just buy it there, the QR on your pack may "
                    + "have been copied - please do not use it and report it to the pharmacist.");
        }
        return result;
    }

    /**
     * Tamper check for one batch. For every transaction already sealed in a block:
     * <ol>
     *   <li>the block row in MySQL must still hash correctly ({@link Block#verify()});</li>
     *   <li>it must equal the trusted in-memory block at that index (catches a re-mined forgery);</li>
     *   <li>the transaction row's fingerprint must equal the {@code txHash} sealed in the block;</li>
     *   <li>for the REGISTER block, the batch fingerprint (details + all pack codes) must equal
     *       the sealed {@code batchHash}.</li>
     * </ol>
     *
     * @throws TamperDetectedException describing the first mismatch
     */
    void checkIntegrity(Batch batch, List<Transaction> history) throws TamperDetectedException, DataAccessException {
        boolean hasRegister = false;
        for (Transaction tx : history) {
            if (tx.getTxType() == TxType.REGISTER) {
                hasRegister = true;
            }
            if (!tx.isAnchored()) {
                continue; // still in the Miner queue; shown as "pending" on the timeline
            }
            int idx = tx.getBlockIndex();
            Block stored = blockDAO.findById(idx)
                    .orElseThrow(() -> new TamperDetectedException("Block #" + idx + " is missing from the ledger.", idx));
            Optional<Block> trusted = blockchain.getBlock(idx);
            if (!stored.verify() || trusted.isEmpty() || !trusted.get().getHash().equals(stored.getHash())) {
                throw new TamperDetectedException("Block #" + idx + " in the ledger was altered.", idx);
            }
            String sealedTxHash = JsonUtil.getString(stored.getDataJson(), "txHash");
            if (!tx.computeHash().equals(sealedTxHash)) {
                throw new TamperDetectedException("The " + tx.getTxType().name().toLowerCase()
                        + " record of " + tx.getTxTimeText() + " was changed after it was sealed in block #" + idx + ".", idx);
            }
            if (tx.getTxType() == TxType.REGISTER) {
                String sealedBatchHash = JsonUtil.getString(stored.getDataJson(), "batchHash");
                if (!batch.computeHash().equals(sealedBatchHash)) {
                    throw new TamperDetectedException("Batch " + batch.getBatchNo()
                            + " details (dates, MRP or pack list) no longer match block #" + idx + ".", idx);
                }
            }
        }
        if (!hasRegister) {
            throw new TamperDetectedException("Batch " + batch.getBatchNo() + " has no registration record.", -1);
        }
    }

    /** Turns the batch's custody history into timeline steps (uses polymorphic labels). */
    private void buildJourney(VerificationResult result, Pack pack, List<Transaction> history,
                              Map<Long, User> users) throws DataAccessException {
        for (Transaction tx : history) {
            // REGISTER happens at the manufacturer; a HANDOVER is shown at the receiver.
            long actorId = (tx.getTxType() == TxType.REGISTER || tx.getToUserId() == null)
                    ? tx.getFromUserId() : tx.getToUserId();
            User actor = lookupUser(users, actorId);
            String hash = tx.isAnchored()
                    ? blockchain.getBlock(tx.getBlockIndex()).map(Block::getShortHash).orElse("")
                    : "";
            result.addStep(new JourneyStep(
                    actor == null ? "Unknown participant" : actor.getJourneyLabel(),   // dynamic dispatch
                    actor == null ? "-" : actor.getCompany(),
                    actor == null ? "" : actor.getCity(),
                    tx.getTxTimeText(),
                    actor == null ? "unknown" : actor.getRole().getSlug(),
                    tx.getBlockIndex(),
                    hash,
                    tx.isAnchored() && result.getStatus() != VerificationStatus.TAMPERED));
        }
        if (pack.isSold()) {
            result.addStep(new JourneyStep("Sold to a customer", "Retail sale", pack.getSoldCity(),
                    pack.getSoldAtText(), "customer", null, "", false));
        }
    }

    private User lookupUser(Map<Long, User> cache, long userId) throws DataAccessException {
        User cached = cache.get(userId);
        if (cached == null) {
            cached = userDAO.findById(userId).orElse(null);
            cache.put(userId, cached);
        }
        return cached;
    }

    /** Logging must never break verification, so failures are only written to the server log. */
    private void logScan(VerificationResult result, String ip, String userAgent) {
        try {
            Long packId = result.getPack() == null ? null : result.getPack().getPackId();
            scanDAO.save(new Scan(ValidationUtil.truncate(result.getPackCode(), 40), packId, result.getStatus(),
                    ValidationUtil.truncate(ip, 45), ValidationUtil.truncate(userAgent, 255)));
        } catch (DataAccessException e) {
            LOG.log(Level.WARNING, "Could not log scan for " + result.getPackCode(), e);
        }
    }
}
