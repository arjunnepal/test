package com.c3s.ai.browse.service;

import com.c3s.ai.browse.web.BrowsePageData;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrowsePageCacheTest {

    private long now;
    private int loads;
    private boolean failNextLoad;

    private BrowsePageCache cache(Duration ttl) {
        return new BrowsePageCache(() -> {
            loads++;
            if (failNextLoad) {
                failNextLoad = false;
                throw new IllegalStateException("db down");
            }
            return new BrowsePageData();
        }, ttl, () -> now);
    }

    @Test
    void reusesThePageUntilTheTtlRunsOut() {
        BrowsePageCache cache = cache(Duration.ofSeconds(60));

        BrowsePageData first = cache.get();
        now = 59_999;
        assertSame(first, cache.get());
        assertEquals(1, loads);

        now = 60_000;
        assertNotSame(first, cache.get());
        assertEquals(2, loads);
    }

    @Test
    void zeroTtlBuildsThePageEveryTime() {
        BrowsePageCache cache = cache(Duration.ZERO);
        cache.get();
        cache.get();
        assertEquals(2, loads);
    }

    @Test
    void aFailedBuildIsNotCached() {
        BrowsePageCache cache = cache(Duration.ofSeconds(60));
        failNextLoad = true;
        assertThrows(IllegalStateException.class, cache::get);

        cache.get();
        assertEquals(2, loads);
    }
}
