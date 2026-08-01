package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Appointment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class AppointmentCreateRequestDto {

    @NotNull
    private UUID linkId;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    @NotNull
    private Appointment.Mode mode;

    @Size(max = 500)
    private String reason;

    @Size(max = 1200)
    private String patientNotes;

    // ✅ AJOUT
    @Size(max = 2048)
    private String videoRoomUrl;

    public UUID getLinkId() { return linkId; }
    public void setLinkId(UUID linkId) { this.linkId = linkId; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public Appointment.Mode getMode() { return mode; }
    public void setMode(Appointment.Mode mode) { this.mode = mode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPatientNotes() { return patientNotes; }
    public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }

    // ✅ GET/SET
    public String getVideoRoomUrl() { return videoRoomUrl; }
    public void setVideoRoomUrl(String videoRoomUrl) { this.videoRoomUrl = videoRoomUrl; }
}
