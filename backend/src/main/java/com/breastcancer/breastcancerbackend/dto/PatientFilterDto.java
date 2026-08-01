package com.breastcancer.breastcancerbackend.dto;

/**
 * Paramètres de filtrage pour la liste des patients.
 * Inclut les filtres User (keyword, gender, city, country, active)
 * + les filtres spécifiques au PatientProfile.
 */
public class PatientFilterDto {

    // ---- filtres User ----
    private String keyword;              // email, firstName, lastName
    private String city;
    private String country;
    private Boolean active;

    // ---- filtres PatientProfile ----
    private String medicalRecordNumber;
    private Boolean medicalConsent;
    private Boolean hasAssignedDoctor;   // true = a un médecin, false = sans médecin

    // getters / setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public void setMedicalRecordNumber(String medicalRecordNumber) { this.medicalRecordNumber = medicalRecordNumber; }

    public Boolean getMedicalConsent() { return medicalConsent; }
    public void setMedicalConsent(Boolean medicalConsent) { this.medicalConsent = medicalConsent; }

    public Boolean getHasAssignedDoctor() { return hasAssignedDoctor; }
    public void setHasAssignedDoctor(Boolean hasAssignedDoctor) { this.hasAssignedDoctor = hasAssignedDoctor; }
}
