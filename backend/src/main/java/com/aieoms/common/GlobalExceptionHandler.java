package com.aieoms.common;

import com.aieoms.security.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> rateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 429,
                        "error", "Too Many Requests",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", ex.getMessage()
                ));
    }
    @ExceptionHandler(SecurityException.class)
public ResponseEntity<?> securityException(SecurityException ex) {

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                    "timestamp", Instant.now(),
                    "status", 403,
                    "error", "Forbidden",
                    "message", ex.getMessage()
            ));
}


    /*
     * Deliberately does NOT return exception class names, root cause class
     * names, or raw exception messages to the client. Those can leak
     * internal details (SQL fragments, internal package names, library
     * versions, stack info) to anyone who can trigger an unhandled error.
     *
     * The full exception is logged server-side with a correlation ID so it
     * can still be diagnosed from Render logs, while the client only gets
     * a generic message plus that ID to report back if needed.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> general(Exception ex) {

        String errorId = UUID.randomUUID().toString();

        log.error("Unhandled exception [errorId={}]", errorId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "Something went wrong. Please try again.",
                        "errorId", errorId
                ));
    }
}

