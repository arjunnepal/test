package com.c3s.ai.browse.page;

import com.c3s.ai.core.web.AppCaches;
import com.c3s.ai.core.web.VersionedCache;
import jakarta.servlet.ServletContext;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * The giving page, built at most once per TTL and shared by every visitor. The instance lives in
 * the ServletContext and registers with AppCaches, so a dashboard write clears it without the
 * admin module having to know this class exists.
 */
public final class BrowsePageCache {

    private static final String ATTRIBUTE = BrowsePageCache.class.getName();

    private final VersionedCache<BrowsePageData> cache;

    BrowsePageCache(Supplier<BrowsePageData> loader, Duration ttl) {
        this.cache = new VersionedCache<>(loader, ttl);
    }

    /** Create it, publish it on the context and hook it up to app-wide invalidation. */
    static BrowsePageCache install(ServletContext context, Supplier<BrowsePageData> loader, Duration ttl) {
        BrowsePageCache pages = new BrowsePageCache(loader, ttl);
        context.setAttribute(ATTRIBUTE, pages);
        AppCaches.register(context, pages::invalidate);
        return pages;
    }

    public BrowsePageData get() {
        return cache.get();
    }

    public void invalidate() {
        cache.invalidate();
    }
}
