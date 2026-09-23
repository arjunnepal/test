package com.c3s.ai.core.servlet;

import com.c3s.ai.common.config.Config;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Baseline security headers on every response, ordered ahead of AuthFilter in web.xml so that
 * filter's redirects and 403s get them too.
 */
public class SecurityHeadersFilter implements Filter {

    private static final String CSP = "default-src 'self'; "
            + "img-src 'self' data:; "
            // 'unsafe-inline' is for style attributes only - the giving page sets category
            // colours and card backgrounds inline. It does not loosen script-src.
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src https://fonts.gstatic.com; "
            + "script-src 'self'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'";

    // the deployment is HTTPS-only (the same switch as the Secure session cookie); behind a
    // TLS-terminating proxy req.isSecure() is false unless RemoteIpValve is configured
    private boolean httpsOnly;

    @Override
    public void init(FilterConfig filterConfig) {
        httpsOnly = Config.INSTANCE.getBoolean("session.cookie.secure", "SESSION_COOKIE_SECURE", true);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Content-Security-Policy", CSP);
        if (httpsOnly || req.isSecure()) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        chain.doFilter(request, response);
    }
}
