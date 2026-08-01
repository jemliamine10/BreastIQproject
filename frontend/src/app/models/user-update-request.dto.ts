import { Gender } from './enums';

export interface UserUpdateRequestDto {
    email?: string;
    firstName?: string;
    lastName?: string;
    phone?: string;
    gender?: Gender;
    dateOfBirth?: string; // LocalDate yyyy-MM-dd
    profilePhotoUrl?: string;
    addressText?: string;
    city?: string;
    country?: string;
    active?: boolean;
}
