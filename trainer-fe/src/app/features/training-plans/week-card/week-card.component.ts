import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlanWeek, PlanWorkoutEntry, WorkoutStep } from '../../../core/training-plan/training-plan.models';

interface RepeatGroup {
  fromIndex: number; // 0-based index of first repeated step
  toIndex: number;   // 0-based index of the repeat step itself (exclusive of repeated block)
  iterations: number;
}

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
  @Input() weekStartDate: string | null = null;
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

  get formattedDateRange(): string {
    if (!this.weekStartDate) return '';
    const start = new Date(this.weekStartDate);
    const end = new Date(start);
    end.setDate(end.getDate() + 6);
    return `${this.formatDate(start)} – ${this.formatDate(end)}`;
  }

  getDayName(dayOfWeek: number): string {
    return WeekCardComponent.DAY_NAMES[dayOfWeek] ?? '';
  }

  /**
   * Finds all repeat groups in a workout's steps.
   * A repeat group is defined by a step with durationType=REPEAT_UNTIL_STEPS_COMPLETE.
   */
  getRepeatGroups(steps: WorkoutStep[]): RepeatGroup[] {
    const groups: RepeatGroup[] = [];
    for (let i = 0; i < steps.length; i++) {
      const step = steps[i];
      if (step.durationType === 'REPEAT_UNTIL_STEPS_COMPLETE') {
        groups.push({
          fromIndex: step.durationValue ?? 0,
          toIndex: i,
          iterations: step.targetValueLow ?? 1,
        });
      }
    }
    return groups;
  }

  /**
   * Checks if a step at the given index is inside a repeat group (but not the repeat step itself).
   */
  isInsideRepeatGroup(steps: WorkoutStep[], index: number): boolean {
    const groups = this.getRepeatGroups(steps);
    return groups.some(g => index >= g.fromIndex && index < g.toIndex);
  }

  /**
   * Checks if a step is the repeat step itself.
   */
  isRepeatStep(step: WorkoutStep): boolean {
    return step.durationType === 'REPEAT_UNTIL_STEPS_COMPLETE';
  }

  /**
   * Gets the repeat header text for a repeat step.
   */
  getRepeatLabel(step: WorkoutStep): string {
    const iterations = step.targetValueLow ?? 1;
    return `Repeat ×${iterations}`;
  }

  formatStepDuration(step: WorkoutStep): string {
    if (step.durationValue == null) return 'Open';
    if (step.durationType === 'REPEAT_UNTIL_STEPS_COMPLETE') {
      return ''; // handled by getRepeatLabel
    }
    if (step.durationType === 'TIME') {
      const minutes = Math.floor(step.durationValue / 60);
      const seconds = step.durationValue % 60;
      if (seconds === 0) return `${minutes} min`;
      return `${minutes}:${seconds.toString().padStart(2, '0')} min`;
    }
    if (step.durationType === 'DISTANCE') {
      if (step.durationValue >= 1000) {
        const km = step.durationValue / 1000;
        return km % 1 === 0 ? `${km} km` : `${km.toFixed(1)} km`;
      }
      return `${step.durationValue} m`;
    }
    return step.durationType;
  }

  formatStepTarget(step: WorkoutStep): string {
    if (step.durationType === 'REPEAT_UNTIL_STEPS_COMPLETE') return '';
    if (step.targetType === 'OPEN') return '';
    if (step.targetType === 'HEART_RATE') {
      if (step.targetValueLow != null && step.targetValueHigh != null) {
        return `HR ${step.targetValueLow}–${step.targetValueHigh} bpm`;
      }
      return 'HR zone';
    }
    if (step.targetType === 'SPEED') {
      if (step.targetValueLow != null && step.targetValueHigh != null) {
        return `Pace ${this.secondsToPace(step.targetValueLow)}–${this.secondsToPace(step.targetValueHigh)}/km`;
      }
      return 'Speed target';
    }
    if (step.targetType === 'CADENCE') {
      if (step.targetValueLow != null && step.targetValueHigh != null) {
        return `${step.targetValueLow}–${step.targetValueHigh} spm`;
      }
      return 'Cadence target';
    }
    if (step.targetType === 'POWER') {
      if (step.targetValueLow != null && step.targetValueHigh != null) {
        return `${step.targetValueLow}–${step.targetValueHigh} W`;
      }
      return 'Power target';
    }
    return '';
  }

  formatIntensity(intensity: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Active',
      REST: 'Rest',
      WARMUP: 'Warm Up',
      COOLDOWN: 'Cool Down',
      RECOVERY: 'Recovery',
      INTERVAL: 'Interval',
    };
    return map[intensity] ?? intensity;
  }

  getIntensityClass(intensity: string): string {
    return `intensity--${intensity.toLowerCase()}`;
  }

  onHeaderClick(): void {
    this.expand.emit();
  }

  private formatDate(date: Date): string {
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    return `${day}/${month}`;
  }

  private secondsToPace(seconds: number): string {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  }
}
