import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { Message } from 'primeng/message';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    InputText,
    Password,
    ButtonModule,
    Message,
  ],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss',
})
export default class RegisterPageComponent {
  registerForm: FormGroup;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group(
      {
        username: ['', [Validators.required]],
        email: ['', [Validators.required]],
        password: ['', [Validators.required]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  onSubmit(): void {
    this.errorMessage = null;
    this.usernameControl.setErrors(
      this.stripServerError(this.usernameControl.errors)
    );
    this.emailControl.setErrors(
      this.stripServerError(this.emailControl.errors)
    );

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const { username, email, password } = this.registerForm.value;
    this.authService.register({ username, email, password }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/auth/login']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 409) {
          const field = err.error?.field;
          if (field === 'username') {
            this.usernameControl.setErrors({ serverError: 'Username is already taken.' });
          } else if (field === 'email') {
            this.emailControl.setErrors({ serverError: 'Email is already in use.' });
          }
        } else if (err.status === 400) {
          this.errorMessage = err.error?.message || 'Invalid input.';
        } else {
          this.errorMessage = 'Something went wrong. Please try again.';
        }
      },
    });
  }

  get usernameControl() {
    return this.registerForm.get('username')!;
  }

  get emailControl() {
    return this.registerForm.get('email')!;
  }

  get passwordControl() {
    return this.registerForm.get('password')!;
  }

  get confirmPasswordControl() {
    return this.registerForm.get('confirmPassword')!;
  }

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  private stripServerError(errors: ValidationErrors | null): ValidationErrors | null {
    if (!errors) return null;
    const { serverError, ...rest } = errors;
    return Object.keys(rest).length > 0 ? rest : null;
  }
}
