package com.c3s.ai.browse.service;

import com.c3s.ai.browse.web.BrowsePageData;

import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

// Caches the browse page data for a short time. Browse never writes, so a TTL is enough.
public final class BrowsePageCache {

    private final Supplier<BrowsePageData> loader;
    private final long ttlMillis;
    private final LongSupplier clock;

    private volatile Entry entry;

    private record Entry(BrowsePageData data, long loadedAt) {
    }

    public BrowsePageCache(Supplier<BrowsePageData> loader, Duration ttl) {
        this(loader, ttl, System::currentTimeMillis);
    }

    BrowsePageCache(Supplier<BrowsePageData> loader, Duration ttl, LongSupplier clock) {
        this.loader = loader;
        this.ttlMillis = ttl.toMillis();
        this.clock = clock;
    }

    public BrowsePageData get() {
        if (ttlMillis <= 0) {
            return loader.get();
        }
        Entry current = entry;
        if (isFresh(current)) {
            return current.data();
        }
        synchronized (this) {
            // another request may have rebuilt it while this one waited for the lock
            current = entry;
            if (isFresh(current)) {
                return current.data();
            }
            BrowsePageData fresh = loader.get();
            entry = new Entry(fresh, clock.getAsLong());
            return fresh;
        }
    }

    private boolean isFresh(Entry e) {
        return e != null && clock.getAsLong() - e.loadedAt() < ttlMillis;
    }
}
