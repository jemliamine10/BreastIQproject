import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { DoctorFullResponseDto } from '../../models/doctor-full-response.dto';
import { DoctorType, ConsultationMode } from '../../models/enums';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { LinkService } from '../../services/link.service';
import { LinkResponseDto, LinkStatus } from '../../models/links-appointments.dto';

@Component({
  selector: 'app-my-doctors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-doctors.component.html',
  styleUrl: './my-doctors.component.css'
})
export class MyDoctorsComponent implements OnInit {

  // UI State
  activeTab: 'team' | 'directory' = 'team';
  loading = false;
  error: string | null = null;
  
  // Data
  currentUser: any = null;
  patientProfileId: string | null = null;
  
  // Lists
  myDoctors: DoctorFullResponseDto[] = [];
  pendingRequests: LinkResponseDto[] = [];
  directoryDoctors: DoctorFullResponseDto[] = [];
  
  // Filtering
  searchQuery = '';
  selectedSpecialty: string | null = null;
  
  // Action state
  processingAction: Record<string, boolean> = {};

  constructor(
    private readonly router: Router,
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly linkService: LinkService
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    this.loadInitialData();
  }

  loadInitialData(): void {
    if (!this.currentUser) return;

    this.loading = true;
    this.userService.getPatientByUserId(this.currentUser.id).subscribe({
      next: (patient) => {
        this.patientProfileId = patient.patientProfileId;
        this.refreshData();
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Impossible de charger le profil patient.';
      }
    });
  }

  refreshData(): void {
    if (!this.patientProfileId) return;
    
    this.loading = true;
    forkJoin({
      allDoctors: this.userService.getAllDoctors(),
      connected: this.linkService.getConnected('patient', this.patientProfileId),
      pending: this.linkService.getPending('patient', this.patientProfileId)
    }).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: ({ allDoctors, connected, pending }) => {
        // Find connected doctors
        const connectedIds = new Set(connected.map(l => l.doctorProfileId));
        this.myDoctors = allDoctors.filter(d => connectedIds.has(d.doctorProfileId));
        
        // Find pending requests (Patient -> Doctor)
        this.pendingRequests = pending;
        
        // Directory excludes already connected doctors
        this.directoryDoctors = allDoctors.filter(d => !connectedIds.has(d.doctorProfileId));
      },
      error: (err) => this.error = 'Erreur lors du chargement des données.'
    });
  }

  // --- Getters for Filters ---

  get filteredMyDoctors(): DoctorFullResponseDto[] {
    return this.myDoctors.filter(d => this.matchesSearch(d));
  }

  get filteredDirectory(): DoctorFullResponseDto[] {
    return this.directoryDoctors.filter(d => {
      if (this.selectedSpecialty && d.doctorType !== this.selectedSpecialty) return false;
      return this.matchesSearch(d);
    });
  }

  get specialties(): (string | DoctorType)[] {
    return [...new Set(this.directoryDoctors.map(d => d.doctorType))];
  }

  // --- Actions ---

  sendRequest(doctor: DoctorFullResponseDto): void {
    if (!this.patientProfileId) return;
    
    this.processingAction[doctor.doctorProfileId] = true;
    
    this.linkService.createRequest({
      patientId: this.patientProfileId,
      doctorId: doctor.doctorProfileId,
      patientProfileId: this.patientProfileId,
      doctorProfileId: doctor.doctorProfileId,
      requestedBy: 'PATIENT'
    }).subscribe({
      next: () => {
        this.refreshData(); // Reload to move from directory to pending/connected if auto-approved
        this.processingAction[doctor.doctorProfileId] = false;
        this.activeTab = 'team'; // Switch to see the request
      },
      error: (err) => {
        this.error = 'Erreur lors de l\'envoi de la demande.';
        this.processingAction[doctor.doctorProfileId] = false;
      }
    });
  }

  bookAppointment(doctor: DoctorFullResponseDto): void {
    this.router.navigate(['/patient/appointments'], {
      queryParams: {
        doctorId: doctor.doctorProfileId,
        action: 'book',
        patientId: this.patientProfileId
      }
    });
  }

  sendMessage(doctor: DoctorFullResponseDto): void {
    this.router.navigate(['/patient/messages'], { queryParams: { doctorId: doctor.userId } });
  }

  // --- Helpers ---

  matchesSearch(d: DoctorFullResponseDto): boolean {
    if (!this.searchQuery) return true;
    const q = this.searchQuery.toLowerCase();
    const fullName = `${d.firstName} ${d.lastName}`.toLowerCase();
    const city = (d.city || '').toLowerCase();
    const spec = (d.speciality || '').toLowerCase();
    return fullName.includes(q) || city.includes(q) || spec.includes(q);
  }

  getInitials(d: DoctorFullResponseDto): string {
    return `${d.firstName?.charAt(0) || ''}${d.lastName?.charAt(0) || ''}`.toUpperCase();
  }
  
  isPending(doctor: DoctorFullResponseDto): boolean {
    return this.pendingRequests.some(r => r.doctorProfileId === doctor.doctorProfileId);
  }

  formatSpecialty(type: DoctorType | string): string {
    const map: Record<string, string> = {
      'GENERALIST': 'Généraliste',
      'SURGEON': 'Chirurgien',
      'ONCOLOGIST': 'Oncologue',
      'RADIOLOGIST': 'Radiologue',
      'PATHOLOGIST': 'Pathologiste'
    };
    return map[String(type)] || String(type);
  }

  formatConsultation(mode: string): string {
    const map: Record<string, string> = {
      'IN_PERSON': 'En cabinet',
      'REMOTE': 'Téléconsultation',
      'HYBRID': 'Hybride'
    };
    return map[mode] || mode;
  }
}
