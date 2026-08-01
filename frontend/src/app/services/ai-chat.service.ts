import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AiChatRequest, AiChatResponse } from '../models/ai-chat.models';

@Injectable({
  providedIn: 'root'
})
export class AiChatService {
  private readonly API_URL = '/api/chat';

  constructor(private http: HttpClient) {}

  sendMessage(request: AiChatRequest): Observable<AiChatResponse> {
    return this.http.post<AiChatResponse>(this.API_URL, request);
  }
}