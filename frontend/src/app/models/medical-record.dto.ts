import { CancerStage, TumorType, BloodType, ReceptorStatus, HistoryType } from './enums';
import { TreatmentResponseDto } from './treatment-response.dto';

// ── Create ──

export interface ClinicalDataDto {
    estrogenReceptor?: ReceptorStatus;
    progesteroneReceptor?: ReceptorStatus;
    her2Status?: ReceptorStatus;
    ki67?: number;
    tumorSize?: number;
    lymphNodesInvolved?: number;
    metastasis: boolean;
    grade?: number;
    notes?: string;
    
    // Needed for template
    erStatus?: string;
    prStatus?: string;
    ki67Index?: number;
    lastMammographyDate?: string;
    surgeryType?: string;
    radiotherapy?: boolean;
    chemotherapy?: boolean;
    hormoneTherapy?: boolean;

    // TNM components (computed server-side)
    tnmT?: string;
    tnmN?: string;
    tnmM?: string;
}

export interface MedicalRecordCreateDto {
    patientId: string;
    diagnosis?: string;
    cancerStage?: CancerStage;
    tumorType?: TumorType;
    consentGiven?: boolean;
    notes?: string;
    bloodType?: BloodType;
    heightCm?: number;
    weightKg?: number;
    clinicalData?: ClinicalDataDto;
}

// ── History ──

export interface MedicalHistoryDto {
    id?: string;
    patientId?: string;
    historyType: HistoryType;
    title: string;
    description: string;
    eventDate?: string;
    
    // Needed for template
    occurrenceDate?: string;
    relation?: string;
    notes?: string;
    type?: string; 
}

// ── Response ──

export interface MedicalRecordResponseDto {
    id: string;
    patientId: string;
    diagnosis?: string;
    cancerStage?: CancerStage;
    tumorType?: TumorType;
    consentGiven: boolean;
    notes?: string;
    bloodType?: string;
    heightCm?: number;
    weightKg?: number;
    bmi?: number;
    clinicalData?: ClinicalDataDto;
    medicalHistories: MedicalHistoryDto[];
    allergies: AllergyInRecordDto[];
    treatments: TreatmentResponseDto[];
    createdAt: string;
    updatedAt: string;

    // Needed for template
    tumorGrade?: string;
    tumorSizeMm?: number;
    diagnosisNotes?: string;
    history?: MedicalHistoryDto[];

    // TNM staging (computed server-side)
    tnmClassification?: string;
    stageAutoComputed?: boolean;
    computedStageLabel?: string;
}

export interface AllergyInRecordDto {
    id: string;
    substance: string;
    reaction: string;
    severity: string;
}
