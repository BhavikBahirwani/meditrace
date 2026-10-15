package com.meditrace.filter;

import com.meditrace.exception.UnauthorizedException;
import com.meditrace.model.Role;
import com.meditrace.model.User;
import com.meditrace.util.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Role-based access control for the private areas (mapped in web.xml to
 * {@code /manufacturer/*}, {@code /distributor/*} and {@code /pharmacy/*}).
 * <ol>
 *   <li>No logged-in user - redirect to the login page, remembering where they wanted to go.</li>
 *   <li>Logged in, but the first URL segment is not their own role's area (e.g. a pharmacy
 *       opening {@code /manufacturer/dashboard}) - HTTP 403 Forbidden.</li>
 *   <li>Otherwise the request continues to the servlet.</li>
 * </ol>
 * Keeping this check in ONE filter means no servlet can forget it.
 */
public class AuthFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);       // do not create a session here
        User user = (session == null) ? null : (User) session.getAttribute(AppConstants.SESSION_USER);

        if (user == null) {
            String target = request.getRequestURI().substring(request.getContextPath().length());
            if (request.getQueryString() != null) {
                target += "?" + request.getQueryString();
            }
            response.sendRedirect(request.getContextPath() + "/login?next="
                    + URLEncoder.encode(target, StandardCharsets.UTF_8));
            return;
        }

        String area = firstSegment(request.getServletPath());   // "manufacturer", "distributor", "pharmacy"
        Optional<Role> required = Role.fromSlug(area);
        if (required.isEmpty() || required.get() != user.getRole()) {
            UnauthorizedException denied = new UnauthorizedException("This area is only for "
                    + required.map(r -> r.getDisplayName() + " accounts").orElse("authorised users")
                    + ". You are signed in as a " + user.getRole().getDisplayName() + ".");
            request.setAttribute("errorMessage", denied.getMessage());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, denied.getMessage());
            return;
        }

        // Private pages must not be cached: after logout, "Back" must not show them again.
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    private static String firstSegment(String servletPath) {
        String path = servletPath.startsWith("/") ? servletPath.substring(1) : servletPath;
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(0, slash);
    }
}
