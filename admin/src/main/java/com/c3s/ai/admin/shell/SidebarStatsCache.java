package com.c3s.ai.admin.shell;

import com.c3s.ai.admin.profile.ProfileService;
import com.c3s.ai.admin.profile.SidebarStats;
import com.c3s.ai.core.web.VersionedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sidebar's published count and raised total, held until a write clears it through AppCaches.
 * The query itself stays behind ProfileService, like every other read of uwnefl_profiles.
 */
public final class SidebarStatsCache {

    private static final Logger log = LoggerFactory.getLogger(SidebarStatsCache.class);

    public static final SidebarStatsCache INSTANCE = new SidebarStatsCache(new ProfileService()::sidebarStats);

    private final VersionedCache<SidebarStats> cache;

    SidebarStatsCache(java.util.function.Supplier<SidebarStats> loader) {
        this.cache = new VersionedCache<>(loader, null);
    }

    /** Zeros rather than an error page if the query fails - the numbers aren't worth losing a page over. */
    public SidebarStats get() {
        try {
            return cache.get();
        } catch (RuntimeException e) {
            log.warn("Failed to load sidebar stats; showing zeros (will retry)", e);
            return SidebarStats.EMPTY;
        }
    }

    public void invalidate() {
        cache.invalidate();
    }
}
