import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DoctorDashboardDto } from '../models/doctor-dashboard.dto';

@Injectable({ providedIn: 'root' })
export class DoctorDashboardService {

  private baseUrl = '/api/doctor/dashboard';

  constructor(private http: HttpClient) {}

  getDashboard(doctorProfileId: string): Observable<DoctorDashboardDto> {
    return this.http.get<DoctorDashboardDto>(`${this.baseUrl}/${doctorProfileId}`);
  }
}
