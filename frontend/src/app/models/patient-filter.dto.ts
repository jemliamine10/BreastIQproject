export interface PatientFilterDto {
    keyword?: string;
    city?: string;
    country?: string;
    active?: boolean;
    medicalRecordNumber?: string;
    medicalConsent?: boolean;
    hasAssignedDoctor?: boolean;
}
