import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { animate, style, transition, trigger } from '@angular/animations';
import { RegistrationService } from '../../services/registration.service';
import { UserRole, Gender, DoctorType, ConsultationMode } from '../../models/enums';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

@Component({
  selector: 'app-register-doctor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-doctor.component.html',
  styleUrl: './register-doctor.component.css',
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
export class RegisterDoctorComponent implements OnInit {
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
  doctorForm: FormGroup;
  detailsForm: FormGroup;

  genders = Object.values(Gender);
  doctorTypes = Object.values(DoctorType);
  consultationModes = Object.values(ConsultationMode);

  doctorTypeLabels: Record<string, string> = {
    'ONCOLOGIST': 'Oncologue',
    'SURGEON': 'Chirurgien',
    'RADIOLOGIST': 'Radiologue',
    'GENERALIST': 'Généraliste',
    'PATHOLOGIST': 'Pathologiste',
    'OTHER': 'Autre'
  };

  doctorTypeIcons: Record<string, string> = {
    'ONCOLOGIST': 'microscope',
    'SURGEON': 'hospital',
    'RADIOLOGIST': 'scan',
    'GENERALIST': 'stethoscope',
    'PATHOLOGIST': 'dna',
    'OTHER': 'user-check'
  };

  consultationModeLabels: Record<string, string> = {
    'IN_PERSON': 'En personne',
    'REMOTE': 'Téléconsultation',
    'HYBRID': 'Hybride'
  };

  consultationModeIcons: Record<string, string> = {
    'IN_PERSON': 'building',
    'REMOTE': 'laptop',
    'HYBRID': 'sync'
  };

  genderLabels: Record<string, string> = {
    'MALE': 'Homme',
    'FEMALE': 'Femme',
    'OTHER': 'Autre'
  };

  steps = [
    { number: 1, label: 'Identité', icon: 'user' },
    { number: 2, label: 'Spécialité', icon: 'stethoscope' },
    { number: 3, label: 'Détails', icon: 'clipboard' }
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
      phone: ['', [Validators.maxLength(30)]],
      gender: [''],
      dateOfBirth: ['']
    });

    this.doctorForm = this.fb.group({
      doctorType: ['', Validators.required],
      speciality: ['', [Validators.required, Validators.maxLength(120)]],
      licenseNumber: ['', [Validators.required, Validators.maxLength(80)]],
      consultationMode: ['', Validators.required],
      yearsOfExperience: [null],
      consultationFee: [null],
      languages: ['', Validators.maxLength(120)]
    });

    this.detailsForm = this.fb.group({
      clinicName: ['', Validators.maxLength(160)],
      bio: ['', Validators.maxLength(1500)],
      addressText: ['', Validators.maxLength(300)],
      city: ['', Validators.maxLength(80)],
      country: ['', Validators.maxLength(80)]
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
    if (this.currentStep === 2 && this.doctorForm.invalid) {
      this.doctorForm.markAllAsTouched();
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

  selectDoctorType(type: string): void {
    this.doctorForm.patchValue({ doctorType: type });
  }

  selectConsultationMode(mode: string): void {
    this.doctorForm.patchValue({ consultationMode: mode });
  }

  onSubmit(): void {
    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const userPayload = {
      ...this.userForm.value,
      role: UserRole.DOCTOR,
      city: this.detailsForm.value.city,
      country: this.detailsForm.value.country,
      addressText: this.detailsForm.value.addressText
    };

    const doctorPayload = {
      ...this.doctorForm.value,
      clinicName: this.detailsForm.value.clinicName,
      bio: this.detailsForm.value.bio,
      addressText: this.detailsForm.value.addressText
    };

    this.registrationService.registerDoctor(userPayload, doctorPayload).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/public/login'], {
          queryParams: { registered: 'doctor' }
        });
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err?.error?.message || "Erreur lors de l'inscription. Veuillez réessayer.";
      }
    });
  }

  get uf() { return this.userForm.controls; }
  get df() { return this.doctorForm.controls; }
  get dtf() { return this.detailsForm.controls; }

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

  getBioLength(): number {
    return this.detailsForm.get('bio')?.value?.length || 0;
  }
}
