package com.c3s.ai.browse.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// In-memory login throttles: 5 failures for a username, or 20 from one client address, within
// 15 minutes lock it for 15 minutes. Counts are per JVM.
public final class LoginThrottle {

    static final int USERNAME_MAX_FAILURES = 5;
    static final int CLIENT_MAX_FAILURES = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000;

    public static final LoginThrottle BY_USERNAME = new LoginThrottle(USERNAME_MAX_FAILURES);
    public static final LoginThrottle BY_CLIENT = new LoginThrottle(CLIENT_MAX_FAILURES);

    private final int maxFailures;
    private final ConcurrentHashMap<String, State> byKey = new ConcurrentHashMap<>();

    // tests create their own instances
    LoginThrottle(int maxFailures) {
        this.maxFailures = maxFailures;
    }

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

    public boolean isLocked(String key) {
        State s = byKey.get(normalize(key));
        return s != null && s.lockedUntil != null && Instant.now().isBefore(s.lockedUntil);
    }

    // keeps the failures still inside the window plus this one, and locks once there are too many
    public void recordFailure(String key) {
        Instant now = Instant.now();
        byKey.compute(normalize(key), (ignored, current) -> {
            List<Instant> recent = new ArrayList<>(maxFailures);
            if (current != null) {
                for (Instant t : current.failures) {
                    if (now.isBefore(t.plus(WINDOW))) {
                        recent.add(t);
                    }
                }
            }
            recent.add(now);
            Instant lockedUntil = recent.size() >= maxFailures
                    ? now.plus(LOCKOUT)
                    : (current != null ? current.lockedUntil : null);
            return new State(List.copyOf(recent), lockedUntil);
        });
        sweep(now);
    }

    public void reset(String key) {
        byKey.remove(normalize(key));
    }

    // drop expired entries and keep the map under the cap
    private void sweep(Instant now) {
        byKey.values().removeIf(s -> s.isExpired(now));
        int over = byKey.size() - MAX_TRACKED_KEYS;
        if (over <= 0) {
            return;
        }
        byKey.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().lastActivity()))
                .limit(over)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(byKey::remove);
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
