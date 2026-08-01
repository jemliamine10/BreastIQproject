import { AllergyResponseDto } from './allergy-response.dto';
import { TreatmentResponseDto } from './treatment-response.dto';
import { PatientStatus, BloodType } from './enums';

export interface PatientProfileResponseDto {
    id: string;
    userId: string;
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
    profileCompletion?: number;
    medicalConsent: boolean;
    consentTimestamp?: string;
    lastKnownLatitude?: number;
    lastKnownLongitude?: number;
    allergies?: AllergyResponseDto[];
    treatments?: TreatmentResponseDto[];
}
