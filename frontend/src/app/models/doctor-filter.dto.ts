import { DoctorType, ConsultationMode } from './enums';

export interface DoctorFilterDto {
    keyword?: string;
    city?: string;
    country?: string;
    active?: boolean;
    doctorType?: DoctorType;
    speciality?: string;
    consultationMode?: ConsultationMode;
    verified?: boolean;
    clinicName?: string;
    minYearsOfExperience?: number;
    maxConsultationFee?: number;
    language?: string;
}
