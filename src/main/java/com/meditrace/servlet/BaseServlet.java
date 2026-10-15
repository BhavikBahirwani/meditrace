package com.meditrace.servlet;

import com.meditrace.blockchain.Blockchain;
import com.meditrace.blockchain.ChainValidator;
import com.meditrace.blockchain.Miner;
import com.meditrace.model.User;
import com.meditrace.util.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Common helpers for all MediTrace servlets (inheritance used to avoid repeating code):
 * forwarding to JSPs, redirects, flash messages, the logged-in user and the
 * application-wide blockchain objects created by the listener.
 */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Forwards to a JSP under /WEB-INF/jsp/ (browsers cannot open those directly). */
    protected void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(AppConstants.JSP_DIR + jsp).forward(req, resp);
    }

    /** Redirects to a path inside this web app (Post/Redirect/Get pattern after form posts). */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    /** @return the logged-in user, or null */
    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute(AppConstants.SESSION_USER);
    }

    /** Stores a one-time message that the next page will display and remove. */
    protected void setFlash(HttpServletRequest req, String message) {
        req.getSession().setAttribute(AppConstants.SESSION_FLASH, message);
    }

    /** Moves a pending flash message into the request (attribute "flash") so it is shown only once. */
    protected void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(AppConstants.SESSION_FLASH) != null) {
            req.setAttribute("flash", session.getAttribute(AppConstants.SESSION_FLASH));
            session.removeAttribute(AppConstants.SESSION_FLASH);
        }
    }

    // ---------------------------------------------------------------- application objects

    protected Blockchain blockchain() {
        return (Blockchain) getServletContext().getAttribute(AppConstants.CTX_BLOCKCHAIN);
    }

    protected Miner miner() {
        return (Miner) getServletContext().getAttribute(AppConstants.CTX_MINER);
    }

    protected ChainValidator validator() {
        return (ChainValidator) getServletContext().getAttribute(AppConstants.CTX_VALIDATOR);
    }

    /**
     * If the listener could not start (e.g. MySQL is down), shows a friendly 503 page.
     *
     * @return true when the request was answered with the error page
     */
    protected boolean rejectIfNotStarted(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Object error = getServletContext().getAttribute(AppConstants.CTX_STARTUP_ERROR);
        if (error == null && blockchain() != null) {
            return false;
        }
        showError(req, resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Database not connected",
                error == null ? "MediTrace is still starting. Please try again in a moment." : error.toString());
        return true;
    }

    /** Renders the shared error page with a status code. */
    protected void showError(HttpServletRequest req, HttpServletResponse resp, int status, String title,
                             String message) throws ServletException, IOException {
        resp.setStatus(status);
        req.setAttribute("errorTitle", title);
        req.setAttribute("errorMessage", message);
        forward(req, resp, "error.jsp");
    }
}
