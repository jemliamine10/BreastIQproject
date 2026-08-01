import { EventType } from './enums';

export interface MedicalEventResponseDto {
    id: string;
    patientId: string;
    eventType: EventType;
    title: string;
    description?: string;
    severity?: string;
    referenceId?: string;
    referenceType?: string;
    eventDate: string;          // Instant ISO
    createdAt: string;
}
