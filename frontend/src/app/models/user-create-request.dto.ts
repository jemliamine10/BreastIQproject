import { UserRole, Gender } from './enums';

export interface UserCreateRequestDto {
    email: string;
    password: string; // Min 6, Max 100
    role: UserRole;
    firstName: string; // Max 80
    lastName: string; // Max 80
    phone?: string; // Max 30
    gender?: Gender;
    dateOfBirth?: string; // LocalDate as string (YYYY-MM-DD)
    profilePhotoUrl?: string; // Max 500
    addressText?: string; // Max 300
    city?: string; // Max 80
    country?: string; // Max 80
}
