import { DoctorType, ConsultationMode, Gender } from './enums';

export interface DoctorFullResponseDto {
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

    // ---- infos DoctorProfile ----
    doctorProfileId: string;
    doctorType: DoctorType;
    speciality?: string;
    licenseNumber?: string;
    clinicName?: string;
    bio?: string;
    yearsOfExperience?: number;
    languages?: string;
    consultationMode: ConsultationMode;
    consultationFee?: number;
    timezone?: string;
    verified: boolean;
    verifiedAt?: string;
    addressText?: string;
    latitude?: number;
    longitude?: number;
}
