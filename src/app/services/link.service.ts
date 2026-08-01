import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LinkActionRequestDto,
  LinkRequestCreateDto,
  LinkResponseDto
} from '../models/links-appointments.dto';

@Injectable({
  providedIn: 'root'
})
export class LinkService {
  private readonly baseUrl = '/api/links';

  constructor(private readonly http: HttpClient) { }

  createRequest(payload: LinkRequestCreateDto): Observable<LinkResponseDto> {
    return this.http.post<LinkResponseDto>(`${this.baseUrl}/create-request`, payload);
  }

  getPending(actorType: 'patient' | 'doctor', actorId: string): Observable<LinkResponseDto[]> {
    const params = new HttpParams().set('actorType', actorType).set('actorId', actorId);
    return this.http.get<LinkResponseDto[]>(`${this.baseUrl}/pending`, { params });
  }

  getConnected(actorType: 'patient' | 'doctor', actorId: string): Observable<LinkResponseDto[]> {
    const params = new HttpParams().set('actorType', actorType).set('actorId', actorId);
    return this.http.get<LinkResponseDto[]>(`${this.baseUrl}/connected`, { params });
  }

  approve(payload: LinkActionRequestDto): Observable<LinkResponseDto> {
    return this.http.post<LinkResponseDto>(`${this.baseUrl}/approve`, payload);
  }

  refuse(payload: LinkActionRequestDto): Observable<LinkResponseDto> {
    return this.http.post<LinkResponseDto>(`${this.baseUrl}/refuse`, payload);
  }
}
