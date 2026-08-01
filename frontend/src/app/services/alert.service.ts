import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AlertResponseDto } from '../models/alert.dto';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly API = '/api/alerts';

  constructor(private http: HttpClient) {}

  /** Get alerts for a patient (optionally only unresolved) */
  getByPatient(patientId: string, doctorId: string, unresolvedOnly = false): Observable<AlertResponseDto[]> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('unresolvedOnly', unresolvedOnly.toString());
    return this.http.get<AlertResponseDto[]>(`${this.API}/patient/${patientId}`, { params });
  }

  /** Get unresolved alerts for a doctor's assigned patients */
  getByDoctor(doctorId: string): Observable<AlertResponseDto[]> {
    return this.http.get<AlertResponseDto[]>(`${this.API}/doctor/${doctorId}`);
  }

  /** Resolve an alert (doctor action) */
  resolve(alertId: string, doctorUserId: string, notes?: string): Observable<AlertResponseDto> {
    let params = new HttpParams().set('doctorUserId', doctorUserId);
    if (notes) params = params.set('notes', notes);
    return this.http.put<AlertResponseDto>(`${this.API}/${alertId}/resolve`, null, { params });
  }
}
