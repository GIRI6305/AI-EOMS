package com.aieoms.auth.dto;

public record LoginResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String token
) {
}
