package com.aieoms.auth.dto;

public record AuthResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
