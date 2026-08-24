package com.aieoms.security;

/**
 * Thrown when a caller exceeds LoginRateLimiter's allowed attempts.
 * Mapped to HTTP 429 by GlobalExceptionHandler.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
