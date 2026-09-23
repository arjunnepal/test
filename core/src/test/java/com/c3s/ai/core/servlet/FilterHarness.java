package com.c3s.ai.core.servlet;

import com.c3s.ai.core.auth.AuthSession.Scope;
import com.c3s.ai.core.auth.AuthSession;
import com.c3s.ai.core.user.AuthenticatedUser;
import com.c3s.ai.core.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * One request through AuthFilter, with just enough of the servlet API to answer what the
 * filter asks: path, method, session and the two ways it can refuse (redirect or sendError).
 */
final class FilterHarness {

    private final String method;
    private final String path;
    private final Map<String, Object> sessionAttributes = new HashMap<>();
    private boolean sessionStarted;

    String redirectedTo;
    Integer errorStatus;
    boolean passedThrough;
    final Map<String, String> headers = new HashMap<>();

    private FilterHarness(String method, String path) {
        this.method = method;
        this.path = path;
    }

    static FilterHarness get(String path) {
        return new FilterHarness("GET", path);
    }

    static FilterHarness post(String path) {
        return new FilterHarness("POST", path);
    }

    /** Sign this request in on one side, as if that login page had run. */
    FilterHarness signedIn(Scope scope, String username, Role role) {
        sessionStarted = true;
        AuthSession.begin(request(), scope, new AuthenticatedUser(username, username, role));
        return this;
    }

    Object sessionAttribute(String name) {
        return sessionStarted ? sessionAttributes.get(name) : null;
    }

    void run() throws Exception {
        new AuthFilter().doFilter(request(), response(), chain());
    }

    private HttpServletRequest cachedRequest;

    private HttpServletRequest request() {
        if (cachedRequest == null) {
            cachedRequest = (HttpServletRequest) Proxy.newProxyInstance(
                    FilterHarness.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    new RequestHandler());
        }
        return cachedRequest;
    }

    private final Session session = new Session();

    private final class RequestHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method m, Object[] args) {
            return switch (m.getName()) {
                case "getServletPath" -> path;
                case "getPathInfo" -> null;
                case "getMethod" -> method;
                case "getContextPath" -> "";
                case "getRemoteAddr" -> "203.0.113.7";
                case "getParameter" -> null;   // no CSRF token is ever presented here
                case "changeSessionId" -> "rotated";
                case "getSession" -> {
                    boolean create = args == null || args.length == 0 || (Boolean) args[0];
                    if (create) {
                        sessionStarted = true;
                    }
                    yield sessionStarted ? session : null;
                }
                case "toString" -> "FilterHarness(" + method + " " + path + ")";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(
                        "AuthFilter should not need HttpServletRequest." + m.getName());
            };
        }
    }

    private HttpServletResponse response() {
        return (HttpServletResponse) Proxy.newProxyInstance(
                FilterHarness.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, m, args) -> switch (m.getName()) {
                    case "sendRedirect" -> {
                        redirectedTo = (String) args[0];
                        yield null;
                    }
                    case "sendError" -> {
                        errorStatus = (Integer) args[0];
                        yield null;
                    }
                    case "setHeader" -> {
                        headers.put((String) args[0], (String) args[1]);
                        yield null;
                    }
                    case "toString" -> "FilterHarnessResponse";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(
                            "AuthFilter should not need HttpServletResponse." + m.getName());
                });
    }

    private FilterChain chain() {
        return (req, resp) -> passedThrough = true;
    }

    private final class Session implements HttpSession {
        @Override public Object getAttribute(String name) { return sessionAttributes.get(name); }
        @Override public void setAttribute(String name, Object value) { sessionAttributes.put(name, value); }
        @Override public void removeAttribute(String name) { sessionAttributes.remove(name); }
        @Override public void invalidate() { sessionAttributes.clear(); sessionStarted = false; }
        @Override public String getId() { return "harness"; }
        @Override public Enumeration<String> getAttributeNames() {
            return Collections.enumeration(sessionAttributes.keySet());
        }
        @Override public long getCreationTime() { return 0; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) { }
        @Override public int getMaxInactiveInterval() { return 0; }
        @Override public boolean isNew() { return false; }
    }
}
