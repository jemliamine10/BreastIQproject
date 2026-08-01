import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MammogramPrediction {
  image: string;
  crop: string;
  label: string;           // 'mass' | 'calc'
  classification: string;  // 'Benign' | 'Malignant'
  score: number;
  features: {
    morphology?: {
      area_mm2?: number;
      perimeter_mm?: number;
      circularity?: number;
      eccentricity?: number;
    };
    intensity?: {
      mean?: number;
      std_dev?: number;
    };
    texture?: {
      glcm_homogeneity?: number;
    };
  };
}

export interface MammogramAnalysisResponse {
  detections: boolean;
  fullImage: string | null;
  fullNormalImage: string | null;
  segmentationImage: string | null;
  individualPredictions: MammogramPrediction[];
  globalConfidence: number;
  globalVerdict: string;
  analysisId: string;       // ID of the persisted analysis
  patientFirstName?: string;
  patientLastName?: string;
}

export interface MammogramAnalysisHistory {
  id: string;
  patientProfileId: string;
  patientFirstName: string;
  patientLastName: string;
  analysisDate: string;
  globalVerdict: string;
  globalConfidence: number;
  detectionsCount: number;
  hasReport: boolean;
}

export interface MammogramAnalysisDetail {
  id: string;
  patientProfileId: string;
  patientFirstName: string;
  patientLastName: string;
  doctorProfileId: string;
  analysisDate: string;
  globalVerdict: string;
  globalConfidence: number;
  detectionsCount: number;
  fullImage: string | null;
  fullNormalImage: string | null;
  segmentationImage: string | null;
  individualPredictions: MammogramPrediction[];
  aiReport: string | null;
  reportGeneratedAt: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ImagingService {
  private readonly API = '/api/v1';

  constructor(private readonly http: HttpClient) {}

  /**
   * Check if the AI service is available.
   */
  checkAiHealth(): Observable<{ aiServiceAvailable: boolean; message: string }> {
    return this.http.get<{ aiServiceAvailable: boolean; message: string }>(
      `${this.API}/mammogram/health`
    );
  }

  /**
   * Upload a mammogram image linked to a patient and get AI analysis results.
   */
  analyzeMammogram(
    doctorId: string,
    patientProfileId: string,
    file: File,
    pixelSpacing?: string
  ): Observable<MammogramAnalysisResponse> {
    console.log('[ImagingService] Starting analysis request:', { doctorId, patientProfileId, fileName: file.name });
    const formData = new FormData();
    formData.append('file', file);
    formData.append('patientProfileId', patientProfileId);
    if (pixelSpacing) {
      formData.append('pixelSpacing', pixelSpacing);
    }

    return this.http.post<MammogramAnalysisResponse>(
      `${this.API}/doctor/${doctorId}/mammogram/analyze`,
      formData
    );
  }

  /**
   * Get all analysis history for the doctor.
   */
  getDoctorHistory(doctorId: string): Observable<MammogramAnalysisHistory[]> {
    return this.http.get<MammogramAnalysisHistory[]>(
      `${this.API}/doctor/${doctorId}/mammogram/history`
    );
  }

  /**
   * Get analysis history for a specific patient.
   */
  getPatientHistory(doctorId: string, patientId: string): Observable<MammogramAnalysisHistory[]> {
    return this.http.get<MammogramAnalysisHistory[]>(
      `${this.API}/doctor/${doctorId}/mammogram/patient/${patientId}/history`
    );
  }

  /**
   * Get detailed view of a specific analysis (with images).
   */
  getAnalysisDetail(analysisId: string): Observable<MammogramAnalysisDetail> {
    return this.http.get<MammogramAnalysisDetail>(
      `${this.API}/mammogram/analysis/${analysisId}`
    );
  }

  /**
   * Generate AI report for an existing analysis.
   */
  generateReport(analysisId: string): Observable<{ report: string; analysisId: string }> {
    return this.http.post<{ report: string; analysisId: string }>(
      `${this.API}/mammogram/analysis/${analysisId}/report`,
      {}
    );
  }

  // ═══════════ RISK PREDICTION ═══════════

  /**
   * Check if the risk prediction AI service is available.
   */
  checkRiskHealth(): Observable<{ riskServiceAvailable: boolean; message: string }> {
    return this.http.get<{ riskServiceAvailable: boolean; message: string }>(
      `${this.API}/risk-prediction/health`
    );
  }

  /**
   * Predict recurrence risk for a patient (auto-populates known fields from DB).
   */
  predictRiskForPatient(patientId: string, features: RiskPredictionRequest): Observable<RiskPredictionResponse> {
    return this.http.post<RiskPredictionResponse>(
      `${this.API}/risk-prediction/predict-for-patient/${patientId}`,
      features
    );
  }
}

// ═══════════ RISK PREDICTION INTERFACES ═══════════

export interface RiskPredictionRequest {
  age_at_diagnosis?: number;
  type_of_breast_surgery?: string;
  cellularity?: string;
  chemotherapy?: string;
  pam50_claudin_low_subtype?: string;
  er_status_measured_by_ihc?: string;
  er_status?: string;
  neoplasm_histologic_grade?: number;
  her2_status_measured_by_snp6?: string;
  her2_status?: string;
  tumor_other_histologic_subtype?: string;
  hormone_therapy?: string;
  inferred_menopausal_state?: string;
  integrative_cluster?: string;
  primary_tumor_laterality?: string;
  lymph_nodes_examined_positive?: number;
  mutation_count?: number;
  nottingham_prognostic_index?: number;
  pr_status?: string;
  radio_therapy?: string;
  '3_gene_classifier_subtype'?: string;
  tumor_size?: number;
  tumor_stage?: number;
}

export interface RiskPredictionResponse {
  probability: number;
  probability_percent: number;
  is_high_risk: boolean;
  risk_level: string;
  threshold: number;
  model_version: string;
  features_used: number;
  model_metrics: {
    f1: number;
    auc: number;
  };
}
