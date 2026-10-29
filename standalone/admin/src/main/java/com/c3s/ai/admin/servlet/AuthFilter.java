package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.auth.AuthSession;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.web.Csrf;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Every request: /css and /js and /healthz pass; non-GET needs a valid CSRF token;
 * /login needs no session; everything else needs one. A CSRF or auth miss = drop the
 * session and redirect to /login.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // servletPath+pathInfo is already decoded and normalized, unlike getRequestURI()
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String path = pathInfo == null ? servletPath : servletPath + pathInfo;

        if (path.startsWith("/css/") || path.startsWith("/js/") || path.equals("/healthz")) {
            chain.doFilter(request, response);
            return;
        }

        // POST /profile is multipart - its _csrf is a form part this filter can't read
        // before the parts are parsed, so ProfileServlet checks it after parsing.
        boolean multipartProfilePost = "POST".equalsIgnoreCase(req.getMethod()) && path.equals("/profile");

        if (!isSafeMethod(req.getMethod()) && !multipartProfilePost && !Csrf.matches(req)) {
            reject(req, resp, "missing/invalid CSRF token for " + req.getMethod() + " " + path);
            return;
        }

        // login needs no session (but the CSRF check above still ran on its POST)
        if (path.equals("/login")) {
            chain.doFilter(request, response);
            return;
        }

        if (!AuthSession.isAuthenticated(req)) {
            reject(req, resp, "unauthenticated request to " + path);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isSafeMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private void reject(HttpServletRequest req, HttpServletResponse resp, String reason) throws IOException {
        // reason has request text in it - clean it so it can't forge log lines
        log.info("Auth reject, redirecting to /login - {}", Log.clean(reason));
        AuthSession.end(req);
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
