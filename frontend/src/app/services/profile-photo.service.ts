import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ProfilePhotoService {
  private readonly API = '/api/profile-photos';

  constructor(private http: HttpClient) {}

  /**
   * Upload a profile photo for a user.
   * Returns the URL of the uploaded photo.
   */
  uploadPhoto(userId: string, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.API}/upload/${userId}`, formData)
      .pipe(map(res => res.url));
  }

  /**
   * Delete the profile photo for a user.
   */
  deletePhoto(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${userId}`);
  }

  /**
   * Get the URL to display a user's profile photo.
   * Handles both raw IDs and full paths, and appends a stable cache-busting timestamp.
   */
  getPhotoUrl(idOrPath: string): string {
    if (!idOrPath) return '';
    
    let path = idOrPath;
    if (!path.startsWith('/api') && !path.startsWith('http')) {
      path = `${this.API}/${idOrPath}`;
    }
    
    // Use a stable timestamp (changes every 30 seconds) to avoid NG0100 errors 
    // while still providing recent cache busting.
    const t = Math.floor(Date.now() / 30000);
    const separator = path.includes('?') ? '&' : '?';
    return `${path}${separator}t=${t}`;
  }
}
