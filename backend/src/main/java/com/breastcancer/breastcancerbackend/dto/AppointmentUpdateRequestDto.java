package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Appointment;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class AppointmentUpdateRequestDto {

    private Instant startAt;
    private Instant endAt;

    private Appointment.Mode mode;

    @Size(max = 500)
    private String reason;

    @Size(max = 1200)
    private String patientNotes;

    @Size(max = 1200)
    private String doctorNotes;

    @Size(max = 800)
    private String videoRoomUrl;

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

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    public String getVideoRoomUrl() { return videoRoomUrl; }
    public void setVideoRoomUrl(String videoRoomUrl) { this.videoRoomUrl = videoRoomUrl; }
}
