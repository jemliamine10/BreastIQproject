import { AllergySeverity } from './enums';

export interface AllergyResponseDto {
    id: string; // UUID
    patientProfileId: string; // UUID
    substance: string;
    reaction: string;
    severity: AllergySeverity;
}
