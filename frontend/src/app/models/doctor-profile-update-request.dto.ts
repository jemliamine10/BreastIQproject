import { DoctorType, ConsultationMode } from './enums';

export interface DoctorProfileUpdateRequestDto {
    doctorType?: DoctorType; // Optional
    speciality?: string; // Max 120
    clinicName?: string; // Max 160
    bio?: string; // Max 1500
    yearsOfExperience?: number;
    languages?: string; // Max 120
    consultationMode?: ConsultationMode;
    consultationFee?: number; // BigDecimal -> number
    addressText?: string; // Max 300
    latitude?: number;
    longitude?: number;
    verified?: boolean;
}
