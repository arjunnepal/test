package com.c3s.ai.core.web;

import jakarta.servlet.ServletContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * App-wide cache invalidation without module dependencies: a cache registers how to clear itself,
 * and a write anywhere calls invalidateAll.
 */
public final class AppCaches {

    private static final String ATTRIBUTE = AppCaches.class.getName();

    private AppCaches() {
    }

    public static void register(ServletContext context, Runnable invalidate) {
        invalidators(context).add(invalidate);
    }

    public static void invalidateAll(ServletContext context) {
        for (Runnable r : invalidators(context)) {
            r.run();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Runnable> invalidators(ServletContext context) {
        synchronized (context) {
            Object existing = context.getAttribute(ATTRIBUTE);
            if (existing == null) {
                existing = new CopyOnWriteArrayList<Runnable>();
                context.setAttribute(ATTRIBUTE, existing);
            }
            return (List<Runnable>) existing;
        }
    }
}
