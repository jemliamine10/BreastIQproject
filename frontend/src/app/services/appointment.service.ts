import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  AvailabilityExceptionDto,
  AppointmentAvailabilityResponseDto,
  AppointmentCreateFrontRequestDto,
  AppointmentResponseDto,
  AppointmentStatusRDV,
  AppointmentTypeRDV,
  CalendarSlotDto,
  DoctorAvailabilityDto,
  PatientAppointmentDto,
  SpringPage
} from '../models/links-appointments.dto';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private readonly baseUrl = '/api/appointments';
  private readonly doctorsBaseUrl = '/api/doctors';
  private readonly availabilityBaseUrl = '/api/availability';
  private readonly exceptionsBaseUrl = '/api/exceptions';

  constructor(private readonly http: HttpClient) { }

  checkAvailability(params: {
    doctorId: string;
    date: string;
    heure: string;
    durationMinutes?: number;
    typeRDV?: AppointmentTypeRDV;
  }): Observable<AppointmentAvailabilityResponseDto> {
    let httpParams = new HttpParams()
      .set('doctorId', params.doctorId)
      .set('date', params.date)
      .set('heure', params.heure);

    if (params.durationMinutes != null) {
      httpParams = httpParams.set('durationMinutes', params.durationMinutes);
    }
    if (params.typeRDV) {
      httpParams = httpParams.set('typeRDV', params.typeRDV);
    }

    return this.http.get<AppointmentAvailabilityResponseDto>(`${this.baseUrl}/available`, { params: httpParams });
  }

  createFront(payload: AppointmentCreateFrontRequestDto): Observable<PatientAppointmentDto> {
    return this.http.post<PatientAppointmentDto>(`${this.baseUrl}/create`, payload);
  }

  createAppointment(payload: {
    doctorId: string;
    type: AppointmentTypeRDV;
    title?: string;
    description?: string;
    date: string;
    endDate?: string;
    location?: string;
    timezone?: string;
  }): Observable<AppointmentResponseDto> {
    return this.http.post<AppointmentResponseDto>(this.baseUrl, payload);
  }

  updateAppointment(id: string, payload: {
    date?: string;
    endDate?: string;
    title?: string;
    description?: string;
    location?: string;
    status?: AppointmentStatusRDV;
    timezone?: string;
  }): Observable<AppointmentResponseDto> {
    return this.http.put<AppointmentResponseDto>(`${this.baseUrl}/${id}`, payload);
  }

  cancelAppointment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getAppointmentById(id: string): Observable<AppointmentResponseDto> {
    return this.http.get<AppointmentResponseDto>(`${this.baseUrl}/${id}`);
  }

  getPatientAppointments(params: {
    patientId: string;
    doctorId?: string;
    date?: string;
    typeRDV?: AppointmentTypeRDV;
    status?: AppointmentStatusRDV;
    page?: number;
    size?: number;
  }): Observable<SpringPage<PatientAppointmentDto>> {
    let httpParams = new HttpParams().set('patientId', params.patientId);
    if (params.doctorId) httpParams = httpParams.set('doctorId', params.doctorId);
    if (params.date) httpParams = httpParams.set('date', params.date);
    if (params.typeRDV) httpParams = httpParams.set('typeRDV', params.typeRDV);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.page != null) httpParams = httpParams.set('page', params.page);
    if (params.size != null) httpParams = httpParams.set('size', params.size);

    return this.http.get<SpringPage<PatientAppointmentDto>>(`${this.baseUrl}/patient`, { params: httpParams });
  }

  /**
   * Récupération des rendez-vous du docteur
   * Endpoint backend réel : GET /api/appointments/doctor?doctorId=<UUID>&from=<ISO>&to=<ISO>
   */
  getDoctorAppointments(doctorId: string, from?: Date, to?: Date): Observable<AppointmentResponseDto[]> {
    let params = new HttpParams().set('doctorId', doctorId);
    if (from) params = params.set('from', from.toISOString());
    if (to) params = params.set('to', to.toISOString());
    return this.http.get<AppointmentResponseDto[]>('/api/appointments/doctor', { params });
  }

  getDoctorCalendar(doctorId: string, date: string): Observable<CalendarSlotDto[]> {
    const httpParams = new HttpParams().set('date', date);
    return this.http.get<CalendarSlotDto[]>(`${this.doctorsBaseUrl}/${doctorId}/calendar`, { params: httpParams });
  }

  getDoctorAvailability(doctorId: string): Observable<DoctorAvailabilityDto[]> {
    return this.http.get<DoctorAvailabilityDto[]>(`${this.doctorsBaseUrl}/${doctorId}/availability`);
  }

  createDoctorAvailability(doctorId: string, payload: Omit<DoctorAvailabilityDto, 'id' | 'doctorId'>): Observable<DoctorAvailabilityDto> {
    return this.http.post<DoctorAvailabilityDto>(`${this.doctorsBaseUrl}/${doctorId}/availability`, payload);
  }

  updateDoctorAvailability(availabilityId: string, payload: Partial<DoctorAvailabilityDto>): Observable<DoctorAvailabilityDto> {
    return this.http.put<DoctorAvailabilityDto>(`${this.availabilityBaseUrl}/${availabilityId}`, payload);
  }

  disableDoctorAvailability(availabilityId: string): Observable<void> {
    return this.http.delete<void>(`${this.availabilityBaseUrl}/${availabilityId}`);
  }

  /**
   * Récupération des exceptions / indisponibilités du docteur
   * Endpoint backend réel : GET /api/doctors/{doctorId}/availability/exceptions
   */
  getDoctorExceptions(doctorId: string): Observable<AvailabilityExceptionDto[]> {
    return this.http.get<AvailabilityExceptionDto[]>(`/api/doctors/${doctorId}/availability/exceptions`);
  }

  createDoctorException(doctorId: string, payload: Omit<AvailabilityExceptionDto, 'id' | 'doctorId'>): Observable<AvailabilityExceptionDto> {
    return this.http.post<AvailabilityExceptionDto>(`${this.doctorsBaseUrl}/${doctorId}/exceptions`, payload);
  }

  updateDoctorException(exceptionId: string, payload: Partial<AvailabilityExceptionDto>): Observable<AvailabilityExceptionDto> {
    return this.http.put<AvailabilityExceptionDto>(`${this.exceptionsBaseUrl}/${exceptionId}`, payload);
  }

  disableDoctorException(exceptionId: string): Observable<void> {
    return this.http.delete<void>(`${this.exceptionsBaseUrl}/${exceptionId}`);
  }

  private shouldTryFallback(error: unknown): boolean {
    return error instanceof HttpErrorResponse && (error.status === 404 || error.status === 405);
  }
}
