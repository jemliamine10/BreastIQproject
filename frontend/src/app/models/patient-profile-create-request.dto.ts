export interface PatientProfileCreateRequestDto {
    medicalRecordNumber?: string; // Max 80
    emergencyContactName?: string; // Max 120
    emergencyContactPhone?: string; // Max 30
    heightCm?: number;
    weightKg?: number;
    medicalConsent: boolean;
    consentTimestamp?: string; // Set by backend — optional from frontend
    lastKnownLatitude?: number;
    lastKnownLongitude?: number;
}
