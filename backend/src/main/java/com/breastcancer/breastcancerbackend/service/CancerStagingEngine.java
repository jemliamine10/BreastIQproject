package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.entity.ClinicalData;
import com.breastcancer.breastcancerbackend.entity.MedicalRecord;
import org.springframework.stereotype.Service;

/**
 * CancerStagingEngine — computes the AJCC TNM-based cancer stage
 * from clinical data (tumor size, lymph node involvement, metastasis).
 *
 * Based on simplified AJCC 8th Edition breast cancer staging.
 */
@Service
public class CancerStagingEngine {

    // ── TNM Category Results ──
    public static class TnmResult {
        private final String t;   // T0, Tis, T1, T2, T3, T4
        private final String n;   // N0, N1, N2, N3
        private final String m;   // M0, M1
        private final MedicalRecord.CancerStage stage;
        private final String classification; // e.g. "T2N1M0"
        private final String stageLabel;     // e.g. "Stade IIA"

        public TnmResult(String t, String n, String m, MedicalRecord.CancerStage stage, String stageLabel) {
            this.t = t;
            this.n = n;
            this.m = m;
            this.stage = stage;
            this.classification = t + n + m;
            this.stageLabel = stageLabel;
        }

        public String getT() { return t; }
        public String getN() { return n; }
        public String getM() { return m; }
        public MedicalRecord.CancerStage getStage() { return stage; }
        public String getClassification() { return classification; }
        public String getStageLabel() { return stageLabel; }
    }

    /**
     * Compute the TNM classification and AJCC stage from clinical data.
     *
     * @param cd the clinical data (may be null)
     * @return a TnmResult, or null if clinical data is insufficient
     */
    public TnmResult compute(ClinicalData cd) {
        if (cd == null) return null;

        // Need at least tumor size to compute
        Double tumorMm = cd.getTumorSize();
        Integer lymphNodes = cd.getLymphNodesInvolved();
        boolean metastasis = cd.isMetastasis();

        // Derive T category
        String tCat = classifyT(tumorMm);

        // Derive N category
        String nCat = classifyN(lymphNodes);

        // Derive M category
        String mCat = metastasis ? "M1" : "M0";

        // Determine overall stage
        MedicalRecord.CancerStage stage = determineStage(tCat, nCat, mCat);
        String label = buildStageLabel(stage, tCat, nCat, mCat);

        return new TnmResult(tCat, nCat, mCat, stage, label);
    }

    // ── T Classification (Tumor Size) ──
    private String classifyT(Double tumorMm) {
        if (tumorMm == null || tumorMm <= 0) return "T0";
        if (tumorMm <= 1)   return "Tis";   // In situ / micro-invasive
        if (tumorMm <= 20)  return "T1";     // ≤ 2 cm
        if (tumorMm <= 50)  return "T2";     // 2-5 cm
        if (tumorMm <= 100) return "T3";     // > 5 cm
        return "T4";                          // Chest wall / skin involvement
    }

    // ── N Classification (Lymph Nodes) ──
    private String classifyN(Integer nodes) {
        if (nodes == null || nodes <= 0) return "N0";
        if (nodes <= 3)  return "N1";   // 1-3 nodes
        if (nodes <= 9)  return "N2";   // 4-9 nodes
        return "N3";                     // ≥ 10 nodes
    }

    // ── Stage Determination (Simplified AJCC 8th Ed.) ──
    private MedicalRecord.CancerStage determineStage(String t, String n, String m) {
        // Any M1 → Stage IV
        if ("M1".equals(m)) {
            return MedicalRecord.CancerStage.STAGE_IV;
        }

        // Tis + N0 → Stage 0
        if ("Tis".equals(t) && "N0".equals(n)) {
            return MedicalRecord.CancerStage.STAGE_0;
        }

        // T0/Tis with nodes → at least Stage II
        if (("T0".equals(t) || "Tis".equals(t)) && !"N0".equals(n)) {
            if ("N3".equals(n)) return MedicalRecord.CancerStage.STAGE_III;
            if ("N2".equals(n)) return MedicalRecord.CancerStage.STAGE_III;
            return MedicalRecord.CancerStage.STAGE_II;
        }

        // T1 + N0 → Stage I
        if ("T1".equals(t) && "N0".equals(n)) {
            return MedicalRecord.CancerStage.STAGE_I;
        }

        // T1 + N1 → Stage IIA → mapped to STAGE_II
        if ("T1".equals(t) && "N1".equals(n)) {
            return MedicalRecord.CancerStage.STAGE_II;
        }

        // T1 + N2/N3 → Stage III
        if ("T1".equals(t) && ("N2".equals(n) || "N3".equals(n))) {
            return MedicalRecord.CancerStage.STAGE_III;
        }

        // T2 + N0 → Stage IIA
        if ("T2".equals(t) && "N0".equals(n)) {
            return MedicalRecord.CancerStage.STAGE_II;
        }

        // T2 + N1 → Stage IIB
        if ("T2".equals(t) && "N1".equals(n)) {
            return MedicalRecord.CancerStage.STAGE_II;
        }

        // T2 + N2/N3 → Stage III
        if ("T2".equals(t) && ("N2".equals(n) || "N3".equals(n))) {
            return MedicalRecord.CancerStage.STAGE_III;
        }

        // T3 + N0 → Stage IIB
        if ("T3".equals(t) && "N0".equals(n)) {
            return MedicalRecord.CancerStage.STAGE_II;
        }

        // T3 + any N → Stage III
        if ("T3".equals(t)) {
            return MedicalRecord.CancerStage.STAGE_III;
        }

        // T4 + any → Stage III
        if ("T4".equals(t)) {
            return MedicalRecord.CancerStage.STAGE_III;
        }

        // Fallback — not enough data, assume Stage I
        return MedicalRecord.CancerStage.STAGE_I;
    }

    // ── Human-readable label ──
    private String buildStageLabel(MedicalRecord.CancerStage stage, String t, String n, String m) {
        String roman;
        switch (stage) {
            case STAGE_0:   roman = "Stade 0"; break;
            case STAGE_I:   roman = "Stade I"; break;
            case STAGE_II:  roman = "Stade II"; break;
            case STAGE_III: roman = "Stade III"; break;
            case STAGE_IV:  roman = "Stade IV"; break;
            default:        roman = "Non déterminé";
        }
        return roman + " — " + t + n + m;
    }
}
