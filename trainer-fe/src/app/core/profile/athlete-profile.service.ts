import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProfileRequest, ProfileResponse } from './profile.model';

@Injectable({ providedIn: 'root' })
export class AthleteProfileService {
  private readonly apiUrl = '/api/athlete-profile';

  constructor(private http: HttpClient) {}

  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(this.apiUrl);
  }

  createProfile(data: ProfileRequest): Observable<ProfileResponse> {
    return this.http.post<ProfileResponse>(this.apiUrl, data);
  }

  updateProfile(data: ProfileRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(this.apiUrl, data);
  }
}
