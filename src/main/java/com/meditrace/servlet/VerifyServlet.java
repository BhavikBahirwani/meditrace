package com.meditrace.servlet;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.VerificationResult;
import com.meditrace.service.VerificationService;
import com.meditrace.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public verification endpoint: {@code GET /verify?code=MT-B1023-7F3A9C}.
 * <p>
 * This is the URL a pack's QR code points to, so a phone camera opens the result page
 * directly. No login is needed. Every lookup is logged in the {@code scans} table.
 */
@WebServlet(name = "VerifyServlet", urlPatterns = "/verify")
public class VerifyServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(VerifyServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        if (ValidationUtil.isBlank(code)) {
            redirect(req, resp, "/");
            return;
        }
        if (rejectIfNotStarted(req, resp)) {
            return;
        }
        try {
            VerificationService service = new VerificationService(blockchain());
            VerificationResult result = service.verify(code, req.getRemoteAddr(), req.getHeader("User-Agent"));
            req.setAttribute("result", result);
            // Verification results must always be fresh.
            resp.setHeader("Cache-Control", "no-store");
            forward(req, resp, "verify.jsp");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Verification failed for " + code, e);
            showError(req, resp, 500, "Could not verify right now",
                    "The verification service could not reach its database. Please try again shortly.");
        }
    }
}
