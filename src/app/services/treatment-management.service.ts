import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TreatmentCreateParams,
  TreatmentFullResponseDto,
  TreatmentSessionResponseDto
} from '../models/treatment-management.dto';

export type TreatmentStatus = 'UPCOMING' | 'ACTIVE' | 'ONGOING' | 'COMPLETED' | 'STOPPED';
export type TreatmentResponse = TreatmentFullResponseDto;

@Injectable({ providedIn: 'root' })
export class TreatmentManagementService {
  private readonly API = '/api/treatment-management';
  private readonly baseUrl = '/api/treatment-management';
  private readonly uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  constructor(private http: HttpClient) {}

  /** Create a treatment (all fields sent as query params) */
  create(params: TreatmentCreateParams, doctorId: string): Observable<void> {
    let httpParams = new HttpParams()
      .set('patientId', params.patientId)
      .set('doctorId', doctorId)
      .set('type', params.type);
    if (params.protocol) httpParams = httpParams.set('protocol', params.protocol);
    if (params.medicationName) httpParams = httpParams.set('medicationName', params.medicationName);
    if (params.dosage) httpParams = httpParams.set('dosage', params.dosage);
    if (params.startDate) httpParams = httpParams.set('startDate', params.startDate);
    if (params.endDate) httpParams = httpParams.set('endDate', params.endDate);
    if (params.cyclesTotal != null) httpParams = httpParams.set('cyclesTotal', params.cyclesTotal.toString());
    if (params.intervalDays != null) httpParams = httpParams.set('intervalDays', params.intervalDays.toString());
    if (params.notes) httpParams = httpParams.set('notes', params.notes);

    return this.http.post<void>(this.API, null, { params: httpParams });
  }

  /** IMPORTANT: patientId must be injected in URL, never passed as raw "{patientId}" text */
  getPatientTreatments(patientId: string, doctorId: string, status?: TreatmentStatus): Observable<TreatmentResponse[]> {
    const cleanPatientId = (patientId || '').trim();
    const cleanDoctorId = (doctorId || '').trim();

    if (!this.uuidRegex.test(cleanPatientId)) {
      throw new Error('[TreatmentManagementService] Invalid patientId for getPatientTreatments');
    }

    if (!this.uuidRegex.test(cleanDoctorId)) {
      throw new Error('[TreatmentManagementService] Missing or invalid doctorId query param for getPatientTreatments');
    }

    let params = new HttpParams().set('doctorId', cleanDoctorId);
    if (status) params = params.set('status', status);

    return this.http.get<TreatmentResponse[]>(
      `${this.baseUrl}/patient/${cleanPatientId}/treatments`,
      { params }
    );
  }

  /** Soft-delete a treatment */
  delete(treatmentId: string, doctorId: string): Observable<void> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.delete<void>(`${this.API}/${treatmentId}`, { params });
  }

  /** Get sessions for a treatment */
  getSessions(treatmentId: string, doctorId: string): Observable<TreatmentSessionResponseDto[]> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.get<TreatmentSessionResponseDto[]>(`${this.API}/${treatmentId}/sessions`, { params });
  }

  /** Mark a session as DONE */
  markDone(sessionId: string, doctorId: string, notes?: string, sideEffects?: string): Observable<TreatmentSessionResponseDto> {
    let params = new HttpParams().set('doctorId', doctorId);
    if (notes) params = params.set('notes', notes);
    if (sideEffects) params = params.set('sideEffects', sideEffects);
    return this.http.put<TreatmentSessionResponseDto>(
      `${this.API}/sessions/${sessionId}/done`, null, { params }
    );
  }

  /** Mark a session as MISSED */
  markMissed(sessionId: string, doctorId: string, reason?: string): Observable<TreatmentSessionResponseDto> {
    let params = new HttpParams().set('doctorId', doctorId);
    if (reason) params = params.set('reason', reason);
    return this.http.put<TreatmentSessionResponseDto>(
      `${this.API}/sessions/${sessionId}/missed`, null, { params }
    );
  }

  /** Refresh treatment statuses for a patient */
  refreshStatuses(patientId: string, doctorId: string): Observable<void> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.post<void>(`${this.API}/patient/${patientId}/refresh`, null, { params });
  }
}
