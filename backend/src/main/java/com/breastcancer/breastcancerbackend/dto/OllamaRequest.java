package com.breastcancer.breastcancerbackend.dto;

import java.util.ArrayList;
import java.util.List;

public class OllamaRequest {

    private String model;
    private List<OllamaMessage> messages = new ArrayList<>();
    private boolean stream = false;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<OllamaMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OllamaMessage> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}