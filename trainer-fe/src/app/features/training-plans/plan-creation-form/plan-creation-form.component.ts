import { Component, EventEmitter, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { MultiSelect } from 'primeng/multiselect';
import { DatePicker } from 'primeng/datepicker';
import { ButtonModule } from 'primeng/button';
import { Toast } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { TrainingPlanService } from '../../../core/training-plan/training-plan.service';
import { CreatePlanRequest, AiModel, PlanDistance, PlanDuration } from '../../../core/training-plan/training-plan.models';
import { PaceFormatUtils } from '../../../core/training-plan/pace-format.utils';

interface SelectOption<T = string> {
  label: string;
  value: T;
}

@Component({
  selector: 'app-plan-creation-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Dialog,
    InputText,
    Select,
    MultiSelect,
    DatePicker,
    ButtonModule,
    Toast,
  ],
  providers: [MessageService],
  templateUrl: './plan-creation-form.component.html',
  styleUrl: './plan-creation-form.component.scss',
})
export class PlanCreationFormComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() planCreated = new EventEmitter<void>();

  loading = signal(false);

  form!: FormGroup;

  distanceOptions: SelectOption<PlanDistance>[] = [
    { label: '5K', value: 'FIVE_K' },
    { label: '10K', value: 'TEN_K' },
    { label: 'Half Marathon', value: 'HALF_MARATHON' },
    { label: 'Marathon', value: 'MARATHON' },
  ];

  durationOptions: SelectOption<PlanDuration>[] = [
    { label: '8 Weeks', value: 'WEEKS_8' },
    { label: '10 Weeks', value: 'WEEKS_10' },
    { label: '12 Weeks', value: 'WEEKS_12' },
  ];

  aiModelOptions: SelectOption<AiModel>[] = [
    { label: 'ChatGPT', value: 'CHATGPT' },
    { label: 'Claude', value: 'CLAUDE' },
    { label: 'Gemini', value: 'GEMINI' },
    { label: 'Kiro', value: 'KIRO' },
    { label: 'Dummy', value: 'DUMMY' },
  ];

  trainingDayOptions: SelectOption<number>[] = [
    { label: 'Monday', value: 1 },
    { label: 'Tuesday', value: 2 },
    { label: 'Wednesday', value: 3 },
    { label: 'Thursday', value: 4 },
    { label: 'Friday', value: 5 },
    { label: 'Saturday', value: 6 },
    { label: 'Sunday', value: 7 },
  ];

  longRunDayOptions: SelectOption<number>[] = [];

  minDate: Date = new Date();

  constructor(
    private trainingPlanService: TrainingPlanService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    // Set minDate to tomorrow
    this.minDate = new Date();
    this.minDate.setDate(this.minDate.getDate() + 1);
    this.minDate.setHours(0, 0, 0, 0);

    this.initForm();
  }

  private initForm(): void {
    this.form = new FormGroup({
      eventName: new FormControl('', [Validators.required, Validators.maxLength(100)]),
      distance: new FormControl<PlanDistance | null>(null, [Validators.required]),
      duration: new FormControl<PlanDuration | null>(null, [Validators.required]),
      raceDate: new FormControl<Date | null>(null, [Validators.required]),
      targetPace: new FormControl('', [Validators.required]),
      aiModel: new FormControl<AiModel | null>(null, [Validators.required]),
      trainingDays: new FormControl<number[]>([], [Validators.required]),
      longRunDay: new FormControl<number | null>(null, [Validators.required]),
    });
  }

  onTrainingDaysChange(): void {
    const selectedDays: number[] = this.form.get('trainingDays')?.value || [];

    // Update longRunDay options to only show selected training days
    this.longRunDayOptions = this.trainingDayOptions.filter(
      (option) => selectedDays.includes(option.value),
    );

    // Clear longRunDay if it's no longer in the selection
    const currentLongRunDay = this.form.get('longRunDay')?.value;
    if (currentLongRunDay && !selectedDays.includes(currentLongRunDay)) {
      this.form.get('longRunDay')?.setValue(null);
    }
  }

  isLongRunDayDisabled(): boolean {
    const trainingDays: number[] = this.form.get('trainingDays')?.value || [];
    return trainingDays.length === 0;
  }

  onSubmit(): void {
    // Mark all fields as touched to show validation errors
    this.form.markAllAsTouched();

    // Validate pace
    const paceValue = this.form.get('targetPace')?.value;
    if (paceValue && !PaceFormatUtils.isValidPace(paceValue)) {
      // Pace is invalid — form should not submit
      return;
    }

    // Validate trainingDays has at least 1
    const trainingDays: number[] = this.form.get('trainingDays')?.value || [];
    if (trainingDays.length === 0) {
      return;
    }

    if (this.form.invalid) {
      return;
    }

    this.loading.set(true);

    const paceSeconds = PaceFormatUtils.paceToSeconds(paceValue);
    if (paceSeconds === null) {
      this.loading.set(false);
      return;
    }

    const raceDate: Date = this.form.get('raceDate')?.value;
    const raceDateStr = this.formatDateToISO(raceDate);

    const request: CreatePlanRequest = {
      eventName: this.form.get('eventName')?.value.trim(),
      distance: this.form.get('distance')?.value,
      duration: this.form.get('duration')?.value,
      raceDate: raceDateStr,
      targetPaceSecondsPerKm: paceSeconds,
      aiModel: this.form.get('aiModel')?.value,
      trainingDays: trainingDays,
      longRunDay: this.form.get('longRunDay')?.value,
    };

    this.trainingPlanService.createPlan(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Training plan created successfully',
        });
        this.closeDialog();
        this.planCreated.emit();
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const message = error.error?.message || 'Failed to create training plan';
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: message,
        });
      },
    });
  }

  closeDialog(): void {
    this.form.reset();
    this.longRunDayOptions = [];
    this.visible = false;
    this.visibleChange.emit(false);
  }

  isPaceInvalid(): boolean {
    const control = this.form.get('targetPace');
    if (!control || !control.touched || !control.value) {
      return false;
    }
    return !PaceFormatUtils.isValidPace(control.value);
  }

  getPaceErrorMessage(): string {
    const control = this.form.get('targetPace');
    if (!control || !control.value) {
      return 'Target pace is required';
    }
    const seconds = PaceFormatUtils.paceToSeconds(control.value);
    if (seconds === null) {
      return 'Enter pace in MM:SS format (e.g., 5:00)';
    }
    if (seconds < 150 || seconds > 900) {
      return 'Pace must be between 2:30 and 15:00 per km';
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    return !!control && control.invalid && control.touched;
  }

  private formatDateToISO(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
