import { DocumentEventDto } from './document.dto';

export interface StatusUpdatePayload {
    type: 'STATUS_UPDATE';
    patientId: string;
    status: string;            // STABLE | WARNING | CRITICAL
    healthScore: number;
    timestamp: string;         // ISO instant
}

export interface CriticalAlertPayload {
    type: 'CRITICAL_ALERT';
    alertId: string;
    patientId: string;
    severity: string;
    alertType: string;
    message: string;
    timestamp: string;
    patientName?: string;
}

export type AppWebSocketPayload = StatusUpdatePayload | CriticalAlertPayload | DocumentEventDto;
