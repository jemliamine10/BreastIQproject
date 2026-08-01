export enum UserRole {
    ADMIN = 'ADMIN',
    DOCTOR = 'DOCTOR',
    PATIENT = 'PATIENT'
}

export enum Gender {
    MALE = 'MALE',
    FEMALE = 'FEMALE',
    OTHER = 'OTHER'
}

export enum DoctorType {
    ONCOLOGIST = 'ONCOLOGIST',
    SURGEON = 'SURGEON',
    RADIOLOGIST = 'RADIOLOGIST',
    GENERALIST = 'GENERALIST',
    PATHOLOGIST = 'PATHOLOGIST',
    OTHER = 'OTHER'
}

export enum ConsultationMode {
    IN_PERSON = 'IN_PERSON',
    REMOTE = 'REMOTE',
    HYBRID = 'HYBRID'
}

export enum LinkStatus {
    PENDING = 'PENDING',
    ACTIVE = 'ACTIVE',
    REJECTED = 'REJECTED',
    BLOCKED = 'BLOCKED',
    ENDED = 'ENDED'
}

export enum LinkRequestedBy {
    PATIENT = 'PATIENT',
    DOCTOR = 'DOCTOR'
}

export enum AllergySeverity {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH'
}

// ── Clinical Monitoring Enums ──

export enum PatientStatus {
    STABLE = 'STABLE',
    WARNING = 'WARNING',
    CRITICAL = 'CRITICAL'
}

export enum BloodType {
    A_POSITIVE = 'A_POSITIVE',
    A_NEGATIVE = 'A_NEGATIVE',
    B_POSITIVE = 'B_POSITIVE',
    B_NEGATIVE = 'B_NEGATIVE',
    AB_POSITIVE = 'AB_POSITIVE',
    AB_NEGATIVE = 'AB_NEGATIVE',
    O_POSITIVE = 'O_POSITIVE',
    O_NEGATIVE = 'O_NEGATIVE'
}

export enum CancerStage {
    STAGE_0 = 'STAGE_0',
    STAGE_I = 'STAGE_I',
    STAGE_II = 'STAGE_II',
    STAGE_III = 'STAGE_III',
    STAGE_IV = 'STAGE_IV'
}

export enum TumorType {
    HR_POSITIVE = 'HR_POSITIVE',
    HER2_POSITIVE = 'HER2_POSITIVE',
    TRIPLE_NEGATIVE = 'TRIPLE_NEGATIVE',
    HR_POSITIVE_HER2_POSITIVE = 'HR_POSITIVE_HER2_POSITIVE',
    UNKNOWN = 'UNKNOWN'
}

export enum ReceptorStatus {
    POSITIVE = 'POSITIVE',
    NEGATIVE = 'NEGATIVE',
    UNKNOWN = 'UNKNOWN'
}

export enum TreatmentType {
    CHEMO = 'CHEMO',
    RADIO = 'RADIO',
    SURGERY = 'SURGERY',
    HORMONAL = 'HORMONAL',
    IMMUNOTHERAPY = 'IMMUNOTHERAPY'
}

export enum TreatmentStatus {
    UPCOMING = 'UPCOMING',
    ACTIVE = 'ACTIVE',
    COMPLETED = 'COMPLETED',
    STOPPED = 'STOPPED'
}

export enum SessionStatus {
    PLANNED = 'PLANNED',
    DONE = 'DONE',
    MISSED = 'MISSED',
    CANCELLED = 'CANCELLED'
}

export enum AlertSeverity {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH',
    CRITICAL = 'CRITICAL'
}

export enum AlertType {
    INFECTION_RISK = 'INFECTION_RISK',
    SEVERE_PAIN = 'SEVERE_PAIN',
    RAPID_WEIGHT_LOSS = 'RAPID_WEIGHT_LOSS',
    COMBINED_SYMPTOMS = 'COMBINED_SYMPTOMS',
    HIGH_TEMPERATURE = 'HIGH_TEMPERATURE',
    MISSED_SESSION = 'MISSED_SESSION',
    CUSTOM = 'CUSTOM'
}

export enum EventType {
    DIAGNOSIS = 'DIAGNOSIS',
    TREATMENT_START = 'TREATMENT_START',
    TREATMENT_END = 'TREATMENT_END',
    SESSION_COMPLETED = 'SESSION_COMPLETED',
    SESSION_MISSED = 'SESSION_MISSED',
    ALERT_GENERATED = 'ALERT_GENERATED',
    TRACKER_ENTRY = 'TRACKER_ENTRY',
    APPOINTMENT = 'APPOINTMENT',
    MEDICAL_NOTE = 'MEDICAL_NOTE',
    STATUS_CHANGE = 'STATUS_CHANGE'
}

export enum HistoryType {
    PERSONAL = 'PERSONAL',
    FAMILY = 'FAMILY',
    SURGICAL = 'SURGICAL'
}

export enum RiskLevel {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH'
}
