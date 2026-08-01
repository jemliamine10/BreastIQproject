import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MedicalRecordCreateDto,
  MedicalRecordResponseDto,
  ClinicalDataDto,
  MedicalHistoryDto
} from '../models/medical-record.dto';
import { CancerStage, TumorType } from '../models/enums';

@Injectable({ providedIn: 'root' })
export class MedicalRecordService {
  private readonly API = '/api/medical-records';

  constructor(private http: HttpClient) {}

  /** Create a new medical record (doctor) */
  create(dto: MedicalRecordCreateDto, doctorId: string): Observable<MedicalRecordResponseDto> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.post<MedicalRecordResponseDto>(this.API, dto, { params });
  }

  /** Get aggregated medical record for a patient */
  getByPatient(patientId: string, doctorId: string): Observable<MedicalRecordResponseDto> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.get<MedicalRecordResponseDto>(`${this.API}/patient/${patientId}`, { params });
  }

  /** Update diagnosis fields (query params) */
  updateDiagnosis(
    patientId: string,
    doctorId: string,
    diagnosis?: string,
    cancerStage?: CancerStage,
    tumorType?: TumorType
  ): Observable<MedicalRecordResponseDto> {
    let params = new HttpParams().set('doctorId', doctorId);
    if (diagnosis) params = params.set('diagnosis', diagnosis);
    if (cancerStage) params = params.set('cancerStage', cancerStage);
    if (tumorType) params = params.set('tumorType', tumorType);
    return this.http.put<MedicalRecordResponseDto>(
      `${this.API}/patient/${patientId}/diagnosis`, null, { params }
    );
  }

  /** Update clinical data */
  updateClinicalData(patientId: string, dto: ClinicalDataDto, doctorId: string): Observable<ClinicalDataDto> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.put<ClinicalDataDto>(
      `${this.API}/patient/${patientId}/clinical-data`, dto, { params }
    );
  }

  /** Add medical history entry */
  addHistory(patientId: string, dto: MedicalHistoryDto, doctorId: string): Observable<MedicalHistoryDto> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.post<MedicalHistoryDto>(
      `${this.API}/patient/${patientId}/history`, dto, { params }
    );
  }

  /** Soft-delete a medical history entry */
  deleteHistory(historyId: string, doctorId: string): Observable<void> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.delete<void>(`${this.API}/history/${historyId}`, { params });
  }
}
