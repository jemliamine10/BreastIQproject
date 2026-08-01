import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AppointmentStatus,
  AppointmentCreateRequestDto,
  AppointmentType,
  AppointmentStats,
  PaginatedResponse,
  PatientAppointment,
  TimelineEvent,
  UpdatePatientAppointment,
  NextAppointmentResponse
} from '../models/appointment.model';

@Injectable({
  providedIn: 'root'
})
export class PatientAppointmentService {
  private readonly baseUrl = '/api/appointments';
  private readonly patientBaseUrl = '/api/patient/appointments';

  constructor(private readonly http: HttpClient) {}

  getAppointments(params?: {
    patientId?: string;
    date?: string;
    type?: AppointmentType;
    status?: AppointmentStatus;
    doctorId?: string;
    page?: number;
    size?: number;
  }): Observable<PaginatedResponse<PatientAppointment>> {
    let httpParams = new HttpParams();

    if (params?.patientId) httpParams = httpParams.set('patientId', params.patientId);
    if (params?.date) httpParams = httpParams.set('date', params.date);
    if (params?.type) httpParams = httpParams.set('type', params.type);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.doctorId) httpParams = httpParams.set('doctorId', params.doctorId);
    if (params?.page != null) httpParams = httpParams.set('page', params.page);
    if (params?.size != null) httpParams = httpParams.set('size', params.size);

    return this.http.get<PaginatedResponse<PatientAppointment>>(this.patientBaseUrl, {
      params: httpParams
    });
  }

  getAppointmentDetails(id: string): Observable<PatientAppointment> {
    return this.http.get<PatientAppointment>(`${this.baseUrl}/${id}`);
  }

  createAppointment(payload: AppointmentCreateRequestDto): Observable<PatientAppointment> {
    return this.http.post<PatientAppointment>(this.baseUrl, payload);
  }

  updateAppointment(id: string, payload: UpdatePatientAppointment): Observable<PatientAppointment> {
    return this.http.put<PatientAppointment>(`${this.baseUrl}/${id}`, payload);
  }

  rescheduleAppointment(id: string, payload: Pick<UpdatePatientAppointment, 'date' | 'endDate' | 'timezone'>): Observable<PatientAppointment> {
    return this.http.put<PatientAppointment>(`${this.baseUrl}/${id}`, payload);
  }

  cancelAppointment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getNextAppointment(patientId?: string): Observable<NextAppointmentResponse> {
    let params = new HttpParams();
    if (patientId) {
      params = params.set('patientId', patientId);
    }
    return this.http.get<NextAppointmentResponse>(`${this.patientBaseUrl}/next`, { params });
  }

  getStats(patientId?: string): Observable<AppointmentStats> {
    let params = new HttpParams();
    if (patientId) {
      params = params.set('patientId', patientId);
    }
    return this.http.get<AppointmentStats>(`${this.patientBaseUrl}/stats`, { params });
  }

  getTimeline(patientId?: string): Observable<TimelineEvent[]> {
    let params = new HttpParams();
    if (patientId) {
      params = params.set('patientId', patientId);
    }
    return this.http.get<TimelineEvent[]>(`${this.patientBaseUrl}/timeline`, { params });
  }
}
