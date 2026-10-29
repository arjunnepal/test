package com.c3s.ai.admin.service;

import com.c3s.ai.admin.dao.JdbcProfileDao;
import com.c3s.ai.admin.dao.ProfileDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * App-wide cache of the sidebar's published-profile count and raised total.
 * ProfileService calls invalidate() on any profile change; header.jspf reads INSTANCE.
 * Single-JVM - a direct DB edit isn't picked up until the next app-side change.
 */
public final class SidebarStatsCache {

    private static final Logger log = LoggerFactory.getLogger(SidebarStatsCache.class);

    public static final SidebarStatsCache INSTANCE = new SidebarStatsCache(new JdbcProfileDao());

    private final ProfileDao profileDao;

    // null = not loaded or invalidated
    private volatile ProfileDao.SidebarStats cached;

    public SidebarStatsCache(ProfileDao profileDao) {
        this.profileDao = profileDao;
    }

    public long publishedProfiles() {
        return current().publishedProfiles();
    }

    public long raisedTotal() {
        return current().totalRaised();
    }

    public void invalidate() {
        cached = null;
    }

    private ProfileDao.SidebarStats current() {
        ProfileDao.SidebarStats snapshot = cached;
        return snapshot != null ? snapshot : load();
    }

    private synchronized ProfileDao.SidebarStats load() {
        if (cached != null) {
            return cached;
        }
        try {
            ProfileDao.SidebarStats stats = profileDao.getSidebarStats();
            cached = stats;
            return stats;
        } catch (RuntimeException e) {
            // not worth failing the page over - show zeros, try again next read
            log.warn("Failed to load sidebar stats; showing zeros (will retry)", e);
            return new ProfileDao.SidebarStats(0, 0);
        }
    }
}
