package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.DoctorProfile;

import java.math.BigDecimal;

/**
 * Paramètres de filtrage pour la liste des médecins.
 * Inclut les filtres User (keyword, gender, city, country, active)
 * + les filtres spécifiques au DoctorProfile.
 */
public class DoctorFilterDto {

    // ---- filtres User ----
    private String keyword;              // email, firstName, lastName
    private String city;
    private String country;
    private Boolean active;

    // ---- filtres DoctorProfile ----
    private DoctorProfile.DoctorType doctorType;
    private String speciality;           // contient (LIKE)
    private DoctorProfile.ConsultationMode consultationMode;
    private Boolean verified;
    private String clinicName;           // contient (LIKE)
    private Integer minYearsOfExperience;
    private BigDecimal maxConsultationFee;
    private String language;             // le champ languages contient cette sous‑chaîne

    // getters / setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public DoctorProfile.DoctorType getDoctorType() { return doctorType; }
    public void setDoctorType(DoctorProfile.DoctorType doctorType) { this.doctorType = doctorType; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public DoctorProfile.ConsultationMode getConsultationMode() { return consultationMode; }
    public void setConsultationMode(DoctorProfile.ConsultationMode consultationMode) { this.consultationMode = consultationMode; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public Integer getMinYearsOfExperience() { return minYearsOfExperience; }
    public void setMinYearsOfExperience(Integer minYearsOfExperience) { this.minYearsOfExperience = minYearsOfExperience; }

    public BigDecimal getMaxConsultationFee() { return maxConsultationFee; }
    public void setMaxConsultationFee(BigDecimal maxConsultationFee) { this.maxConsultationFee = maxConsultationFee; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
