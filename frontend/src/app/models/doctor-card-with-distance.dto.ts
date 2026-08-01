export interface DoctorCardWithDistanceDto {
    doctorProfileId: string; // UUID
    userId: string; // UUID
    fullName: string;
    speciality: string;
    clinicName?: string;
    latitude?: number;
    longitude?: number;
    consultationFee?: number; // BigDecimal
    distanceKm?: number;
}
