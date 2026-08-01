import { UserRole, Gender } from './enums';

export interface UserResponseDto {
    id: string; // UUID
    email: string;
    role: UserRole;
    firstName: string;
    lastName: string;
    phone?: string;
    gender?: Gender;
    dateOfBirth?: string; // LocalDate
    profilePhotoUrl?: string;
    addressText?: string;
    city?: string;
    country?: string;
    active: boolean;
    emailVerified: boolean;
    createdAt: string; // Instant
    updatedAt: string; // Instant
    lastLoginAt?: string; // Instant
}
