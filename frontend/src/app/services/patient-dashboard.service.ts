import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PatientDashboardDto } from '../models/patient-dashboard.dto';

@Injectable({ providedIn: 'root' })
export class PatientDashboardService {

  private baseUrl = '/api/patient/dashboard';

  constructor(private http: HttpClient) {}

  getDashboard(patientProfileId: string): Observable<PatientDashboardDto> {
    return this.http.get<PatientDashboardDto>(`${this.baseUrl}/${patientProfileId}`);
  }
}
