import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MedicalEventResponseDto } from '../models/timeline-event.dto';
import { EventType } from '../models/enums';

@Injectable({ providedIn: 'root' })
export class TimelineService {
  private readonly API = '/api/timeline';

  constructor(private http: HttpClient) {}

  /** Get all timeline events for a patient (sorted eventDate desc) */
  getByPatient(patientId: string, doctorId: string): Observable<MedicalEventResponseDto[]> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.get<MedicalEventResponseDto[]>(`${this.API}/patient/${patientId}`, { params });
  }

  /** Get timeline events filtered by event type */
  getByPatientAndType(patientId: string, eventType: EventType, doctorId: string): Observable<MedicalEventResponseDto[]> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.get<MedicalEventResponseDto[]>(
      `${this.API}/patient/${patientId}/type/${eventType}`, { params }
    );
  }

  /** Get timeline for the patient themselves */
  getMyTimeline(patientId: string): Observable<MedicalEventResponseDto[]> {
    return this.http.get<MedicalEventResponseDto[]>(`${this.API}/patient/${patientId}/events`);
  }
}
