package com.breastcancer.breastcancerbackend.dto;

import java.time.Instant;

public class CalendarSlotDto {

    public enum SlotStatus {
        AVAILABLE,
        BOOKED,
        BLOCKED
    }

    private Instant startTime;
    private Instant endTime;
    private SlotStatus status;

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
