package com.c3s.ai.core.servlet;

import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.auth.AuthSession.Scope;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.c3s.ai.core.web.Routes;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Puts requestId and the signed-in user in the MDC so a request's log lines line up; reuses an
 * inbound X-Request-Id when present. First in the web.xml filter order.
 */
public class MdcFilter implements Filter {

    private static final int MAX_INBOUND_ID_LEN = 64;
    private static final int GENERATED_ID_LEN = 8;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        String header = req.getHeader("X-Request-Id");
        String requestId = null;
        if (header != null && !header.isBlank() && header.length() <= MAX_INBOUND_ID_LEN) {
            String sanitized = header.replaceAll("[^A-Za-z0-9._-]", "");
            if (!sanitized.isBlank()) {
                requestId = sanitized;
            }
        }
        if (requestId == null) {
            requestId = UUID.randomUUID().toString().substring(0, GENERATED_ID_LEN);
        }

        MDC.put("requestId", requestId);
        // the identity that authorises this request - /admin runs on the admin scope, everything
        // else on the donor one, and both can be live at once as different people
        String user = AuthSession.currentUser(req, scopeFor(req));
        if (user != null) {
            MDC.put("user", user);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static Scope scopeFor(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        String path = pathInfo == null ? req.getServletPath() : req.getServletPath() + pathInfo;
        return Routes.isAdminPath(path) ? Scope.ADMIN : Scope.USER;
    }
}
