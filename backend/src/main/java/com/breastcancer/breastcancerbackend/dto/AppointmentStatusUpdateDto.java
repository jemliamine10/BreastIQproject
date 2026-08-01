package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Appointment;
import jakarta.validation.constraints.NotNull;

public class AppointmentStatusUpdateDto {

    @NotNull
    private Appointment.Status status;

    public Appointment.Status getStatus() { return status; }
    public void setStatus(Appointment.Status status) { this.status = status; }
}
