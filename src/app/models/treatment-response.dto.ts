import { TreatmentType, TreatmentStatus } from './enums';

export interface TreatmentResponseDto {
    id: string; // UUID
    patientProfileId?: string; // UUID (Optional for mocks)
    treatmentType?: TreatmentType;
    protocol?: string;
    medicationName?: string;
    dosage?: string;
    startDate?: string;
    endDate?: string;
    cyclesTotal?: number;
    currentCycle?: number;
    status?: TreatmentStatus;
    notes?: string;

    // Compatibility with old code naming if necessary (though medicationName is preferred now)
    name?: string; 
    description?: string;
}
