package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.User;

import java.util.UUID;

public class LoginResponseDto {

    private UUID userId;
    private String email;
    private User.Role role;
    private String firstName;
    private String lastName;
    private String message;
    private String profilePhotoUrl;

    // — convenience factory —
    public static LoginResponseDto of(User user) {
        LoginResponseDto dto = new LoginResponseDto();
        dto.setUserId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setProfilePhotoUrl(user.getProfilePhotoUrl());
        dto.setMessage("Connexion réussie.");
        return dto;
    }

    // getters / setters
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
