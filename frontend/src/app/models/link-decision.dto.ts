import { LinkStatus } from './enums';

export interface LinkDecisionDto {
    newStatus: LinkStatus; // ACTIVE / REJECTED / BLOCKED / ENDED
    decisionByUserId: string; // UUID
    rejectionReason?: string; // Max 1000
}
