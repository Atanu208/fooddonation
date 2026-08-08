package com.app.fooddonation.dto;

import com.app.fooddonation.model.User;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserDto user) {

    public static AuthResponse of(String token, long expiresInSeconds, User user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, UserDto.from(user));
    }
}
