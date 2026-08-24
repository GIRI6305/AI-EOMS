package com.aieoms.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal in-memory rate limiter for login attempts, keyed by username.
 *
 * Deliberately dependency-free (no Redis, no Bucket4j) so it doesn't add
 * any new library to the build. Good enough for a single-instance
 * deployment like this one; if this app is ever scaled to multiple
 * backend instances behind a load balancer, this in-memory state would
 * need to move to a shared store (Redis) since each instance would
 * otherwise track attempts independently.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private static final class Attempts {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername =
            new ConcurrentHashMap<>();

    /**
     * Call before attempting authentication. Throws if the caller has
     * exceeded MAX_ATTEMPTS within the current window.
     */
    public void checkAllowed(String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        Attempts attempts =
                attemptsByUsername.computeIfAbsent(
                        username.toLowerCase(),
                        key -> new Attempts()
                );

        long now = System.currentTimeMillis();

        if (now - attempts.windowStart > WINDOW_MILLIS) {
            attempts.count.set(0);
            attempts.windowStart = now;
        }

        if (attempts.count.get() >= MAX_ATTEMPTS) {
            long secondsLeft =
                    (WINDOW_MILLIS - (now - attempts.windowStart)) / 1000;

            throw new RateLimitExceededException(
                    "Too many login attempts. Try again in "
                            + Math.max(secondsLeft, 1)
                            + " seconds."
            );
        }
    }

    /** Call after a failed login attempt. */
    public void recordFailure(String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        Attempts attempts =
                attemptsByUsername.computeIfAbsent(
                        username.toLowerCase(),
                        key -> new Attempts()
                );

        attempts.count.incrementAndGet();
    }

    /** Call after a successful login to reset the counter. */
    public void recordSuccess(String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        attemptsByUsername.remove(username.toLowerCase());
    }
}
