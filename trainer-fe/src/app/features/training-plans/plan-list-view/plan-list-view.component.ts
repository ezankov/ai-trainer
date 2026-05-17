import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { Tag } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Toast } from 'primeng/toast';
import { ProgressSpinner } from 'primeng/progressspinner';
import { ConfirmationService, MessageService } from 'primeng/api';

import { TrainingPlanService } from '../../../core/training-plan/training-plan.service';
import { TrainingPlanSummary, PlanState } from '../../../core/training-plan/training-plan.models';

@Component({
  selector: 'app-plan-list-view',
  standalone: true,
  imports: [
    CommonModule,
    Tag,
    ButtonModule,
    ConfirmDialog,
    Toast,
    ProgressSpinner,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './plan-list-view.component.html',
  styleUrl: './plan-list-view.component.scss',
})
export class PlanListViewComponent implements OnInit {
  @Output() createPlanRequested = new EventEmitter<void>();
  @Output() planActivated = new EventEmitter<void>();

  plans = signal<TrainingPlanSummary[]>([]);
  loading = signal(true);
  loadingPlanIds = signal<Set<string>>(new Set());

  constructor(
    private trainingPlanService: TrainingPlanService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.loading.set(true);

    this.trainingPlanService.getPlans().subscribe({
      next: (plans) => {
        const nonActivePlans = plans
          .filter((p) => p.state !== 'ACTIVE')
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.plans.set(nonActivePlans);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const message = error.error?.message || 'Failed to load plans';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: message,
        });
      },
    });
  }

  getStateSeverity(state: PlanState): 'info' | 'success' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (state) {
      case 'NEW':
        return 'info';
      case 'COMPLETED':
        return 'success';
      case 'TERMINATED':
        return 'warn';
      default:
        return 'secondary';
    }
  }

  isActivateDisabled(plan: TrainingPlanSummary): boolean {
    return plan.state === 'TERMINATED' || this.isPlanLoading(plan.id);
  }

  isDeleteDisabled(plan: TrainingPlanSummary): boolean {
    return this.isPlanLoading(plan.id);
  }

  isPlanLoading(planId: string): boolean {
    return this.loadingPlanIds().has(planId);
  }

  confirmActivate(plan: TrainingPlanSummary): void {
    this.confirmationService.confirm({
      message: 'Activating this plan will terminate your current active plan. Do you want to continue?',
      header: 'Activate Plan',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Activate',
      rejectLabel: 'Cancel',
      accept: () => this.activatePlan(plan),
    });
  }

  confirmDelete(plan: TrainingPlanSummary): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete "${plan.eventName}"? This action cannot be undone.`,
      header: 'Delete Plan',
      icon: 'pi pi-trash',
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deletePlan(plan),
    });
  }

  onCreatePlan(): void {
    this.createPlanRequested.emit();
  }

  formatDistance(distance: string): string {
    const map: Record<string, string> = {
      FIVE_K: '5K',
      TEN_K: '10K',
      HALF_MARATHON: 'Half Marathon',
      MARATHON: 'Marathon',
    };
    return map[distance] || distance;
  }

  formatDuration(duration: string): string {
    const map: Record<string, string> = {
      WEEKS_8: '8 Weeks',
      WEEKS_10: '10 Weeks',
      WEEKS_12: '12 Weeks',
    };
    return map[duration] || duration;
  }

  formatRaceDate(raceDate: string): string {
    const [year, month, day] = raceDate.split('-');
    return `${day}/${month}/${year}`;
  }

  private activatePlan(plan: TrainingPlanSummary): void {
    this.addLoadingPlan(plan.id);

    this.trainingPlanService.activatePlan(plan.id).subscribe({
      next: (activatedPlan) => {
        this.removeLoadingPlan(plan.id);

        // Remove the activated plan from the list
        const currentPlans = this.plans().filter((p) => p.id !== plan.id);

        // If there was a previously active plan, it's now terminated — refresh the list
        // to pick up the terminated plan. The activate response returns the activated plan,
        // but we need to refresh to get the previously active plan's new state.
        this.trainingPlanService.getPlans().subscribe({
          next: (allPlans) => {
            const nonActivePlans = allPlans
              .filter((p) => p.state !== 'ACTIVE')
              .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
            this.plans.set(nonActivePlans);
          },
          error: () => {
            // If refresh fails, just remove the activated plan from the current list
            this.plans.set(currentPlans);
          },
        });

        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `"${plan.eventName}" has been activated`,
        });
        this.planActivated.emit();
      },
      error: (error: HttpErrorResponse) => {
        this.removeLoadingPlan(plan.id);
        const message = error.error?.message || 'Failed to activate plan';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: message,
        });
      },
    });
  }

  private deletePlan(plan: TrainingPlanSummary): void {
    this.addLoadingPlan(plan.id);

    this.trainingPlanService.deletePlan(plan.id).subscribe({
      next: () => {
        this.removeLoadingPlan(plan.id);
        this.plans.set(this.plans().filter((p) => p.id !== plan.id));
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `"${plan.eventName}" has been deleted`,
        });
      },
      error: (error: HttpErrorResponse) => {
        this.removeLoadingPlan(plan.id);
        const message = error.error?.message || 'Failed to delete plan';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: message,
        });
      },
    });
  }

  private addLoadingPlan(planId: string): void {
    const current = new Set(this.loadingPlanIds());
    current.add(planId);
    this.loadingPlanIds.set(current);
  }

  private removeLoadingPlan(planId: string): void {
    const current = new Set(this.loadingPlanIds());
    current.delete(planId);
    this.loadingPlanIds.set(current);
  }
}
