import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  AvailabilityException,
  CalendarSlot,
  DoctorAvailability
} from '../models/appointment.model';

@Injectable({
  providedIn: 'root'
})
export class DoctorCalendarService {
  private readonly doctorsBaseUrl = '/api/doctors';
  private readonly availabilityBaseUrl = '/api/availability';
  private readonly exceptionsBaseUrl = '/api/exceptions';

  constructor(private readonly http: HttpClient) {}

  getCalendarSlots(doctorId: string, date: string): Observable<CalendarSlot[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<CalendarSlot[]>(`${this.doctorsBaseUrl}/${doctorId}/calendar`, { params });
  }

  getDoctorAvailability(doctorId: string): Observable<DoctorAvailability[]> {
    return this.http.get<DoctorAvailability[]>(`${this.doctorsBaseUrl}/${doctorId}/availability`);
  }

  createDoctorAvailability(
    doctorId: string,
    payload: Omit<DoctorAvailability, 'id' | 'doctorId'>
  ): Observable<DoctorAvailability> {
    return this.http.post<DoctorAvailability>(`${this.doctorsBaseUrl}/${doctorId}/availability`, payload);
  }

  updateDoctorAvailability(
    availabilityId: string,
    payload: Partial<DoctorAvailability>
  ): Observable<DoctorAvailability> {
    return this.http.put<DoctorAvailability>(`${this.availabilityBaseUrl}/${availabilityId}`, payload);
  }

  disableDoctorAvailability(availabilityId: string): Observable<void> {
    return this.http.delete<void>(`${this.availabilityBaseUrl}/${availabilityId}`);
  }

  getDoctorExceptions(doctorId: string): Observable<AvailabilityException[]> {
    const byDoctorPath = `${this.exceptionsBaseUrl}/doctor/${doctorId}`;
    const byDoctorQuery = `${this.exceptionsBaseUrl}/doctor`;

    return this.http.get<AvailabilityException[]>(`${this.doctorsBaseUrl}/${doctorId}/exceptions`).pipe(
      catchError((error: unknown) => {
        if (!this.shouldTryFallback(error)) {
          return throwError(() => error);
        }

        return this.http.get<AvailabilityException[]>(byDoctorPath).pipe(
          catchError((secondError: unknown) => {
            if (!this.shouldTryFallback(secondError)) {
              return throwError(() => secondError);
            }

            const fallbackParams = new HttpParams().set('doctorId', doctorId);
            return this.http.get<AvailabilityException[]>(byDoctorQuery, { params: fallbackParams });
          })
        );
      })
    );
  }

  createDoctorException(
    doctorId: string,
    payload: Omit<AvailabilityException, 'id' | 'doctorId'>
  ): Observable<AvailabilityException> {
    return this.http.post<AvailabilityException>(`${this.doctorsBaseUrl}/${doctorId}/exceptions`, payload);
  }

  updateDoctorException(
    exceptionId: string,
    payload: Partial<AvailabilityException>
  ): Observable<AvailabilityException> {
    return this.http.put<AvailabilityException>(`${this.exceptionsBaseUrl}/${exceptionId}`, payload);
  }

  disableDoctorException(exceptionId: string): Observable<void> {
    return this.http.delete<void>(`${this.exceptionsBaseUrl}/${exceptionId}`);
  }

  private shouldTryFallback(error: unknown): boolean {
    return error instanceof HttpErrorResponse && (error.status === 404 || error.status === 405);
  }
}
