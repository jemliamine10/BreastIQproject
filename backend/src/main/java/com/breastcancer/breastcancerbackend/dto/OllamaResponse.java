package com.breastcancer.breastcancerbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {

    private OllamaMessage message;

    public OllamaMessage getMessage() {
        return message;
    }

    public void setMessage(OllamaMessage message) {
        this.message = message;
    }
}