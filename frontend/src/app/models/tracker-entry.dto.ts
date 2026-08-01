export interface TrackerEntryCreateDto {
    patientId: string;
    painLevel?: number;
    fatigueLevel?: number;
    moodLevel?: number;
    temperature?: number;
    weight?: number;
    vomiting: boolean;
    diarrhea: boolean;
    appetiteLoss: boolean;
    notes?: string;
}

export interface TrackerEntryResponseDto {
    id: string;
    patientId: string;
    painLevel?: number;
    fatigueLevel?: number;
    moodLevel?: number;
    temperature?: number;
    weight?: number;
    vomiting: boolean;
    diarrhea: boolean;
    appetiteLoss: boolean;
    notes?: string;
    recordedAt: string;     // Instant ISO
    healthScore: number;
    riskLevel: string;      // LOW | MEDIUM | HIGH
}
