package com.breastcancer.breastcancerbackend.dto;

import java.util.UUID;

public class AppointmentDoctorDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String specialty;
    private String contact;
    private String structure;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getStructure() { return structure; }
    public void setStructure(String structure) { this.structure = structure; }
}
