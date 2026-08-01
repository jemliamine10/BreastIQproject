import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PatientProfileResponseDto } from '../models/patient-profile-response.dto';
import { PatientProfileUpdateRequestDto } from '../models/patient-profile-update-request.dto';
import { LocationUpdateRequestDto } from '../models/location-update-request.dto';
import { MedicalRecordResponseDto, MedicalHistoryDto } from '../models/medical-record.dto';
import { AllergyResponseDto } from '../models/allergy-response.dto';
import { TreatmentResponseDto } from '../models/treatment-response.dto';

@Injectable({ providedIn: 'root' })
export class PatientProfileService {
  private readonly API = '/api/patients';

  constructor(private http: HttpClient) {}

  /** Get patient profile by profile ID */
  getById(patientProfileId: string): Observable<PatientProfileResponseDto> {
    return this.http.get<PatientProfileResponseDto>(`${this.API}/${patientProfileId}`);
  }

  /** Get patient profile by user ID */
  getByUserId(userId: string): Observable<PatientProfileResponseDto> {
    return this.http.get<PatientProfileResponseDto>(`${this.API}/by-user/${userId}`);
  }

  /** Update patient profile */
  update(patientProfileId: string, dto: PatientProfileUpdateRequestDto): Observable<PatientProfileResponseDto> {
    return this.http.put<PatientProfileResponseDto>(`${this.API}/${patientProfileId}`, dto);
  }

  /** Update patient location */
  updateLocation(patientProfileId: string, dto: LocationUpdateRequestDto): Observable<PatientProfileResponseDto> {
    return this.http.put<PatientProfileResponseDto>(`${this.API}/${patientProfileId}/location`, dto);
  }

  /** Toggle medical consent */
  updateConsent(patientProfileId: string, value: boolean): Observable<PatientProfileResponseDto> {
    const params = new HttpParams().set('value', value.toString());
    return this.http.put<PatientProfileResponseDto>(
      `${this.API}/${patientProfileId}/consent`, null, { params }
    );
  }

  // =====================================
  // Medical Record & History
  // =====================================
  private readonly MR_API = '/api/medical-records';

  /** Get personal aggregated medical record */
  getMyMedicalRecord(patientProfileId: string): Observable<MedicalRecordResponseDto> {
    return this.http.get<MedicalRecordResponseDto>(`${this.MR_API}/patient/${patientProfileId}/my-record`);
  }

  /** Add personal medical history */
  addMyMedicalHistory(patientProfileId: string, dto: MedicalHistoryDto): Observable<MedicalHistoryDto> {
    return this.http.post<MedicalHistoryDto>(`${this.MR_API}/patient/${patientProfileId}/histories`, dto);
  }

  /** Update personal medical history */
  updateMyMedicalHistory(historyId: string, dto: MedicalHistoryDto): Observable<MedicalHistoryDto> {
    return this.http.put<MedicalHistoryDto>(`${this.MR_API}/histories/${historyId}`, dto);
  }

  /** Delete personal medical history */
  deleteMyMedicalHistory(historyId: string): Observable<void> {
    return this.http.delete<void>(`${this.MR_API}/histories/${historyId}`);
  }

  // =====================================
  // Allergies
  // =====================================

  addAllergy(dto: AllergyResponseDto): Observable<AllergyResponseDto> {
    return this.http.post<AllergyResponseDto>(`${this.API}/allergies`, dto);
  }

  updateAllergy(id: string, dto: AllergyResponseDto): Observable<AllergyResponseDto> {
    return this.http.put<AllergyResponseDto>(`${this.API}/allergies/${id}`, dto);
  }

  deleteAllergy(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/allergies/${id}`);
  }

  // =====================================
  // Treatments
  // =====================================

  addTreatment(dto: TreatmentResponseDto): Observable<TreatmentResponseDto> {
    return this.http.post<TreatmentResponseDto>(`${this.API}/treatments`, dto);
  }

  updateTreatment(id: string, dto: TreatmentResponseDto): Observable<TreatmentResponseDto> {
    return this.http.put<TreatmentResponseDto>(`${this.API}/treatments/${id}`, dto);
  }

  deleteTreatment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/treatments/${id}`);
  }
}
