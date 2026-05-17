import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { ProgressSpinner } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import { TrainingPlanService } from '../../../core/training-plan/training-plan.service';
import { PaceFormatUtils } from '../../../core/training-plan/pace-format.utils';
import { TrainingPlanDetail } from '../../../core/training-plan/training-plan.models';
import { WeekCardComponent } from '../week-card/week-card.component';

@Component({
  selector: 'app-active-plan-view',
  standalone: true,
  imports: [
    CommonModule,
    ProgressSpinner,
    Toast,
    ButtonModule,
    WeekCardComponent,
  ],
  providers: [MessageService],
  templateUrl: './active-plan-view.component.html',
  styleUrl: './active-plan-view.component.scss',
})
export class ActivePlanViewComponent implements OnInit {
  @Output() createPlanRequested = new EventEmitter<void>();

  plan = signal<TrainingPlanDetail | null>(null);
  loading = signal(true);
  noPlanExists = signal(false);
  expandedWeek = signal<number | null>(null);

  constructor(
    private trainingPlanService: TrainingPlanService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.loadActivePlan();
  }

  loadActivePlan(): void {
    this.loading.set(true);
    this.noPlanExists.set(false);

    this.trainingPlanService.getActivePlan().subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.status === 404) {
          this.noPlanExists.set(true);
        } else {
          const message = error.error?.message || 'Failed to load active plan';
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: message,
          });
        }
      },
    });
  }

  get formattedRaceDate(): string {
    const plan = this.plan();
    if (!plan) return '';
    const [year, month, day] = plan.raceDate.split('-');
    return `${day}/${month}/${year}`;
  }

  get formattedPace(): string {
    const plan = this.plan();
    if (!plan) return '';
    return PaceFormatUtils.secondsToPace(plan.targetPaceSecondsPerKm);
  }

  get formattedTrainingDays(): string {
    const plan = this.plan();
    if (!plan) return '';
    return plan.trainingDays
      .map((day) => this.dayNumberToShortName(day))
      .join(', ');
  }

  get formattedLongRunDay(): string {
    const plan = this.plan();
    if (!plan) return '';
    return this.dayNumberToFullName(plan.longRunDay);
  }

  get formattedDistance(): string {
    const plan = this.plan();
    if (!plan) return '';
    return this.distanceToDisplay(plan.distance);
  }

  get formattedDuration(): string {
    const plan = this.plan();
    if (!plan) return '';
    return this.durationToDisplay(plan.duration);
  }

  toggleWeek(weekNumber: number): void {
    if (this.expandedWeek() === weekNumber) {
      this.expandedWeek.set(null);
    } else {
      this.expandedWeek.set(weekNumber);
    }
  }

  onCreatePlan(): void {
    this.createPlanRequested.emit();
  }

  private dayNumberToShortName(day: number): string {
    const names: Record<number, string> = {
      1: 'Mon',
      2: 'Tue',
      3: 'Wed',
      4: 'Thu',
      5: 'Fri',
      6: 'Sat',
      7: 'Sun',
    };
    return names[day] || '';
  }

  private dayNumberToFullName(day: number): string {
    const names: Record<number, string> = {
      1: 'Monday',
      2: 'Tuesday',
      3: 'Wednesday',
      4: 'Thursday',
      5: 'Friday',
      6: 'Saturday',
      7: 'Sunday',
    };
    return names[day] || '';
  }

  private distanceToDisplay(distance: string): string {
    const map: Record<string, string> = {
      FIVE_K: '5K',
      TEN_K: '10K',
      HALF_MARATHON: 'Half Marathon',
      MARATHON: 'Marathon',
    };
    return map[distance] || distance;
  }

  private durationToDisplay(duration: string): string {
    const map: Record<string, string> = {
      WEEKS_8: '8 Weeks',
      WEEKS_10: '10 Weeks',
      WEEKS_12: '12 Weeks',
    };
    return map[duration] || duration;
  }
}
