package com.c3s.ai.admin.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login throttle, keyed by username. 5 misses in a rolling 15-min window
 * lock the username for 15 min. Single-JVM (so it's per-node behind a load balancer,
 * and a real user can be griefed into a lockout). The map is size-capped so a spray
 * of junk usernames can't blow the heap.
 */
public final class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCKOUT = Duration.ofMinutes(15);
    static final int MAX_TRACKED_USERS = 10_000;

    private static final ConcurrentHashMap<String, State> BY_USER = new ConcurrentHashMap<>();

    private LoginThrottle() {
    }

    /** Failure timestamps still inside WINDOW, plus the current lockout expiry if any. */
    private record State(List<Instant> failures, Instant lockedUntil) {

        boolean isExpired(Instant now) {
            boolean noRecentFailures = failures.isEmpty()
                    || now.isAfter(failures.get(failures.size() - 1).plus(WINDOW));
            boolean notLocked = lockedUntil == null || now.isAfter(lockedUntil);
            return noRecentFailures && notLocked;
        }

        Instant lastActivity() {
            Instant last = failures.isEmpty() ? Instant.EPOCH : failures.get(failures.size() - 1);
            return lockedUntil != null && lockedUntil.isAfter(last) ? lockedUntil : last;
        }
    }

    public static boolean isLocked(String username) {
        State s = BY_USER.get(key(username));
        return s != null && s.lockedUntil != null && Instant.now().isBefore(s.lockedUntil);
    }

    public static void recordFailure(String username) {
        Instant now = Instant.now();
        BY_USER.compute(key(username), (ignored, current) -> {
            List<Instant> recent = new ArrayList<>(MAX_FAILURES);
            if (current != null) {
                for (Instant t : current.failures) {
                    if (now.isBefore(t.plus(WINDOW))) {
                        recent.add(t);
                    }
                }
            }
            recent.add(now);
            Instant lockedUntil = recent.size() >= MAX_FAILURES
                    ? now.plus(LOCKOUT)
                    : (current != null ? current.lockedUntil : null);
            return new State(List.copyOf(recent), lockedUntil);
        });
        sweep(now);
    }

    public static void reset(String username) {
        BY_USER.remove(key(username));
    }

    /** Runs on every failure: drop expired entries, then evict least-recently-active if still over cap. */
    private static void sweep(Instant now) {
        BY_USER.values().removeIf(s -> s.isExpired(now));
        int over = BY_USER.size() - MAX_TRACKED_USERS;
        if (over <= 0) {
            return;
        }
        BY_USER.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().lastActivity()))
                .limit(over)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(BY_USER::remove);
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
