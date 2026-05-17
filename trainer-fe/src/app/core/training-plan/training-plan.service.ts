import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatePlanRequest, TrainingPlanDetail, TrainingPlanSummary } from './training-plan.models';

@Injectable({ providedIn: 'root' })
export class TrainingPlanService {
  private readonly apiUrl = '/api/training-plans';

  constructor(private http: HttpClient) {}

  getPlans(): Observable<TrainingPlanSummary[]> {
    return this.http.get<TrainingPlanSummary[]>(this.apiUrl);
  }

  getPlan(id: string): Observable<TrainingPlanDetail> {
    return this.http.get<TrainingPlanDetail>(`${this.apiUrl}/${id}`);
  }

  getActivePlan(): Observable<TrainingPlanDetail> {
    return this.http.get<TrainingPlanDetail>(`${this.apiUrl}/active`);
  }

  createPlan(request: CreatePlanRequest): Observable<TrainingPlanSummary> {
    return this.http.post<TrainingPlanSummary>(this.apiUrl, request);
  }

  activatePlan(id: string): Observable<TrainingPlanSummary> {
    return this.http.put<TrainingPlanSummary>(`${this.apiUrl}/${id}/activate`, null);
  }

  completePlan(id: string): Observable<TrainingPlanSummary> {
    return this.http.put<TrainingPlanSummary>(`${this.apiUrl}/${id}/complete`, null);
  }

  terminatePlan(id: string): Observable<TrainingPlanSummary> {
    return this.http.put<TrainingPlanSummary>(`${this.apiUrl}/${id}/terminate`, null);
  }

  deletePlan(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
