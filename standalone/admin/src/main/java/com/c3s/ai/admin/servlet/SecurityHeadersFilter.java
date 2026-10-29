package com.c3s.ai.admin.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Baseline security headers on every response. filterName sorts it ahead of AuthFilter
 * so that filter's redirects get the headers too.
 */
@WebFilter(filterName = "AaSecurityHeadersFilter", urlPatterns = "/*")
public class SecurityHeadersFilter implements Filter {

    private static final String CSP = "default-src 'self'; "
            + "img-src 'self' data:; "
            + "style-src 'self' https://fonts.googleapis.com; "
            + "font-src https://fonts.gstatic.com; "
            + "script-src 'self'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Content-Security-Policy", CSP);
        if (req.isSecure()) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        chain.doFilter(request, response);
    }
}
