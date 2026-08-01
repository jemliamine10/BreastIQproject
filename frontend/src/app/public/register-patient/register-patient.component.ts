import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { animate, style, transition, trigger } from '@angular/animations';
import { RegistrationService } from '../../services/registration.service';
import { UserRole, Gender } from '../../models/enums';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

@Component({
  selector: 'app-register-patient',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-patient.component.html',
  styleUrl: './register-patient.component.css',
  animations: [
    trigger('fadeInUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(30px)' }),
        animate('0.6s cubic-bezier(0.23, 1, 0.32, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ]),
    trigger('slideStep', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(60px)' }),
        animate('0.5s cubic-bezier(0.23, 1, 0.32, 1)', style({ opacity: 1, transform: 'translateX(0)' }))
      ]),
      transition(':leave', [
        animate('0.3s ease-in', style({ opacity: 0, transform: 'translateX(-60px)' }))
      ])
    ]),
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('0.4s ease-out', style({ opacity: 1 }))
      ])
    ])
  ]
})
export class RegisterPatientComponent implements OnInit {
  currentStep = 1;
  totalSteps = 3;
  showPassword = false;
  isLoading = false;
  errorMessage = '';
  emailChecking = false;
  emailAvailable: boolean | null = null;
  private emailCheck$ = new Subject<string>();

  // Password Strength
  passwordStrength = 0;
  passwordCriteria = {
    length: false,
    upper: false,
    lower: false,
    digit: false,
    special: false
  };

  userForm: FormGroup;
  medicalForm: FormGroup;
  consentForm: FormGroup;

  genders = Object.values(Gender);

  genderLabels: Record<string, string> = {
    'MALE': 'Homme',
    'FEMALE': 'Femme',
    'OTHER': 'Autre'
  };

  genderIcons: Record<string, string> = {
    'MALE': 'user',
    'FEMALE': 'user',
    'OTHER': 'user'
  };

  steps = [
    { number: 1, label: 'Identité', icon: 'user' },
    { number: 2, label: 'Médical', icon: 'hospital' },
    { number: 3, label: 'Consentement', icon: 'clipboard' }
  ];

  constructor(
    private fb: FormBuilder,
    private registrationService: RegistrationService,
    private router: Router
  ) {
    this.userForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(80)]],
      lastName: ['', [Validators.required, Validators.maxLength(80)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
      phone: ['', Validators.maxLength(30)],
      gender: [''],
      dateOfBirth: [''],
      addressText: ['', Validators.maxLength(300)],
      city: ['', Validators.maxLength(80)],
      country: ['', Validators.maxLength(80)]
    });

    this.medicalForm = this.fb.group({
      medicalRecordNumber: ['', Validators.maxLength(80)],
      emergencyContactName: ['', Validators.maxLength(120)],
      emergencyContactPhone: ['', Validators.maxLength(30)],
      heightCm: [null],
      weightKg: [null]
    });

    this.consentForm = this.fb.group({
      medicalConsent: [false, Validators.requiredTrue]
    });

    // Async email availability check
    this.emailCheck$.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      switchMap(email => {
        this.emailChecking = true;
        return this.registrationService.checkEmailAvailable(email);
      })
    ).subscribe({
      next: (available) => {
        this.emailChecking = false;
        this.emailAvailable = available;
      },
      error: () => {
        this.emailChecking = false;
        this.emailAvailable = null;
      }
    });
  }

  onEmailBlur(): void {
    const email = this.userForm.get('email')?.value;
    if (email && this.userForm.get('email')?.valid) {
      this.emailCheck$.next(email);
    } else {
      this.emailAvailable = null;
    }
  }

  get stepProgress(): number {
    return (this.currentStep / this.totalSteps) * 100;
  }

  nextStep(): void {
    if (this.currentStep === 1 && this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }
    if (this.currentStep === 2 && this.medicalForm.invalid) {
      this.medicalForm.markAllAsTouched();
      return;
    }
    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number): void {
    if (step < this.currentStep) {
      this.currentStep = step;
    }
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.consentForm.invalid) {
      this.consentForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const userPayload = {
      ...this.userForm.value,
      role: UserRole.PATIENT
    };

    const patientPayload = {
      ...this.medicalForm.value,
      medicalConsent: this.consentForm.value.medicalConsent
    };

    this.registrationService.registerPatient(userPayload, patientPayload).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/public/login'], {
          queryParams: { registered: 'patient' }
        });
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err?.error?.message || "Erreur lors de l'inscription. Veuillez réessayer.";
      }
    });
  }

  get uf() { return this.userForm.controls; }
  get mf() { return this.medicalForm.controls; }
  get cf() { return this.consentForm.controls; }

  ngOnInit(): void {
    // Password Strength Monitoring
    this.userForm.get('password')?.valueChanges.subscribe(password => {
      this.updatePasswordStrength(password || '');
    });
  }

  updatePasswordStrength(password: string): void {
    this.passwordCriteria = {
      length: password.length >= 6,
      upper: /[A-Z]/.test(password),
      lower: /[a-z]/.test(password),
      digit: /[0-9]/.test(password),
      special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
    };

    let metCriteria = 0;
    if (this.passwordCriteria.length) metCriteria++;
    if (this.passwordCriteria.upper) metCriteria++;
    if (this.passwordCriteria.lower) metCriteria++;
    if (this.passwordCriteria.digit) metCriteria++;
    if (this.passwordCriteria.special) metCriteria++;

    if (password.length === 0) {
      this.passwordStrength = 0;
    } else if (metCriteria <= 2) {
      this.passwordStrength = 1; // Weak
    } else if (metCriteria === 3) {
      this.passwordStrength = 2; // Medium
    } else if (metCriteria === 4) {
      this.passwordStrength = 3; // Good
    } else {
      this.passwordStrength = 4; // Strong
    }
  }

  getBmi(): string {
    const h = this.medicalForm.get('heightCm')?.value;
    const w = this.medicalForm.get('weightKg')?.value;
    if (h && w && h > 0) {
      const bmi = w / ((h / 100) ** 2);
      return bmi.toFixed(1);
    }
    return '--';
  }
}
