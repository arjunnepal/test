package com.c3s.ai.core.web;

import java.time.Duration;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * One cached value with an optional time-to-live. A load started before invalidate() still returns
 * to its own caller but is not stored, so the next caller reloads.
 */
public final class VersionedCache<T> {

    private final Supplier<T> loader;
    private final long ttlMillis;
    private final LongSupplier clock;

    private volatile Entry<T> entry;
    private long version;   // guarded by this

    private record Entry<T>(T value, long loadedAt) {
    }

    /** ttl null = keep until invalidated; zero or negative = never cache. */
    public VersionedCache(Supplier<T> loader, Duration ttl) {
        this(loader, ttl, System::currentTimeMillis);
    }

    public VersionedCache(Supplier<T> loader, Duration ttl, LongSupplier clock) {
        this.loader = loader;
        this.ttlMillis = ttl == null ? Long.MAX_VALUE : ttl.toMillis();
        this.clock = clock;
    }

    public T get() {
        if (ttlMillis <= 0) {
            return loader.get();
        }
        Entry<T> current = entry;
        if (isFresh(current)) {
            return current.value();
        }
        long startedAt;
        synchronized (this) {
            current = entry;
            if (isFresh(current)) {
                return current.value();
            }
            startedAt = version;
        }
        // load outside the lock, so a slow database doesn't make every request queue behind it
        T fresh = loader.get();
        synchronized (this) {
            if (version == startedAt) {
                entry = new Entry<>(fresh, clock.getAsLong());
            }
        }
        return fresh;
    }

    public void invalidate() {
        synchronized (this) {
            version++;
            entry = null;
        }
    }

    private boolean isFresh(Entry<T> e) {
        return e != null && (ttlMillis == Long.MAX_VALUE || clock.getAsLong() - e.loadedAt() < ttlMillis);
    }
}
