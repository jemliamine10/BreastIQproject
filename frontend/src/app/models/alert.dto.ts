import { AlertSeverity, AlertType } from './enums';

export interface AlertResponseDto {
    id: string;
    patientId: string;
    severity: AlertSeverity;
    alertType: AlertType;
    message: string;
    triggerData?: string;
    resolved: boolean;
    resolvedAt?: string;        // Instant ISO
    resolvedBy?: string;        // UUID
    resolutionNotes?: string;
    createdAt: string;          // Instant ISO

    // Needed for template
    type?: string;
}
