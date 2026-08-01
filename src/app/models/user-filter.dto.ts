import { UserRole, Gender } from './enums';

export interface UserFilterDto {
    keyword?: string;
    role?: UserRole;
    gender?: Gender;
    city?: string;
    country?: string;
    active?: boolean;
    emailVerified?: boolean;
}
