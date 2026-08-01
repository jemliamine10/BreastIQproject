package com.breastcancer.breastcancerbackend.dto;

public class NextAppointmentResponse {
    private PatientAppointmentDto nextAppointment;

    public NextAppointmentResponse(PatientAppointmentDto nextAppointment) {
        this.nextAppointment = nextAppointment;
    }

    public PatientAppointmentDto getNextAppointment() {
        return nextAppointment;
    }
}