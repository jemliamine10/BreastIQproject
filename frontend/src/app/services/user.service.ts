import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserRole } from '../models/enums';
import { UserResponseDto } from '../models/user-response.dto';
import { UserFilterDto } from '../models/user-filter.dto';
import { DoctorFullResponseDto } from '../models/doctor-full-response.dto';
import { DoctorFilterDto } from '../models/doctor-filter.dto';
import { PatientFullResponseDto } from '../models/patient-full-response.dto';
import { PatientFilterDto } from '../models/patient-filter.dto';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly PI_URL = '/api/users';

  constructor(private http: HttpClient) {}

  // ======================== TOUS LES UTILISATEURS ========================

  getAllUsers(): Observable<UserResponseDto[]> {
    return this.http.get<UserResponseDto[]>(this.PI_URL);
  }

  getUserById(id: string): Observable<UserResponseDto> {
    return this.http.get<UserResponseDto>(`${this.PI_URL}/${id}`);
  }

  getUserByEmail(email: string): Observable<UserResponseDto> {
    const params = new HttpParams().set('email', email);
    return this.http.get<UserResponseDto>(`${this.PI_URL}/by-email`, { params });
  }

  getUsersByRole(role: UserRole): Observable<UserResponseDto[]> {
    const params = new HttpParams().set('role', role);
    return this.http.get<UserResponseDto[]>(`${this.PI_URL}/by-role`, { params });
  }

  // ======================== FILTRAGE UTILISATEURS ========================

  filterUsers(filter: UserFilterDto): Observable<UserResponseDto[]> {
    let params = new HttpParams();
    Object.keys(filter).forEach(key => {
      const value = (filter as any)[key];
      if (value !== null && value !== undefined) {
        params = params.set(key, value);
      }
    });
    return this.http.get<UserResponseDto[]>(`${this.PI_URL}/filter`, { params });
  }

  // ======================== MÉDECINS ========================

  getAllDoctors(): Observable<DoctorFullResponseDto[]> {
    return this.http.get<DoctorFullResponseDto[]>(`${this.PI_URL}/doctors`);
  }

  getDoctorByUserId(userId: string): Observable<DoctorFullResponseDto> {
    return this.http.get<DoctorFullResponseDto>(`${this.PI_URL}/doctors/by-user/${userId}`);
  }

  filterDoctors(filter: DoctorFilterDto): Observable<DoctorFullResponseDto[]> {
    let params = new HttpParams();
    Object.keys(filter).forEach(key => {
      const value = (filter as any)[key];
      if (value !== null && value !== undefined) {
        params = params.set(key, value);
      }
    });
    return this.http.get<DoctorFullResponseDto[]>(`${this.PI_URL}/doctors/filter`, { params });
  }

  // ======================== PATIENTS ========================

  getAllPatients(): Observable<PatientFullResponseDto[]> {
    return this.http.get<PatientFullResponseDto[]>(`${this.PI_URL}/patients`);
  }

  getPatientByUserId(userId: string): Observable<PatientFullResponseDto> {
    return this.http.get<PatientFullResponseDto>(`${this.PI_URL}/patients/by-user/${userId}`);
  }

  filterPatients(filter: PatientFilterDto): Observable<PatientFullResponseDto[]> {
    let params = new HttpParams();
    Object.keys(filter).forEach(key => {
      const value = (filter as any)[key];
      if (value !== null && value !== undefined) {
        params = params.set(key, value);
      }
    });
    return this.http.get<PatientFullResponseDto[]>(`${this.PI_URL}/patients/filter`, { params });
  }
}
