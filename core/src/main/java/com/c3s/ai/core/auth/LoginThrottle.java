package com.c3s.ai.core.auth;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sign-in throttles, per JVM: 5 attempts for one account or 20 from one client address
 * within 15 minutes lock it for 15 minutes. An attempt is counted before the password is checked,
 * so parallel requests can't slip past the limit while BCrypt runs. Keys are folded the way
 * MySQL's ai_ci collation matches usernames, so "ádmin" and "ADMIN" share admin's bucket.
 */
public final class LoginThrottle {

    public static final int ACCOUNT_MAX_ATTEMPTS = 5;
    public static final int CLIENT_MAX_ATTEMPTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000;

    public static final LoginThrottle BY_ACCOUNT = new LoginThrottle(ACCOUNT_MAX_ATTEMPTS);
    public static final LoginThrottle BY_CLIENT = new LoginThrottle(CLIENT_MAX_ATTEMPTS);

    private final int maxAttempts;
    private final ConcurrentHashMap<String, State> byKey = new ConcurrentHashMap<>();

    public LoginThrottle(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    private record State(List<Instant> attempts, Instant lockedUntil) {

        boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isExpired(Instant now) {
            boolean noRecentAttempts = attempts.isEmpty()
                    || !now.isBefore(attempts.get(attempts.size() - 1).plus(WINDOW));
            return noRecentAttempts && !isLocked(now);
        }

        Instant lastActivity() {
            Instant last = attempts.isEmpty() ? Instant.EPOCH : attempts.get(attempts.size() - 1);
            return lockedUntil != null && lockedUntil.isAfter(last) ? lockedUntil : last;
        }
    }

    /** Count one attempt. False when already locked; the attempt that reaches the limit locks the key. */
    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        boolean[] allowed = {false};
        byKey.compute(normalize(key), (ignored, current) -> {
            if (current != null && current.isLocked(now)) {
                return current;
            }
            List<Instant> recent = recentAttempts(current, now);
            recent.add(now);
            allowed[0] = true;
            Instant lockedUntil = recent.size() >= maxAttempts ? now.plus(LOCKOUT) : null;
            return new State(List.copyOf(recent), lockedUntil);
        });
        sweep(now);
        return allowed[0];
    }

    /** Give back the most recent attempt (a successful sign-in shouldn't count against its client). */
    public void release(String key) {
        Instant now = Instant.now();
        byKey.computeIfPresent(normalize(key), (ignored, current) -> {
            List<Instant> recent = recentAttempts(current, now);
            if (!recent.isEmpty()) {
                recent.remove(recent.size() - 1);
            }
            Instant lockedUntil = recent.size() >= maxAttempts ? current.lockedUntil : null;
            return recent.isEmpty() && lockedUntil == null ? null : new State(List.copyOf(recent), lockedUntil);
        });
    }

    public boolean isLocked(String key) {
        State s = byKey.get(normalize(key));
        return s != null && s.isLocked(Instant.now());
    }

    public void reset(String key) {
        byKey.remove(normalize(key));
    }

    private List<Instant> recentAttempts(State current, Instant now) {
        List<Instant> recent = new ArrayList<>(maxAttempts);
        if (current != null) {
            for (Instant t : current.attempts) {
                if (now.isBefore(t.plus(WINDOW))) {
                    recent.add(t);
                }
            }
        }
        return recent;
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

    /** Trimmed, compatibility-decomposed, accents stripped, lower-cased. */
    static String normalize(String key) {
        if (key == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(key.trim(), Normalizer.Form.NFKD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
