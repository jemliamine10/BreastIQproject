package com.breastcancer.breastcancerbackend.dto;

public class AppointmentAvailabilityResponseDto {

    private boolean available;
    private String message;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
