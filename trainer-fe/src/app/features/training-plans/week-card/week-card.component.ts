import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlanWeek, PlanWorkoutEntry } from '../../../core/training-plan/training-plan.models';

@Component({
  selector: 'app-week-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './week-card.component.html',
  styleUrl: './week-card.component.scss',
})
export class WeekCardComponent {
  @Input({ required: true }) week!: PlanWeek;
  @Input() expanded = false;
  @Output() expand = new EventEmitter<void>();

  private static readonly DAY_NAMES: Record<number, string> = {
    1: 'Monday',
    2: 'Tuesday',
    3: 'Wednesday',
    4: 'Thursday',
    5: 'Friday',
    6: 'Saturday',
    7: 'Sunday',
  };

  get workoutCount(): number {
    return this.week.workouts.length;
  }

  get distinctWorkoutTypes(): string {
    const names = new Set(this.week.workouts.map(w => w.workout.name));
    return Array.from(names).join(', ');
  }

  get sortedWorkouts(): PlanWorkoutEntry[] {
    return [...this.week.workouts].sort((a, b) => {
      if (a.dayOfWeek !== b.dayOfWeek) {
        return a.dayOfWeek - b.dayOfWeek;
      }
      return a.orderInDay - b.orderInDay;
    });
  }

  getDayName(dayOfWeek: number): string {
    return WeekCardComponent.DAY_NAMES[dayOfWeek] ?? '';
  }

  getWorkoutSummary(entry: PlanWorkoutEntry): string {
    const steps = entry.workout.numValidSteps;
    return `${steps} step${steps !== 1 ? 's' : ''}`;
  }

  onHeaderClick(): void {
    this.expand.emit();
  }
}
