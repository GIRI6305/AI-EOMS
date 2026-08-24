package com.aieoms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "AI-EOMS-Test-Secret-Key-2026-Must-Be-At-Least-32-Characters";

    private static final long EXPIRATION =
            60_000L;

    private JwtService service() {
        return new JwtService(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_shouldCreateValidToken() {

        JwtService jwtService = service();

        String token =
                jwtService.generateToken("testuser");

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(
                jwtService.isTokenValid(token, "testuser")
        );
    }

    @Test
    void extractUsername_shouldReturnTokenSubject() {

        JwtService jwtService = service();

        String token =
                jwtService.generateToken("testuser");

        assertEquals(
                "testuser",
                jwtService.extractUsername(token)
        );
    }

    @Test
    void isTokenValid_shouldReturnFalseForWrongUsername() {

        JwtService jwtService = service();

        String token =
                jwtService.generateToken("testuser");

        assertFalse(
                jwtService.isTokenValid(token, "differentuser")
        );
    }

    @Test
    void isTokenValid_shouldReturnFalseForInvalidToken() {

        JwtService jwtService = service();

        assertFalse(
                jwtService.isTokenValid(
                        "invalid.jwt.token",
                        "testuser"
                )
        );
    }
}
