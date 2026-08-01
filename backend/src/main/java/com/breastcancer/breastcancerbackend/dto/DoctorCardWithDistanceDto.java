package com.breastcancer.breastcancerbackend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class DoctorCardWithDistanceDto {

    private UUID doctorProfileId;
    private UUID userId;

    private String fullName; // concat côté service
    private String speciality;
    private String clinicName;

    private Double latitude;
    private Double longitude;

    private BigDecimal consultationFee;

    private Double distanceKm; // calculé côté service/front

    // getters/setters
    public UUID getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(UUID doctorProfileId) { this.doctorProfileId = doctorProfileId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}
