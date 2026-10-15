package com.meditrace.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * First filter for every request (order is set in web.xml):
 * <ul>
 *   <li>forces UTF-8 so names and the ₹ symbol survive form posts;</li>
 *   <li>adds basic security headers to every response.</li>
 * </ul>
 */
public class EncodingFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.setHeader("X-Content-Type-Options", "nosniff");     // no MIME-type guessing
        response.setHeader("X-Frame-Options", "DENY");               // no click-jacking via iframes
        response.setHeader("Referrer-Policy", "same-origin");

        chain.doFilter(request, response);
    }
}
