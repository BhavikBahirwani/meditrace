package com.meditrace.servlet;

import com.meditrace.exception.DataAccessException;
import com.meditrace.model.User;
import com.meditrace.service.DashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One servlet serves all three dashboards. {@code AuthFilter} has already guaranteed the
 * user is logged in and inside their own role's area; the JSP is chosen from the role
 * ({@code dashboard/manufacturer.jsp}, ...).
 */
@WebServlet(name = "DashboardServlet",
        urlPatterns = {"/manufacturer/dashboard", "/distributor/dashboard", "/pharmacy/dashboard"})
public class DashboardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(DashboardServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rejectIfNotStarted(req, resp)) {
            return;
        }
        User user = currentUser(req);
        try {
            DashboardService service = new DashboardService(blockchain(), miner(), validator());
            req.setAttribute("user", user);
            req.setAttribute("view", service.build(user));
            consumeFlash(req);
            forward(req, resp, "dashboard/" + user.getRole().getSlug() + ".jsp");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Dashboard failed for " + user, e);
            showError(req, resp, 500, "Database error", "Your dashboard could not be loaded. Please try again.");
        }
    }
}
