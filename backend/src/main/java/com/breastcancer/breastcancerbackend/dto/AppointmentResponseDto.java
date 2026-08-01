package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Appointment;

import java.time.Instant;
import java.util.UUID;

public class AppointmentResponseDto {

    private UUID id;
    private UUID linkId;

    private UUID patientProfileId;
    private UUID doctorProfileId;
    private UUID rescheduledFromId;

    private Instant startAt;
    private Instant endAt;

    private Appointment.Mode mode;
    private Appointment.Status status;

    private String reason;
    private String patientFirstName;
    private String patientLastName;
    private String patientNotes;
    private String doctorNotes;

    private String videoRoomUrl;

    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLinkId() { return linkId; }
    public void setLinkId(UUID linkId) { this.linkId = linkId; }

    public UUID getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(UUID patientProfileId) { this.patientProfileId = patientProfileId; }

    public UUID getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(UUID doctorProfileId) { this.doctorProfileId = doctorProfileId; }

    public UUID getRescheduledFromId() { return rescheduledFromId; }
    public void setRescheduledFromId(UUID rescheduledFromId) { this.rescheduledFromId = rescheduledFromId; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public Appointment.Mode getMode() { return mode; }
    public void setMode(Appointment.Mode mode) { this.mode = mode; }

    public Appointment.Status getStatus() { return status; }
    public void setStatus(Appointment.Status status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPatientFirstName() { return patientFirstName; }
    public void setPatientFirstName(String patientFirstName) { this.patientFirstName = patientFirstName; }

    public String getPatientLastName() { return patientLastName; }
    public void setPatientLastName(String patientLastName) { this.patientLastName = patientLastName; }

    public String getPatientNotes() { return patientNotes; }
    public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    public String getVideoRoomUrl() { return videoRoomUrl; }
    public void setVideoRoomUrl(String videoRoomUrl) { this.videoRoomUrl = videoRoomUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
