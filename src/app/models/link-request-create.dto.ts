import { LinkRequestedBy } from './enums';

export interface LinkRequestCreateDto {
    patientProfileId?: string; // UUID
    patientId?: string; // Alias frontend/backend (UUID)
    doctorProfileId?: string; // UUID
    doctorId?: string; // Alias frontend/backend (UUID)
    requestedBy: LinkRequestedBy;
    requestNote?: string; // Max 1000
}
