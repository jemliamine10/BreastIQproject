import { Gender, PatientStatus, BloodType } from './enums';
import { TreatmentFullResponseDto } from './treatment-management.dto';

export interface PatientFullResponseDto {
    // ---- infos User ----
    userId: string;
    email: string;
    firstName: string;
    lastName: string;
    phone?: string;
    gender?: Gender;
    dateOfBirth?: string;
    profilePhotoUrl?: string;
    city?: string;
    country?: string;
    active: boolean;

    // ---- infos PatientProfile ----
    patientProfileId: string;
    assignedDoctorProfileId?: string;
    medicalRecordNumber?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    heightCm?: number;
    weightKg?: number;
    bmi?: number;
    bloodType?: BloodType;
    healthScore: number;
    patientStatus: PatientStatus;
    medicalConsent: boolean;
    consentTimestamp?: string;
    lastKnownLatitude?: number;
    lastKnownLongitude?: number;

    // Needed for template
    treatments?: TreatmentFullResponseDto[];
}
