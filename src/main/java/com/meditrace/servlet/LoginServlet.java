package com.meditrace.servlet;

import com.meditrace.exception.AuthenticationException;
import com.meditrace.exception.DataAccessException;
import com.meditrace.model.User;
import com.meditrace.service.AuthService;
import com.meditrace.util.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GET shows the login form; POST checks the PBKDF2 password and starts a session.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

    private transient AuthService authService;

    @Override
    public void init() {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        if (user != null) {                       // already logged in: go straight to the dashboard
            redirect(req, resp, user.getDashboardPath());
            return;
        }
        consumeFlash(req);
        req.setAttribute("next", safeNext(req.getParameter("next")));
        forward(req, resp, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rejectIfNotStarted(req, resp)) {
            return;
        }
        String username = req.getParameter("username");
        String next = safeNext(req.getParameter("next"));
        try {
            User user = authService.login(username, req.getParameter("password"));

            // Prevent session fixation: give the browser a NEW session id once authenticated.
            HttpSession session = req.getSession();
            req.changeSessionId();
            session.setMaxInactiveInterval(AppConstants.SESSION_TIMEOUT_MINUTES * 60);
            session.setAttribute(AppConstants.SESSION_USER, user);

            LOG.info(() -> "Login: " + user);
            redirect(req, resp, next != null && next.startsWith("/" + user.getRole().getSlug() + "/")
                    ? next : user.getDashboardPath());
        } catch (AuthenticationException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("username", username);
            req.setAttribute("next", next);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            forward(req, resp, "login.jsp");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Login failed due to database error", e);
            showError(req, resp, 500, "Database error", "We could not check your login right now. Please try again.");
        }
    }

    /** Only allow local paths as "next" - never a full URL (prevents open-redirect attacks). */
    private static String safeNext(String next) {
        return (next != null && next.startsWith("/") && !next.startsWith("//")) ? next : null;
    }
}
