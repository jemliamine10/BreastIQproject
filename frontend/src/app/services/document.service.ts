import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DocumentCountsDto,
  DocumentPageResponseDto,
  DocumentResponseDto,
  DocumentStatus,
  DocumentStatusUpdateDto,
  DocumentUploadDto
} from '../models/document.dto';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly API = '/api/v1';

  constructor(private readonly http: HttpClient) {}

  getPatientDocuments(
    patientId: string,
    page = 0,
    size = 9
  ): Observable<DocumentPageResponseDto> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<DocumentPageResponseDto>(
      `${this.API}/patient/${patientId}/documents`,
      { params }
    );
  }

  getPatientDocumentCounts(patientId: string): Observable<DocumentCountsDto> {
    return this.http.get<DocumentCountsDto>(`${this.API}/patient/${patientId}/documents/counts`);
  }

  uploadPatientDocument(
    patientId: string,
    file: File,
    metadata: DocumentUploadDto
  ): Observable<DocumentResponseDto> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append(
      'metadata',
      new Blob([JSON.stringify(metadata)], { type: 'application/json' })
    );

    return this.http.post<DocumentResponseDto>(
      `${this.API}/patient/${patientId}/documents/upload`,
      formData
    );
  }

  sharePatientDocument(
    patientId: string,
    documentId: string,
    doctorId: string
  ): Observable<void> {
    const params = new HttpParams().set('doctorId', doctorId);
    return this.http.post<void>(
      `${this.API}/patient/${patientId}/documents/${documentId}/share`,
      null,
      { params }
    );
  }

  downloadPatientDocument(documentId: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.API}/patient/documents/${documentId}/download`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  deletePatientDocument(documentId: string, requesterId: string): Observable<void> {
    const params = new HttpParams().set('requesterId', requesterId);
    return this.http.delete<void>(`${this.API}/patient/documents/${documentId}`, { params });
  }

  getDoctorPatientDocuments(
    doctorId: string,
    patientId: string,
    page = 0,
    size = 9
  ): Observable<DocumentPageResponseDto> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<DocumentPageResponseDto>(
      `${this.API}/doctor/${doctorId}/patients/${patientId}/documents`,
      { params }
    );
  }

  getDoctorPatientDocumentCounts(doctorId: string, patientId: string): Observable<DocumentCountsDto> {
    return this.http.get<DocumentCountsDto>(
      `${this.API}/doctor/${doctorId}/patients/${patientId}/documents/counts`
    );
  }

  uploadDoctorPatientDocument(
    doctorId: string,
    patientId: string,
    file: File,
    metadata: DocumentUploadDto
  ): Observable<DocumentResponseDto> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append(
      'metadata',
      new Blob([JSON.stringify(metadata)], { type: 'application/json' })
    );

    return this.http.post<DocumentResponseDto>(
      `${this.API}/doctor/${doctorId}/patients/${patientId}/documents/upload`,
      formData
    );
  }

  updateDoctorDocumentStatus(
    doctorId: string,
    documentId: string,
    status: DocumentStatus
  ): Observable<DocumentResponseDto> {
    const payload: DocumentStatusUpdateDto = { status };

    return this.http.patch<DocumentResponseDto>(
      `${this.API}/doctor/${doctorId}/documents/${documentId}/status`,
      payload
    );
  }

  downloadDoctorDocument(doctorId: string, documentId: string): Observable<HttpResponse<Blob>> {
    // Redirection temporaire vers le path patient qui est DÉJÀ chargé en mémoire sur le serveur !
    return this.http.get(`${this.API}/patient/documents/${documentId}/download`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  deleteDoctorDocument(doctorId: string, documentId: string): Observable<void> {
    // Redirection temporaire vers le path patient
    const params = new HttpParams().set('requesterId', doctorId);
    return this.http.delete<void>(`${this.API}/patient/documents/${documentId}`, { params });
  }
}
