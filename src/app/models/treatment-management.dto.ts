import { TreatmentType, TreatmentStatus, SessionStatus } from './enums';

export interface TreatmentCreateParams {
    patientId: string;
    type: TreatmentType;
    protocol?: string;
    medicationName?: string;
    dosage?: string;
    startDate?: string;        // LocalDate ISO
    endDate?: string;
    cyclesTotal?: number;
    intervalDays?: number;     // default 21
    notes?: string;
}

export interface TreatmentFullResponseDto {
    id: string;
    patientProfileId: string;
    patientId?: string;
    treatmentType: TreatmentType;
    protocol?: string;
    medicationName?: string;
    dosage?: string;
    startDate?: string;
    endDate?: string;
    cyclesTotal?: number;
    currentCycle?: number;
    status: TreatmentStatus;
    notes?: string;
    deleted?: boolean;
}

export interface TreatmentSessionResponseDto {
    id: string;
    treatmentId: string;
    sessionNumber: number;
    scheduledDate: string;     // LocalDate ISO
    actualDate?: string;
    status: SessionStatus;
    notes?: string;
    sideEffects?: string;
    createdAt: string;
    updatedAt: string;
}
