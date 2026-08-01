import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TrackerEntryCreateDto, TrackerEntryResponseDto } from '../models/tracker-entry.dto';

@Injectable({ providedIn: 'root' })
export class TrackerService {
  private readonly API = '/api/tracker';

  constructor(private http: HttpClient) {}

  /** Submit daily tracker entry — triggers alerts, risk, healthScore, timeline, WebSocket */
  submit(dto: TrackerEntryCreateDto, doctorId?: string): Observable<TrackerEntryResponseDto> {
    let params = new HttpParams();
    if (doctorId) params = params.set('doctorId', doctorId);
    return this.http.post<TrackerEntryResponseDto>(this.API, dto, { params });
  }

  /** Get all tracker entries for a patient (sorted recordedAt desc) */
  getHistory(patientId: string, doctorId: string): Observable<TrackerEntryResponseDto[]> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.get<TrackerEntryResponseDto[]>(`${this.API}/patient/${patientId}`, { params });
  }

  /** Get latest tracker entry for a patient */
  getLatest(patientId: string, doctorId: string): Observable<TrackerEntryResponseDto> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.get<TrackerEntryResponseDto>(`${this.API}/patient/${patientId}/latest`, { params });
  }

  /** Patient: Get my own history */
  getMyHistory(patientId: string): Observable<TrackerEntryResponseDto[]> {
    return this.http.get<TrackerEntryResponseDto[]>(`${this.API}/my-history/${patientId}`);
  }

  /** Patient: Get my latest entry */
  getMyLatest(patientId: string): Observable<TrackerEntryResponseDto> {
    return this.http.get<TrackerEntryResponseDto>(`${this.API}/my-latest/${patientId}`);
  }
}
