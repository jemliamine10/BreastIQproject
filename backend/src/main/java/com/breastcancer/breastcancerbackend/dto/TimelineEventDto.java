package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Appointment;

import java.time.Instant;

public class TimelineEventDto {

    public enum TimelineStatus {
        COMPLETED,
        ACTIVE,
        UPCOMING
    }

    private Instant date;
    private Appointment.AppointmentType type;
    private String label;
    private String description;
    private TimelineStatus status;

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public Appointment.AppointmentType getType() { return type; }
    public void setType(Appointment.AppointmentType type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TimelineStatus getStatus() { return status; }
    public void setStatus(TimelineStatus status) { this.status = status; }
}
