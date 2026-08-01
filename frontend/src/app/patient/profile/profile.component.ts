import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { PatientFullResponseDto } from '../../models/patient-full-response.dto';
import { Gender, UserRole, AllergySeverity } from '../../models/enums';
import { AllergyResponseDto } from '../../models/allergy-response.dto';
import { TreatmentResponseDto } from '../../models/treatment-response.dto';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {

  // Combined Profile Data
  profileData: PatientFullResponseDto | null = null;
  allergies: AllergyResponseDto[] = [];
  treatments: TreatmentResponseDto[] = [];
  isLoading = true;

  // Photo upload
  isUploadingPhoto = false;
  photoPreview: string | null = null;
  selectedFile: File | null = null;
  showPhotoModal = false;
  photoTimestamp = Date.now();

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private profilePhotoService: ProfilePhotoService
  ) { }

  ngOnInit(): void {
    const currentUser = this.authService.currentUser;
    if (currentUser && currentUser.id) {
      this.fetchPatientData(currentUser.id);
    } else {
      this.isLoading = false;
    }
  }

  fetchPatientData(userId: string): void {
    this.userService.getPatientByUserId(userId).subscribe({
      next: (data) => {
        this.profileData = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur lors de la récupération du profil patient', err);
        this.isLoading = false;
      }
    });
  }

  get profilePhoto(): string {
    if (this.photoPreview) {
      return this.photoPreview;
    }
    if (this.profileData?.profilePhotoUrl) {
      // If it's a relative backend URL, append cache-buster
      if (this.profileData.profilePhotoUrl.startsWith('/api/')) {
        return this.profileData.profilePhotoUrl + '?t=' + this.photoTimestamp;
      }
      return this.profileData.profilePhotoUrl;
    }
    return '';
  }

  get hasPhoto(): boolean {
    return !!(this.profileData?.profilePhotoUrl);
  }

  get userInitials(): string {
    if (!this.profileData) return 'P';
    const f = this.profileData.firstName?.charAt(0)?.toUpperCase() ?? '';
    const l = this.profileData.lastName?.charAt(0)?.toUpperCase() ?? '';
    return (f + l) || 'P';
  }

  // ── Photo Upload ──

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0]) return;

    const file = input.files[0];

    // Validate on client side
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (!allowed.includes(file.type)) {
      alert('Format non supporté. Utilisez JPEG, PNG, WebP ou GIF.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      alert('Le fichier ne doit pas dépasser 5 Mo.');
      return;
    }

    this.selectedFile = file;

    // Generate preview
    const reader = new FileReader();
    reader.onload = (e) => {
      this.photoPreview = e.target?.result as string;
      this.showPhotoModal = true;
    };
    reader.readAsDataURL(file);

    // Reset input so same file can be re-selected
    input.value = '';
  }

  confirmUpload(): void {
    if (!this.selectedFile || !this.profileData?.userId) return;

    this.isUploadingPhoto = true;
    this.profilePhotoService.uploadPhoto(this.profileData.userId, this.selectedFile).subscribe({
      next: (url) => {
        if (this.profileData) {
          this.profileData.profilePhotoUrl = url;
        }
        // Update stored user too
        const stored = this.authService.currentUser;
        if (stored) {
          (stored as any).profilePhotoUrl = url;
          localStorage.setItem('currentUser', JSON.stringify(stored));
        }
        this.photoTimestamp = Date.now();
        this.photoPreview = null;
        this.selectedFile = null;
        this.showPhotoModal = false;
        this.isUploadingPhoto = false;
      },
      error: (err) => {
        console.error('Erreur upload photo', err);
        alert('Erreur lors de l\'upload de la photo.');
        this.isUploadingPhoto = false;
      }
    });
  }

  cancelUpload(): void {
    this.photoPreview = null;
    this.selectedFile = null;
    this.showPhotoModal = false;
  }

  deletePhoto(): void {
    if (!this.profileData?.userId) return;
    if (!confirm('Supprimer votre photo de profil ?')) return;

    this.profilePhotoService.deletePhoto(this.profileData.userId).subscribe({
      next: () => {
        if (this.profileData) {
          this.profileData.profilePhotoUrl = undefined;
        }
        const stored = this.authService.currentUser;
        if (stored) {
          (stored as any).profilePhotoUrl = null;
          localStorage.setItem('currentUser', JSON.stringify(stored));
        }
        this.photoTimestamp = Date.now();
      },
      error: (err) => {
        console.error('Erreur suppression photo', err);
      }
    });
  }

  triggerFileInput(): void {
    document.getElementById('photo-upload-input')?.click();
  }

  // ── Existing getters ──

  get age(): number {
    if (!this.profileData?.dateOfBirth) return 0;
    const birthDate = new Date(this.profileData.dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  }

  get bmi(): number {
    if (!this.profileData?.heightCm || !this.profileData?.weightKg) return 0;
    const heightM = this.profileData.heightCm / 100;
    return parseFloat((this.profileData.weightKg / (heightM * heightM)).toFixed(1));
  }

  get bmiStatus(): string {
    const val = this.bmi;
    if (val < 18.5) return 'Insuffisance pondérale';
    if (val < 25) return 'Poids normal';
    if (val < 30) return 'Surpoids';
    return 'Obésité';
  }

  getJoinDate(): string {
    if (!this.profileData) return '—';
    return 'Membre depuis 2024'; 
  }

  getAllergyClass(severity: AllergySeverity): string {
    switch (severity) {
      case AllergySeverity.HIGH: return 'severity-high';
      case AllergySeverity.MEDIUM: return 'severity-mid';
      default: return 'severity-low';
    }
  }
}
