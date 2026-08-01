package com.breastcancer.breastcancerbackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class AppointmentRescheduleRequestDto {

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    private String reason;

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
