import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserCreateRequestDto } from '../models/user-create-request.dto';
import { DoctorProfileCreateRequestDto } from '../models/doctor-profile-create-request.dto';
import { DoctorProfileResponseDto } from '../models/doctor-profile-response.dto';
import { PatientProfileCreateRequestDto } from '../models/patient-profile-create-request.dto';
import { PatientProfileResponseDto } from '../models/patient-profile-response.dto';

export interface RegisterDoctorRequest {
  user: UserCreateRequestDto;
  doctor: DoctorProfileCreateRequestDto;
}

export interface RegisterPatientRequest {
  user: UserCreateRequestDto;
  patient: PatientProfileCreateRequestDto;
}

@Injectable({
  providedIn: 'root'
})
export class RegistrationService {
  private readonly API_URL = '/api/registration';

  constructor(private http: HttpClient) {}

  /** GET /api/registration/email-available?email=... */
  checkEmailAvailable(email: string): Observable<boolean> {
    const params = new HttpParams().set('email', email);
    return this.http.get<boolean>(`${this.API_URL}/email-available`, { params });
  }

  /** GET /api/registration/doctor-types */
  listDoctorTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.API_URL}/doctor-types`);
  }

  /** GET /api/registration/genders */
  listGenders(): Observable<string[]> {
    return this.http.get<string[]>(`${this.API_URL}/genders`);
  }

  /** POST /api/registration/doctor  body: { user, doctor } */
  registerDoctor(
    user: UserCreateRequestDto,
    doctor: DoctorProfileCreateRequestDto
  ): Observable<DoctorProfileResponseDto> {
    const body: RegisterDoctorRequest = { user, doctor };
    return this.http.post<DoctorProfileResponseDto>(`${this.API_URL}/doctor`, body);
  }

  /** POST /api/registration/patient  body: { user, patient } */
  registerPatient(
    user: UserCreateRequestDto,
    patient: PatientProfileCreateRequestDto
  ): Observable<PatientProfileResponseDto> {
    const body: RegisterPatientRequest = { user, patient };
    return this.http.post<PatientProfileResponseDto>(`${this.API_URL}/patient`, body);
  }
}
