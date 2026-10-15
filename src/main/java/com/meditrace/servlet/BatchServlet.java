package com.meditrace.servlet;

import com.meditrace.exception.DataAccessException;
import com.meditrace.exception.MediTraceException;
import com.meditrace.exception.UnauthorizedException;
import com.meditrace.model.Batch;
import com.meditrace.model.User;
import com.meditrace.service.BatchService;
import com.meditrace.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manufacturer batch management (protected by AuthFilter: manufacturers only).
 * <ul>
 *   <li>{@code POST /manufacturer/batches} - register a new batch and its packs;</li>
 *   <li>{@code GET  /manufacturer/batches?no=B1023} - batch detail with all pack codes.</li>
 * </ul>
 */
@WebServlet(name = "BatchServlet", urlPatterns = "/manufacturer/batches")
public class BatchServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BatchServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rejectIfNotStarted(req, resp)) {
            return;
        }
        String batchNo = ValidationUtil.normalisePackCode(req.getParameter("no"));
        try {
            Optional<Batch> batch = new BatchService(miner()).findOwnBatch(currentUser(req), batchNo);
            if (batch.isEmpty()) {
                showError(req, resp, 404, "Batch not found", "You have no batch numbered " + batchNo + ".");
                return;
            }
            req.setAttribute("user", currentUser(req));
            req.setAttribute("batch", batch.get());
            consumeFlash(req);
            forward(req, resp, "batch.jsp");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Could not load batch " + batchNo, e);
            showError(req, resp, 500, "Database error", "The batch could not be loaded.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rejectIfNotStarted(req, resp)) {
            return;
        }
        User user = currentUser(req);
        try {
            Batch batch = readForm(req);
            Batch saved = new BatchService(miner()).registerBatch(user, batch);
            setFlash(req, "Batch " + saved.getBatchNo() + " registered with " + saved.getPackCount()
                    + " unique pack codes. It is being sealed on the blockchain.");
            redirect(req, resp, "/manufacturer/batches?no=" + saved.getBatchNo());
        } catch (UnauthorizedException e) {
            showError(req, resp, 403, "Not allowed", e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Batch registration failed", e);
            setFlash(req, "Could not save the batch - nothing was stored. Please try again.");
            redirect(req, resp, user.getDashboardPath());
        } catch (MediTraceException e) {         // validation problems: message is user-friendly
            setFlash(req, e.getMessage());
            redirect(req, resp, user.getDashboardPath() + "#register");
        }
    }

    /** Converts the HTML form strings into a Batch, reporting unparseable values. */
    private static Batch readForm(HttpServletRequest req) throws MediTraceException {
        Batch b = new Batch();
        b.setBatchNo(ValidationUtil.normalisePackCode(req.getParameter("batchNo")));
        b.setMedicineName(ValidationUtil.truncate(ValidationUtil.clean(req.getParameter("medicineName")), 100));
        b.setStrength(ValidationUtil.truncate(ValidationUtil.clean(req.getParameter("strength")), 30));
        b.setDosageForm(ValidationUtil.truncate(ValidationUtil.clean(req.getParameter("dosageForm")), 30));
        try {
            b.setMfgDate(LocalDate.parse(req.getParameter("mfgDate")));
            b.setExpiryDate(LocalDate.parse(req.getParameter("expiryDate")));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new MediTraceException("Please enter valid manufacturing and expiry dates.");
        }
        try {
            b.setMrp(new BigDecimal(req.getParameter("mrp").trim()));
            b.setPackCount(Integer.parseInt(req.getParameter("packCount").trim()));
        } catch (NumberFormatException | NullPointerException e) {
            throw new MediTraceException("MRP and number of packs must be numbers.");
        }
        return b;
    }
}
