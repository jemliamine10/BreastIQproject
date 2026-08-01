import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { Availability, CreateAvailabilityDto, UpdateAvailabilityDto } from '../../models/availability.model';
import { ConsultationMode, DoctorType } from '../../models/enums';
import { DoctorFullResponseDto } from '../../models/doctor-full-response.dto';
import { AvailabilityService } from '../../services/availability.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';

interface AvailabilityForm {
  dayOfWeek: number;
  startHour: string;
  endHour: string;
  slotDuration: number;
  isActive: boolean;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  doctorProfile: DoctorFullResponseDto | null = null;
  doctorProfileId: string | null = null;

  availabilities: Availability[] = [];

  loadingProfile = false;
  loadingAvailability = false;
  savingAvailability = false;

  errorMessage: string | null = null;
  successMessage: string | null = null;

  formOpen = false;
  editingAvailabilityId: string | null = null;
  availabilityForm: AvailabilityForm = this.createDefaultForm();

  // Photo upload
  isUploadingPhoto = false;
  photoPreview: string | null = null;
  selectedFile: File | null = null;
  showPhotoModal = false;
  photoTimestamp = Date.now();

  readonly dayOptions = [
    { value: 1, label: 'Lundi' },
    { value: 2, label: 'Mardi' },
    { value: 3, label: 'Mercredi' },
    { value: 4, label: 'Jeudi' },
    { value: 5, label: 'Vendredi' },
    { value: 6, label: 'Samedi' },
    { value: 7, label: 'Dimanche' }
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly availabilityService: AvailabilityService,
    private readonly profilePhotoService: ProfilePhotoService
  ) {}

  ngOnInit(): void {
    this.resolveDoctorProfile();
  }

  get fullName(): string {
    if (!this.doctorProfile) {
      return 'Profil medecin';
    }
    return `Dr. ${this.doctorProfile.firstName} ${this.doctorProfile.lastName}`;
  }

  get activeAvailabilityCount(): number {
    return this.availabilities.filter((slot) => slot.isActive).length;
  }

  get formattedConsultationMode(): string {
    if (!this.doctorProfile?.consultationMode) {
      return 'Non renseigne';
    }
    if (this.doctorProfile.consultationMode === ConsultationMode.HYBRID) {
      return 'Presentiel + Teleconsultation';
    }
    if (this.doctorProfile.consultationMode === ConsultationMode.IN_PERSON) {
      return 'Presentiel';
    }
    return 'Teleconsultation';
  }

  get formattedDoctorType(): string {
    const value = this.doctorProfile?.doctorType;
    if (!value) {
      return 'Specialite non renseignee';
    }
    if (value === DoctorType.ONCOLOGIST) return 'Oncologue';
    if (value === DoctorType.SURGEON) return 'Chirurgien';
    if (value === DoctorType.RADIOLOGIST) return 'Radiologue';
    if (value === DoctorType.GENERALIST) return 'Generaliste';
    if (value === DoctorType.PATHOLOGIST) return 'Pathologiste';
    return 'Autre specialite';
  }

  get profilePhotoUrl(): string {
    if (this.photoPreview) return this.photoPreview;
    if (this.doctorProfile?.profilePhotoUrl) {
      if (this.doctorProfile.profilePhotoUrl.startsWith('/api/')) {
        return this.doctorProfile.profilePhotoUrl + '?t=' + this.photoTimestamp;
      }
      return this.doctorProfile.profilePhotoUrl;
    }
    return '';
  }

  get hasPhoto(): boolean {
    return !!(this.doctorProfile?.profilePhotoUrl);
  }

