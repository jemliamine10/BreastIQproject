import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Availability, CreateAvailabilityDto, UpdateAvailabilityDto } from '../models/availability.model';

interface ApiErrorPayload {
  message?: string;
  error?: string;
  details?: unknown;
}

@Injectable({
  providedIn: 'root'
})
export class AvailabilityService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) { }

  getAvailabilities(doctorId: string): Observable<Availability[]> {
    return this.http.get<Availability[]>(`${this.apiUrl}/doctors/${doctorId}/availability`)
      .pipe(
        catchError(this.handleError)
      );
  }

  createAvailability(doctorId: string, data: CreateAvailabilityDto): Observable<Availability> {
    return this.http.post<Availability>(`${this.apiUrl}/doctors/${doctorId}/availability`, data)
      .pipe(
        catchError(this.handleError)
      );
  }

  updateAvailability(availabilityId: string, data: UpdateAvailabilityDto): Observable<Availability> {
    return this.http.put<Availability>(`${this.apiUrl}/availability/${availabilityId}`, data)
      .pipe(
        catchError(this.handleError)
      );
  }

  deleteAvailability(availabilityId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/availability/${availabilityId}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse) {
    if (error.error instanceof ErrorEvent) {
      return throwError(() => error.error.message || 'Erreur reseau.');
    }

    const payload = error.error as ApiErrorPayload | string | null;
    if (typeof payload === 'string' && payload.trim()) {
      return throwError(() => payload);
    }

    if (payload && typeof payload === 'object') {
      if (payload.message?.trim()) {
        return throwError(() => payload.message as string);
      }
      if (payload.error?.trim()) {
        return throwError(() => payload.error as string);
      }
    }

    if (error.status === 400) {
      return throwError(() => 'Requete invalide. Verifiez les donnees envoyees.');
    }

    if (error.status === 404) {
      return throwError(() => 'Ressource introuvable.');
    }

    if (error.status === 0) {
      return throwError(() => 'Serveur inaccessible. Verifiez la connexion.');
    }

    return throwError(() => 'Une erreur inattendue est survenue.');
  }
}
