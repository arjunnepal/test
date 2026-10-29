package com.c3s.ai.browse.servlet;

import com.c3s.ai.browse.auth.AuthSession;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

// Puts requestId and user in the MDC for logging. "AAA" makes it the first filter.
@WebFilter(filterName = "AAAMdcFilter", urlPatterns = "/*")
public class MdcFilter implements Filter {

    private static final int MAX_INBOUND_ID_LEN = 64;
    private static final int GENERATED_ID_LEN = 8;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        // reuse the caller's request id if it looks valid
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
        String user = AuthSession.currentUser(req);
        if (user != null) {
            MDC.put("user", user);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
