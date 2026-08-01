// login-response.dto.ts
import { UserRole } from './enums';

export interface LoginResponseDto {
    userId: string; // UUID
    email: string;
    role: UserRole;
    firstName: string;
    lastName: string;
    message: string;
    profilePhotoUrl?: string;
}
