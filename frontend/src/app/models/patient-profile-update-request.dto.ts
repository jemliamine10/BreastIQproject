export interface PatientProfileUpdateRequestDto {
    medicalRecordNumber?: string; // Max 80
    emergencyContactName?: string; // Max 120
    emergencyContactPhone?: string; // Max 30
    heightCm?: number;
    weightKg?: number;
    medicalConsent?: boolean;
    consentTimestamp?: string; // Instant
    lastKnownLatitude?: number;
    lastKnownLongitude?: number;
    assignedDoctorProfileId?: string; // UUID
}
