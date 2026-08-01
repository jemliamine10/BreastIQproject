package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.entity.User;
import com.breastcancer.breastcancerbackend.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // ✅ GET /api/registration/email-available?email=...
    @GetMapping("/email-available")
    public boolean checkEmailAvailable(@RequestParam String email) {
        return registrationService.checkEmailAvailable(email);
    }

    // ✅ GET /api/registration/doctor-types
    @GetMapping("/doctor-types")
    public List<DoctorProfile.DoctorType> listDoctorTypes() {
        return registrationService.listDoctorTypes();
    }

    // ✅ GET /api/registration/genders
    @GetMapping("/genders")
    public List<User.Gender> listGenders() {
        return registrationService.listGenders();
    }

    // ✅ POST /api/registration/doctor
    @PostMapping("/doctor")
    public DoctorProfileResponseDto registerDoctor(@Valid @RequestBody RegisterDoctorRequest req) {
        return registrationService.registerDoctor(req.getUser(), req.getDoctor());
    }

    // ✅ POST /api/registration/patient
    @PostMapping("/patient")
    public PatientProfileResponseDto registerPatient(@Valid @RequestBody RegisterPatientRequest req) {
        return registrationService.registerPatient(req.getUser(), req.getPatient());
    }

    // ====== Request wrappers (pour envoyer user+profile dans un seul JSON) ======
    public static class RegisterDoctorRequest {
        @NotNull @Valid private UserCreateRequestDto user;
        @NotNull @Valid private DoctorProfileCreateRequestDto doctor;

        public UserCreateRequestDto getUser() { return user; }
        public void setUser(UserCreateRequestDto user) { this.user = user; }

        public DoctorProfileCreateRequestDto getDoctor() { return doctor; }
        public void setDoctor(DoctorProfileCreateRequestDto doctor) { this.doctor = doctor; }
    }

    public static class RegisterPatientRequest {
        @NotNull @Valid private UserCreateRequestDto user;
        @NotNull @Valid private PatientProfileCreateRequestDto patient;

        public UserCreateRequestDto getUser() { return user; }
        public void setUser(UserCreateRequestDto user) { this.user = user; }

        public PatientProfileCreateRequestDto getPatient() { return patient; }
        public void setPatient(PatientProfileCreateRequestDto patient) { this.patient = patient; }
    }
}
