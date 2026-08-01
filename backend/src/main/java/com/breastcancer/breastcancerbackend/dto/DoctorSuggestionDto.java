package com.breastcancer.breastcancerbackend.dto;

import java.util.UUID;

public class DoctorSuggestionDto {

    private UUID doctorProfileId;
    private UUID userId;
    private String fullName;
    private String speciality;
    private String imageUrl;
    private String consultationMode;
    private boolean availableToday;

    public UUID getDoctorProfileId() {
        return doctorProfileId;
    }

    public void setDoctorProfileId(UUID doctorProfileId) {
        this.doctorProfileId = doctorProfileId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getConsultationMode() {
        return consultationMode;
    }

    public void setConsultationMode(String consultationMode) {
        this.consultationMode = consultationMode;
    }

    public boolean isAvailableToday() {
        return availableToday;
    }

    public void setAvailableToday(boolean availableToday) {
        this.availableToday = availableToday;
    }
}