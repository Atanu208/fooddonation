package com.app.fooddonation.dto;

import com.app.fooddonation.model.User;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String name,
        String email,
        String role,
        String phoneNumber,
        String address,
        String city,
        String state,
        String pincode,
        String organizationName,
        boolean active,
        LocalDateTime createdAt) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getPhoneNumber(),
                user.getAddress(),
                user.getCity(),
                user.getState(),
                user.getPincode(),
                user.getOrganizationName(),
                user.isActive(),
                user.getCreatedAt());
    }
}
