import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { UserResponseDto } from '../models/user-response.dto';
import { LoginRequestDto } from '../models/login-request.dto';
import { LoginResponseDto } from '../models/login-response.dto';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = '/api';
  private currentUserSubject = new BehaviorSubject<UserResponseDto | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      this.currentUserSubject.next(JSON.parse(storedUser));
    }
  }

  login(credentials: LoginRequestDto): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>(`${this.API_URL}/auth/login`, credentials).pipe(
      tap(response => {
        // Note: The backend DTO does not return a token in the body.
        // Assuming cookie-based auth or token header handling is managed elsewhere/interceptor.
        // For compatibility with the app's User model, we create a compatible object.
        const user: any = {
            ...response,
            id: response.userId // Map userId to id for frontend compatibility
        };
        
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }

  get isLoggedIn(): boolean {
    return !!localStorage.getItem('currentUser');
  }

  get currentUser(): UserResponseDto | null {
    return this.currentUserSubject.value;
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  updateCurrentUser(user: UserResponseDto): void {
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
}
