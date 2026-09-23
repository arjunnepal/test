package com.c3s.ai.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Just enough HttpServletRequest/HttpSession to drive AuthSession in a unit test:
 * attributes, session creation, invalidate() and changeSessionId(). Both interfaces are
 * far too wide to implement by hand, so the request is a dynamic proxy that answers the
 * four methods AuthSession actually uses and refuses everything else.
 */
final class StubSessionRequest {

    private Session session;
    private int sessionIdCounter;
    final HttpServletRequest request;

    StubSessionRequest() {
        this.request = (HttpServletRequest) Proxy.newProxyInstance(
                StubSessionRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new Handler());
    }

    /** Null until something creates one; replaced wholesale by invalidate(). */
    Session session() {
        return session;
    }

    String sessionId() {
        return session == null ? null : session.id;
    }

    boolean sessionInvalidated() {
        return session == null || session.invalidated;
    }

    private final class Handler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getSession":
                    boolean create = args == null || args.length == 0 || (Boolean) args[0];
                    if (session == null && create) {
                        session = new Session("id-" + (++sessionIdCounter));
                    }
                    return session;
                case "changeSessionId":
                    if (session == null) {
                        throw new IllegalStateException("no session to rotate");
                    }
                    session.id = "id-" + (++sessionIdCounter);
                    return session.id;
                case "toString":
                    return "StubRequest";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    throw new UnsupportedOperationException(
                            "AuthSession should not need HttpServletRequest." + method.getName());
            }
        }
    }

    /** Attribute map plus the one bit of lifecycle AuthSession cares about. */
    final class Session implements HttpSession {

        private final Map<String, Object> attributes = new HashMap<>();
        private String id;
        private boolean invalidated;

        Session(String id) {
            this.id = id;
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        @Override
        public void invalidate() {
            invalidated = true;
            attributes.clear();
            // the container hands out a fresh session next time
            StubSessionRequest.this.session = null;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        @Override public long getCreationTime() { return 0; }
        @Override public long getLastAccessedTime() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) { }
        @Override public int getMaxInactiveInterval() { return 0; }
        @Override public boolean isNew() { return false; }
    }
}