  // ── Photo Upload ──

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0]) return;
    const file = input.files[0];
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (!allowed.includes(file.type)) {
      this.errorMessage = 'Format non supporté. Utilisez JPEG, PNG, WebP ou GIF.';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Le fichier ne doit pas dépasser 5 Mo.';
      return;
    }
    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      this.photoPreview = e.target?.result as string;
      this.showPhotoModal = true;
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  confirmPhotoUpload(): void {
    if (!this.selectedFile || !this.doctorProfile?.userId) return;
    this.isUploadingPhoto = true;
    this.profilePhotoService.uploadPhoto(this.doctorProfile.userId, this.selectedFile).subscribe({
      next: (url) => {
        if (this.doctorProfile) this.doctorProfile.profilePhotoUrl = url;
        const stored = this.authService.currentUser;
        if (stored) {
          stored.profilePhotoUrl = url;
          this.authService.updateCurrentUser(stored);
        }
        this.photoTimestamp = Date.now();
        this.photoPreview = null;
        this.selectedFile = null;
        this.showPhotoModal = false;
        this.isUploadingPhoto = false;
        this.successMessage = 'Photo de profil mise à jour.';
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors de l\'upload de la photo.';
        this.isUploadingPhoto = false;
      }
    });
  }

  cancelPhotoUpload(): void {
    this.photoPreview = null;
    this.selectedFile = null;
    this.showPhotoModal = false;
  }

  deletePhoto(): void {
    if (!this.doctorProfile?.userId) return;
    if (!confirm('Supprimer votre photo de profil ?')) return;
    this.profilePhotoService.deletePhoto(this.doctorProfile.userId).subscribe({
      next: () => {
        if (this.doctorProfile) this.doctorProfile.profilePhotoUrl = undefined;
        const stored = this.authService.currentUser;
        if (stored) {
          stored.profilePhotoUrl = undefined;
          this.authService.updateCurrentUser(stored);
        }
        this.photoTimestamp = Date.now();
        this.successMessage = 'Photo supprimée.';
      },
      error: () => { this.errorMessage = 'Erreur lors de la suppression.'; }
    });
  }

  triggerPhotoInput(): void {
    document.getElementById('doctor-photo-upload-input')?.click();
  }

  get initials(): string {
    const first = this.doctorProfile?.firstName?.charAt(0).toUpperCase() ?? 'D';
    const last = this.doctorProfile?.lastName?.charAt(0).toUpperCase() ?? 'R';
    return `${first}${last}`;
  }

  getDayName(day: number): string {
    return this.dayOptions.find((item) => item.value === day)?.label ?? 'Jour inconnu';
  }

  get availabilitiesByDay(): { day: number; dayName: string; slots: Availability[] }[] {
    const groups: { [key: number]: Availability[] } = {};
    
    // Initialize all days (optional, can just show days with slots)
    // this.dayOptions.forEach(opt => groups[opt.value] = []);

    this.availabilities.forEach(slot => {
      if (!groups[slot.dayOfWeek]) {
        groups[slot.dayOfWeek] = [];
      }
      groups[slot.dayOfWeek].push(slot);
    });

    return Object.keys(groups)
      .map(key => parseInt(key))
      .sort((a, b) => a - b)
      .map(day => ({
        day,
        dayName: this.getDayName(day),
        slots: groups[day]
      }));
  }

  openCreateAvailability(): void {
    this.successMessage = null;
    this.formOpen = true;
    this.editingAvailabilityId = null;
    this.availabilityForm = this.createDefaultForm();
  }

  openEditAvailability(slot: Availability): void {
    this.successMessage = null;
    this.formOpen = true;
    this.editingAvailabilityId = slot.id;
    this.availabilityForm = {
      dayOfWeek: slot.dayOfWeek,
      startHour: slot.startHour,
      endHour: slot.endHour,
      slotDuration: slot.slotDuration,
      isActive: slot.isActive
    };
  }

  closeAvailabilityForm(): void {
    this.formOpen = false;
    this.editingAvailabilityId = null;
    this.availabilityForm = this.createDefaultForm();
  }

  saveAvailability(): void {
    if (!this.doctorProfileId || !this.isValidUuid(this.doctorProfileId)) {
      this.errorMessage = 'Identifiant medecin invalide. Rechargez la session.';
      return;
    }

    const validationError = this.validateForm();
    if (validationError) {
      this.errorMessage = validationError;
      return;
    }

    this.savingAvailability = true;
    this.errorMessage = null;

    if (this.editingAvailabilityId) {
      const payload: UpdateAvailabilityDto = {
        dayOfWeek: this.availabilityForm.dayOfWeek,
        startHour: this.availabilityForm.startHour,
        endHour: this.availabilityForm.endHour,
        slotDuration: this.availabilityForm.slotDuration,
        isActive: this.availabilityForm.isActive
      };

      this.availabilityService.updateAvailability(this.editingAvailabilityId, payload)
        .pipe(finalize(() => {
          this.savingAvailability = false;
        }))
        .subscribe({
          next: () => {
            this.successMessage = 'Disponibilite mise a jour.';
            this.closeAvailabilityForm();
            this.loadAvailabilities();
          },
          error: (err) => {
            this.errorMessage = this.mapError(err, 'Impossible de modifier la disponibilite.');
          }
        });

      return;
    }

    const payload: CreateAvailabilityDto = {
      dayOfWeek: this.availabilityForm.dayOfWeek,
      startHour: this.availabilityForm.startHour,
      endHour: this.availabilityForm.endHour,
      slotDuration: this.availabilityForm.slotDuration,
      isActive: this.availabilityForm.isActive
    };

    this.availabilityService.createAvailability(this.doctorProfileId, payload)
      .pipe(finalize(() => {
        this.savingAvailability = false;
      }))
      .subscribe({
        next: () => {
          this.successMessage = 'Disponibilite ajoutee.';
          this.closeAvailabilityForm();
          this.loadAvailabilities();
        },
        error: (err) => {
          this.errorMessage = this.mapError(err, 'Impossible de creer la disponibilite.');
        }
      });
  }

  toggleAvailability(slot: Availability): void {
    this.errorMessage = null;
    const payload: UpdateAvailabilityDto = {
      isActive: !slot.isActive
    };

    this.availabilityService.updateAvailability(slot.id, payload).subscribe({
      next: () => {
        this.successMessage = `Disponibilite ${!slot.isActive ? 'activee' : 'desactivee'}.`;
        this.loadAvailabilities();
      },
      error: (err) => {
        this.errorMessage = this.mapError(err, 'Impossible de changer le statut.');
      }
    });
  }

  deleteAvailability(availabilityId: string): void {
    if (!confirm('Supprimer ce creneau ?')) {
      return;
    }

    this.errorMessage = null;
    this.availabilityService.deleteAvailability(availabilityId).subscribe({
      next: () => {
        this.successMessage = 'Disponibilite supprimee.';
        this.loadAvailabilities();
      },
      error: (err) => {
        this.errorMessage = this.mapError(err, 'Impossible de supprimer la disponibilite.');
      }
    });
  }

  trackByAvailabilityId(_index: number, item: Availability): string {
    return item.id;
  }

  private resolveDoctorProfile(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) {
      this.errorMessage = 'Utilisateur medecin introuvable. Merci de vous reconnecter.';
      return;
    }

    if (!this.isValidUuid(userId)) {
      this.errorMessage = 'Identifiant utilisateur invalide.';
      return;
    }

    this.loadingProfile = true;
    this.userService.getDoctorByUserId(userId)
      .pipe(finalize(() => {
        this.loadingProfile = false;
      }))
      .subscribe({
        next: (doctor) => {
          this.doctorProfile = doctor;
          this.doctorProfileId = doctor.doctorProfileId;

          if (!this.isValidUuid(this.doctorProfileId)) {
            this.errorMessage = 'doctorProfileId invalide recu depuis le backend.';
            return;
          }

          this.loadAvailabilities();
        },
        error: (err) => {
          this.errorMessage = this.mapError(err, 'Impossible de charger le profil medecin.');
        }
      });
  }

  private loadAvailabilities(): void {
    if (!this.doctorProfileId || !this.isValidUuid(this.doctorProfileId)) {
      return;
    }

    this.loadingAvailability = true;
    this.availabilityService.getAvailabilities(this.doctorProfileId)
      .pipe(finalize(() => {
        this.loadingAvailability = false;
      }))
      .subscribe({
        next: (data) => {
          this.availabilities = data
            .map((item) => this.normalizeAvailability(item))
            .sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startHour.localeCompare(b.startHour));
        },
        error: (err) => {
          this.errorMessage = this.mapError(err, 'Impossible de charger les disponibilites.');
          this.availabilities = [];
        }
      });
  }

  private validateForm(): string | null {
    if (this.availabilityForm.dayOfWeek < 1 || this.availabilityForm.dayOfWeek > 7) {
      return 'Le jour doit etre compris entre 1 et 7.';
    }

    if (!this.isValidHour(this.availabilityForm.startHour) || !this.isValidHour(this.availabilityForm.endHour)) {
      return 'Les heures doivent respecter le format HH:mm.';
    }

    if (this.availabilityForm.startHour >= this.availabilityForm.endHour) {
      return 'L heure de fin doit etre apres l heure de debut.';
    }

    if (!Number.isInteger(this.availabilityForm.slotDuration) || this.availabilityForm.slotDuration <= 0) {
      return 'La duree du slot doit etre un entier positif.';
    }

    return null;
  }

  private normalizeAvailability(item: Availability): Availability {
    const rawValue = (item as any).dayOfWeek;
    let numericDay: number;

    if (typeof rawValue === 'number') {
      numericDay = rawValue;
    } else if (typeof rawValue === 'string') {
      const upper = rawValue.toUpperCase();
      const map: { [key: string]: number } = {
        'MONDAY': 1, 'LUNDI': 1,
        'TUESDAY': 2, 'MARDI': 2,
        'WEDNESDAY': 3, 'MERCREDI': 3,
        'THURSDAY': 4, 'JEUDI': 4,
        'FRIDAY': 5, 'VENDREDI': 5,
        'SATURDAY': 6, 'SAMEDI': 6,
        'SUNDAY': 7, 'DIMANCHE': 7
      };
      numericDay = map[upper] ?? parseInt(rawValue, 10);
    } else {
      numericDay = 1;
    }

    return {
      ...item,
      dayOfWeek: Number.isFinite(numericDay) ? numericDay : 1
    };
  }

  private createDefaultForm(): AvailabilityForm {
    return {
      dayOfWeek: 1,
      startHour: '09:00',
      endHour: '12:00',
      slotDuration: 30,
      isActive: true
    };
  }

  private isValidHour(value: string): boolean {
    return /^([01]\d|2[0-3]):([0-5]\d)$/.test(value);
  }

  private isValidUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
  }

  private mapError(error: unknown, fallback: string): string {
    if (typeof error === 'string' && error.trim()) {
      return error;
    }

    const typed = error as { message?: string; error?: { message?: string } };
    if (typed?.error?.message) {
      return typed.error.message;
    }
    if (typed?.message) {
      return typed.message;
    }

    return fallback;
  }
}
