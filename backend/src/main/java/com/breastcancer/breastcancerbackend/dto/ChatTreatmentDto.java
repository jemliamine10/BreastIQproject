package com.breastcancer.breastcancerbackend.dto;

public class ChatTreatmentDto {

    private String id;
    private String treatmentType;
    private String protocol;
    private String status;
    private Integer currentCycle;
    private Integer totalCycles;
    private String startDate;
    private String endDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTreatmentType() { return treatmentType; }
    public void setTreatmentType(String treatmentType) { this.treatmentType = treatmentType; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCurrentCycle() { return currentCycle; }
    public void setCurrentCycle(Integer currentCycle) { this.currentCycle = currentCycle; }

    public Integer getTotalCycles() { return totalCycles; }
    public void setTotalCycles(Integer totalCycles) { this.totalCycles = totalCycles; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
