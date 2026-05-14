import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputMask } from 'primeng/inputmask';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Toast } from 'primeng/toast';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Message } from 'primeng/message';
import { MessageService } from 'primeng/api';

import { AthleteProfileService } from '../../core/profile/athlete-profile.service';
import { ProfileResponse } from '../../core/profile/profile.model';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePicker,
    InputNumber,
    InputMask,
    ButtonModule,
    TableModule,
    Toast,
    ProgressSpinner,
    Message,
  ],
  providers: [MessageService],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
})
export default class ProfilePageComponent implements OnInit {
  profileForm!: FormGroup;
  loading = true;
  submitting = false;
  isEditMode = false;
  profile: ProfileResponse | null = null;

  maxDate: Date;

  constructor(
    private fb: FormBuilder,
    private profileService: AthleteProfileService,
    private messageService: MessageService
  ) {
    // Max date for date picker: today minus 13 years
    const today = new Date();
    this.maxDate = new Date(today.getFullYear() - 13, today.getMonth(), today.getDate());
  }

  ngOnInit(): void {
    this.initForm();
    this.loadProfile();
  }

  private initForm(): void {
    this.profileForm = this.fb.group(
      {
        dateOfBirth: [null, [Validators.required]],
        weightKg: [null, [Validators.required, Validators.min(20), Validators.max(300)]],
        restingHR: [null, [Validators.required, Validators.min(25), Validators.max(120)]],
        maxHR: [null, [Validators.required, Validators.min(100), Validators.max(250)]],
        lthr: [null, [Validators.min(100), Validators.max(250)]],
        thresholdPace: [null],
        vo2Max: [null, [Validators.min(20), Validators.max(90)]],
        fiveKTime: [null],
        tenKTime: [null],
        halfMarathonTime: [null],
        marathonTime: [null],
      },
      { validators: [this.crossFieldValidator.bind(this)] }
    );
  }

