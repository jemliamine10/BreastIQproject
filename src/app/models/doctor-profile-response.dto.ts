import { DoctorType, ConsultationMode } from './enums';

export interface DoctorProfileResponseDto {
    id: string; // UUID
    userId: string; // UUID
    doctorType?: DoctorType;
    speciality?: string;
    licenseNumber?: string;
    clinicName?: string;
    bio?: string;
    yearsOfExperience?: number;
    languages?: string;
    consultationMode?: ConsultationMode;
    consultationFee?: number;
    timezone?: string;
    verified: boolean;
    verifiedAt?: string; // Instant
    addressText?: string;
    latitude?: number;
    longitude?: number;
}
