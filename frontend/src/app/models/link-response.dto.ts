import { LinkStatus, LinkRequestedBy } from './enums';

export interface LinkResponseDto {
    id: string; // UUID
    patientProfileId: string; // UUID
    doctorProfileId: string; // UUID
    status: LinkStatus;
    requestedBy: LinkRequestedBy;
    requestNote?: string;
    decisionByUserId?: string; // UUID
    rejectionReason?: string;
    requestedAt?: string; // Instant
    activatedAt?: string; // Instant
    endedAt?: string; // Instant
    lastUpdatedAt?: string; // Instant
}
