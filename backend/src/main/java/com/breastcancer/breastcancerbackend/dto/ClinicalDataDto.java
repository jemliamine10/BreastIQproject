package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.ClinicalData;
import java.util.UUID;

public class ClinicalDataDto {

    private UUID id;
    private ClinicalData.ReceptorStatus estrogenReceptor;
    private ClinicalData.ReceptorStatus progesteroneReceptor;
    private ClinicalData.ReceptorStatus her2Status;
    private Double ki67;
    private Double tumorSize;
    private Integer lymphNodesInvolved;
    private boolean metastasis;
    private Integer grade;
    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ClinicalData.ReceptorStatus getEstrogenReceptor() { return estrogenReceptor; }
    public void setEstrogenReceptor(ClinicalData.ReceptorStatus er) { this.estrogenReceptor = er; }

    public ClinicalData.ReceptorStatus getProgesteroneReceptor() { return progesteroneReceptor; }
    public void setProgesteroneReceptor(ClinicalData.ReceptorStatus pr) { this.progesteroneReceptor = pr; }

    public ClinicalData.ReceptorStatus getHer2Status() { return her2Status; }
    public void setHer2Status(ClinicalData.ReceptorStatus her2) { this.her2Status = her2; }

    public Double getKi67() { return ki67; }
    public void setKi67(Double ki67) { this.ki67 = ki67; }

    public Double getTumorSize() { return tumorSize; }
    public void setTumorSize(Double tumorSize) { this.tumorSize = tumorSize; }

    public Integer getLymphNodesInvolved() { return lymphNodesInvolved; }
    public void setLymphNodesInvolved(Integer n) { this.lymphNodesInvolved = n; }

    public boolean isMetastasis() { return metastasis; }
    public void setMetastasis(boolean metastasis) { this.metastasis = metastasis; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
