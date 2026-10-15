package com.meditrace.servlet;

import com.meditrace.util.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Landing page. The empty-string mapping matches exactly the context root
 * ({@code http://localhost:8080/meditrace/}).
 */
@WebServlet(name = "HomeServlet", urlPatterns = "")
public class HomeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("startupError", getServletContext().getAttribute(AppConstants.CTX_STARTUP_ERROR));
        forward(req, resp, "index.jsp");
    }
}
