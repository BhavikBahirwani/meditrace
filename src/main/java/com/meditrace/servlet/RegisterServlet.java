package com.meditrace.servlet;

import com.meditrace.exception.AuthenticationException;
import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Role;
import com.meditrace.model.User;
import com.meditrace.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GET shows the sign-up form with the role picker; POST validates and creates the account,
 * then redirects to the login page (Post/Redirect/Get).
 */
@WebServlet(name = "RegisterServlet", urlPatterns = "/register")
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RegisterServlet.class.getName());

    /** Form fields echoed back to the page when validation fails (never the password). */
    private static final String[] ECHO_FIELDS = {"username", "email", "fullName", "role", "company", "city", "licenseNo"};

    private transient AuthService authService;

    @Override
    public void init() {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("roles", Role.values());
        req.setAttribute("role", req.getParameter("role"));
        forward(req, resp, "register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (rejectIfNotStarted(req, resp)) {
            return;
        }
        try {
            User user = authService.register(
                    req.getParameter("username"), req.getParameter("email"),
                    req.getParameter("password"), req.getParameter("confirm"),
                    req.getParameter("fullName"), req.getParameter("role"),
                    req.getParameter("company"), req.getParameter("city"), req.getParameter("licenseNo"));
            setFlash(req, "Welcome to MediTrace, " + user.getCompany() + "! Please sign in.");
            redirect(req, resp, "/login");
        } catch (AuthenticationException e) {
            for (String field : ECHO_FIELDS) {
                req.setAttribute(field, req.getParameter(field));
            }
            req.setAttribute("roles", Role.values());
            req.setAttribute("error", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forward(req, resp, "register.jsp");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Registration failed", e);
            showError(req, resp, 500, "Database error", "Your account could not be created. Please try again.");
        }
    }
}