  private loadProfile(): void {
    this.loading = true;
    this.profileService.getProfile().subscribe({
      next: (response) => {
        this.profile = response;
        this.isEditMode = true;
        this.populateForm(response);
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.isEditMode = false;
        }
        this.loading = false;
      },
    });
  }

  private populateForm(profile: ProfileResponse): void {
    this.profileForm.patchValue({
      dateOfBirth: new Date(profile.dateOfBirth + 'T00:00:00'),
      weightKg: profile.weightKg,
      restingHR: profile.restingHR,
      maxHR: profile.maxHR,
      lthr: profile.lthr,
      thresholdPace: profile.thresholdPaceSecondsPerKm
        ? this.secondsToMMSS(profile.thresholdPaceSecondsPerKm)
        : null,
      vo2Max: profile.vo2Max,
      fiveKTime: profile.fiveKSeconds ? this.secondsToHHMMSS(profile.fiveKSeconds) : null,
      tenKTime: profile.tenKSeconds ? this.secondsToHHMMSS(profile.tenKSeconds) : null,
      halfMarathonTime: profile.halfMarathonSeconds
        ? this.secondsToHHMMSS(profile.halfMarathonSeconds)
        : null,
      marathonTime: profile.marathonSeconds
        ? this.secondsToHHMMSS(profile.marathonSeconds)
        : null,
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const payload = this.buildPayload();

    const request$ = this.isEditMode
      ? this.profileService.updateProfile(payload)
      : this.profileService.createProfile(payload);

    request$.subscribe({
      next: (response) => {
        this.submitting = false;
        this.profile = response;
        this.isEditMode = true;
        this.populateForm(response);
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: this.isEditMode ? 'Profile updated successfully.' : 'Profile created successfully.',
          life: 5000,
        });
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        let detail = 'Something went wrong. Please try again.';
        if (err.status === 409) {
          detail = 'Profile already exists.';
        } else if (err.status === 400 && err.error?.message) {
          detail = err.error.message;
        }
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail,
          sticky: true,
        });
      },
    });
  }

  private buildPayload(): any {
    const form = this.profileForm.value;
    const dateOfBirth = form.dateOfBirth
      ? this.formatDateToISO(form.dateOfBirth)
      : null;

    return {
      dateOfBirth,
      weightKg: form.weightKg,
      restingHR: form.restingHR,
      maxHR: form.maxHR,
      lthr: form.lthr || null,
      thresholdPaceSecondsPerKm: form.thresholdPace
        ? this.mmssToSeconds(form.thresholdPace)
        : null,
      vo2Max: form.vo2Max || null,
      fiveKSeconds: form.fiveKTime ? this.hhmmssToSeconds(form.fiveKTime) : null,
      tenKSeconds: form.tenKTime ? this.hhmmssToSeconds(form.tenKTime) : null,
      halfMarathonSeconds: form.halfMarathonTime
        ? this.hhmmssToSeconds(form.halfMarathonTime)
        : null,
      marathonSeconds: form.marathonTime
        ? this.hhmmssToSeconds(form.marathonTime)
        : null,
    };
  }

  // Cross-field validator
  crossFieldValidator(group: AbstractControl): ValidationErrors | null {
    const errors: ValidationErrors = {};
    const restingHR = group.get('restingHR')?.value;
    const maxHR = group.get('maxHR')?.value;
    const lthr = group.get('lthr')?.value;

    if (restingHR != null && maxHR != null && maxHR <= restingHR) {
      errors['maxHRNotGreaterThanResting'] = true;
    }

    if (lthr != null && restingHR != null && lthr <= restingHR) {
      errors['lthrNotGreaterThanResting'] = true;
    }

    if (lthr != null && maxHR != null && lthr > maxHR) {
      errors['lthrExceedsMaxHR'] = true;
    }

    // Race time ordering
    const fiveK = group.get('fiveKTime')?.value
      ? this.hhmmssToSeconds(group.get('fiveKTime')!.value)
      : null;
    const tenK = group.get('tenKTime')?.value
      ? this.hhmmssToSeconds(group.get('tenKTime')!.value)
      : null;
    const half = group.get('halfMarathonTime')?.value
      ? this.hhmmssToSeconds(group.get('halfMarathonTime')!.value)
      : null;
    const marathon = group.get('marathonTime')?.value
      ? this.hhmmssToSeconds(group.get('marathonTime')!.value)
      : null;

    if (fiveK != null && tenK != null && tenK <= fiveK) {
      errors['tenKNotGreaterThan5K'] = true;
    }
    if (tenK != null && half != null && half <= tenK) {
      errors['halfNotGreaterThan10K'] = true;
    }
    if (half != null && marathon != null && marathon <= half) {
      errors['marathonNotGreaterThanHalf'] = true;
    }

    return Object.keys(errors).length > 0 ? errors : null;
  }

  // Time formatting utilities
  secondsToMMSS(totalSeconds: number): string {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  mmssToSeconds(mmss: string): number | null {
    if (!mmss) return null;
    const clean = mmss.replace(/_/g, '');
    const parts = clean.split(':');
    if (parts.length !== 2) return null;
    const minutes = parseInt(parts[0], 10);
    const seconds = parseInt(parts[1], 10);
    if (isNaN(minutes) || isNaN(seconds)) return null;
    return minutes * 60 + seconds;
  }

  secondsToHHMMSS(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  hhmmssToSeconds(hhmmss: string): number | null {
    if (!hhmmss) return null;
    const clean = hhmmss.replace(/_/g, '');
    const parts = clean.split(':');
    if (parts.length === 3) {
      const h = parseInt(parts[0], 10);
      const m = parseInt(parts[1], 10);
      const s = parseInt(parts[2], 10);
      if (isNaN(h) || isNaN(m) || isNaN(s)) return null;
      return h * 3600 + m * 60 + s;
    }
    if (parts.length === 2) {
      const m = parseInt(parts[0], 10);
      const s = parseInt(parts[1], 10);
      if (isNaN(m) || isNaN(s)) return null;
      return m * 60 + s;
    }
    return null;
  }

  formatPaceZone(secondsPerKm: number): string {
    return this.secondsToMMSS(secondsPerKm);
  }

  private formatDateToISO(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // Convenience getters for template
  get hrZones() {
    return this.profile?.hrProfile?.zones ?? [];
  }

  get paceZones() {
    return this.profile?.paceProfile?.zones ?? [];
  }

  get hasHrProfile(): boolean {
    return this.profile?.hrProfile != null && this.profile.hrProfile.zones.length > 0;
  }

  get hasPaceProfile(): boolean {
    return this.profile?.paceProfile != null && this.profile.paceProfile.zones.length > 0;
  }
}
