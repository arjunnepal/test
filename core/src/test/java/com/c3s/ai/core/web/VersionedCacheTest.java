package com.c3s.ai.core.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionedCacheTest {

    private long now;
    private final AtomicInteger loads = new AtomicInteger();

    private VersionedCache<Integer> cache(Duration ttl) {
        return new VersionedCache<>(loads::incrementAndGet, ttl, () -> now);
    }

    @Test
    void reusesTheValueUntilTheTtlRunsOut() {
        VersionedCache<Integer> cache = cache(Duration.ofSeconds(60));
        assertEquals(1, cache.get());
        now = 59_999;
        assertEquals(1, cache.get());
        now = 60_000;
        assertEquals(2, cache.get());
    }

    @Test
    void noTtlKeepsTheValueUntilInvalidated() {
        VersionedCache<Integer> cache = cache(null);
        cache.get();
        now = Long.MAX_VALUE / 2;
        assertEquals(1, cache.get());
        cache.invalidate();
        assertEquals(2, cache.get());
    }

    @Test
    void zeroTtlLoadsEveryTime() {
        VersionedCache<Integer> cache = cache(Duration.ZERO);
        cache.get();
        cache.get();
        assertEquals(2, loads.get());
    }

    @Test
    void aFailedLoadIsNotCached() {
        AtomicInteger calls = new AtomicInteger();
        VersionedCache<Integer> cache = new VersionedCache<>(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("db down");
            }
            return calls.get();
        }, Duration.ofSeconds(60), () -> now);
        assertThrows(IllegalStateException.class, cache::get);
        assertEquals(2, cache.get());
    }

    // The race the old SidebarStatsCache had: a load reads the old data, a write commits and
    // invalidates, then the load finishes. The stale value must not be stored.
    @Test
    void aLoadStartedBeforeInvalidateIsNotStored() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger dbValue = new AtomicInteger(1);
        VersionedCache<Integer> cache = new VersionedCache<>(() -> {
            int read = dbValue.get();
            loading.countDown();
            await(release);
            return read;
        }, null, () -> now);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> slowLoad = pool.submit(cache::get);
            loading.await(5, TimeUnit.SECONDS);

            dbValue.set(2);        // the write commits...
            cache.invalidate();    // ...and invalidates while the old load is still running
            release.countDown();

            assertEquals(1, slowLoad.get(5, TimeUnit.SECONDS));   // its own caller gets what it read
            assertEquals(2, cache.get());                          // but it wasn't kept
        } finally {
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
