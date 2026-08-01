package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.User;

/**
 * Paramètres de filtrage pour la liste des utilisateurs.
 * Tous les champs sont optionnels — seuls les champs non-null sont appliqués.
 */
public class UserFilterDto {

    private String keyword;          // recherche dans email, firstName, lastName
    private User.Role role;
    private User.Gender gender;
    private String city;
    private String country;
    private Boolean active;
    private Boolean emailVerified;

    // getters / setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }

    public User.Gender getGender() { return gender; }
    public void setGender(User.Gender gender) { this.gender = gender; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
}
