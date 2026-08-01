import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { TreatmentManagementService, TreatmentStatus as TreatmentQueryStatus } from '../../services/treatment-management.service';
import { LinkService } from '../../services/link.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';

import { PatientFullResponseDto } from '../../models/patient-full-response.dto';
import {
  TreatmentCreateParams,
  TreatmentFullResponseDto,
  TreatmentSessionResponseDto
} from '../../models/treatment-management.dto';
import { LinkResponseDto } from '../../models/links-appointments.dto';
import { TreatmentType, TreatmentStatus, SessionStatus } from '../../models/enums';

@Component({
  selector: 'app-treatment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './treatment.component.html',
  styleUrl: './treatment.component.css'
})
export class TreatmentComponent implements OnInit, OnDestroy {
  private readonly uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  loading = true;
  doctorProfileId = '';
  patients: PatientFullResponseDto[] = [];
  activeLinks: LinkResponseDto[] = [];
  selectedPatient: PatientFullResponseDto | null = null;
  selectedPatientTreatments: TreatmentFullResponseDto[] = [];
  searchTerm = '';

  showCreateForm = false;
  createForm: Partial<TreatmentCreateParams> = {};
  creating = false;
  createError = '';

  treatmentTypes = Object.values(TreatmentType);
  sessions: TreatmentSessionResponseDto[] = [];
  selectedTreatmentId = '';
  loadingSessions = false;
  sessionModal: { session: TreatmentSessionResponseDto; action: 'done' | 'missed'; notes: string; sideEffects: string } | null = null;

  private subs: Subscription[] = [];

  constructor(
    private auth: AuthService,
    private userSvc: UserService,
    private treatmentSvc: TreatmentManagementService,
    private linkSvc: LinkService,
    private photoSvc: ProfilePhotoService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const doctorUserId = this.auth.currentUser?.id;
    if (!doctorUserId) {
      this.loading = false;
      return;
    }

    this.subs.push(
      this.userSvc.getDoctorByUserId(doctorUserId).subscribe({
        next: (doctor) => {
          this.doctorProfileId = (doctor.doctorProfileId || '').trim();
          if (!this.doctorProfileId) {
            console.warn('[DoctorTreatment] doctorProfileId manquant: patients non charges.');
            this.loading = false;
            return;
          }

          this.loadActiveLinkedPatients();
        },
        error: () => { this.loading = false; }
      })
    );

    // Deep-linking
    this.subs.push(
      this.route.queryParams.subscribe(params => {
        const userId = params['userId'];
        const patientProfileId = params['id'];
        if (userId || patientProfileId) {
          this.checkAndAutoSelect(userId, patientProfileId);
        }
      })
    );
  }

  private checkAndAutoSelect(userId?: string, profileId?: string): void {
    if (this.patients.length > 0) {
      this.performAutoSelect(userId, profileId);
    }
  }

  private performAutoSelect(userId?: string, profileId?: string): void {
    let target = null;
    if (profileId) {
      target = this.patients.find(p => p.patientProfileId === profileId);
    } else if (userId) {
      target = this.patients.find(p => p.userId === userId);
    }
    if (target) {
      this.selectPatient(target);
    }
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  get filteredPatients(): PatientFullResponseDto[] {
    if (!this.searchTerm) return this.patients;
    const term = this.searchTerm.toLowerCase();
    return this.patients.filter(p =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(term)
    );
  }

  selectPatient(p: PatientFullResponseDto): void {
    const activeLink = this.activeLinks.find((link) => link.patientProfileId === p.patientProfileId);
    if (!activeLink || activeLink.status !== 'ACTIVE') {
      console.warn('[DoctorTreatment] Selection bloquee: lien patient-medecin non ACTIVE', {
        patientId: p.patientProfileId,
        doctorId: this.doctorProfileId,
        linkStatus: activeLink?.status ?? 'NOT_FOUND'
      });
      return;
    }

    this.selectedPatient = p;
    this.loadSelectedPatientTreatments();
    this.sessions = [];
    this.selectedTreatmentId = '';
    this.showCreateForm = false;
  }

  deselectPatient(): void {
    this.selectedPatient = null;
    this.selectedPatientTreatments = [];
    this.sessions = [];
    this.selectedTreatmentId = '';
    this.showCreateForm = false;
  }

  openCreateForm(): void {
    if (!this.selectedPatient) return;
    this.createForm = {
      patientId: this.selectedPatient.patientProfileId,
      type: TreatmentType.CHEMO,
      intervalDays: 21
    };
    this.showCreateForm = true;
    this.createError = '';
  }

  submitCreate(): void {
    const doctorId = this.doctorProfileId;
    if (this.creating || !this.createForm.patientId || !this.createForm.type || !doctorId) return;
    if (!this.hasActiveLinkForSelectedPatient()) return;

    console.log('[DoctorTreatment] Creation traitement autorisee', {
      patientId: this.createForm.patientId,
      doctorId,
      linkStatus: 'ACTIVE'
    });

    this.creating = true;

    this.subs.push(
      this.treatmentSvc.create(this.createForm as TreatmentCreateParams, doctorId).subscribe({
        next: () => {
          this.creating = false;
          this.showCreateForm = false;
          this.loadSelectedPatientTreatments();
          this.sessions = [];
          this.selectedTreatmentId = '';
        },
        error: (err) => {
          this.createError = err?.error?.message || 'Erreur lors de la création';
          this.creating = false;
        }
      })
    );
  }

  loadSessions(treatmentId: string): void {
    const doctorId = this.doctorProfileId;
    if (!doctorId) return;
    if (!this.hasActiveLinkForSelectedPatient()) return;
    if (this.selectedTreatmentId === treatmentId) {
      this.selectedTreatmentId = '';
      this.sessions = [];
      return;
    }
    this.selectedTreatmentId = treatmentId;
    this.loadingSessions = true;

    console.log('[DoctorTreatment] Chargement sessions autorise', {
      patientId: this.selectedPatient?.patientProfileId,
      doctorId,
      treatmentId,
      linkStatus: 'ACTIVE'
    });

    this.subs.push(
      this.treatmentSvc.getSessions(treatmentId, doctorId).subscribe({
        next: (s) => { this.sessions = s; this.loadingSessions = false; },
        error: () => { this.loadingSessions = false; }
      })
    );
  }

  openSessionAction(session: TreatmentSessionResponseDto, action: 'done' | 'missed'): void {
    this.sessionModal = { session, action, notes: '', sideEffects: '' };
  }

  submitSessionAction(): void {
    if (!this.sessionModal) return;
    const doctorId = this.doctorProfileId;
    if (!doctorId) return;
    if (!this.hasActiveLinkForSelectedPatient()) return;
    const { session, action, notes, sideEffects } = this.sessionModal;

    console.log('[DoctorTreatment] Action session autorisee', {
      patientId: this.selectedPatient?.patientProfileId,
      doctorId,
      sessionId: session.id,
      action,
      linkStatus: 'ACTIVE'
    });

    const obs = action === 'done'
      ? this.treatmentSvc.markDone(session.id, doctorId, notes || undefined, sideEffects || undefined)
      : this.treatmentSvc.markMissed(session.id, doctorId, notes || undefined);

    this.subs.push(obs.subscribe({
      next: (updated) => {
        const idx = this.sessions.findIndex(s => s.id === session.id);
        if (idx >= 0) this.sessions[idx] = updated;
        this.sessionModal = null;
      },
      error: () => { this.sessionModal = null; }
    }));
  }

  deleteTreatment(treatmentId: string): void {
    const doctorId = this.doctorProfileId;
    if (!doctorId || !confirm('Supprimer ce traitement ?')) return;
    if (!this.hasActiveLinkForSelectedPatient()) return;

    console.log('[DoctorTreatment] Suppression traitement autorisee', {
      patientId: this.selectedPatient?.patientProfileId,
      doctorId,
      treatmentId,
      linkStatus: 'ACTIVE'
    });

    this.subs.push(
      this.treatmentSvc.delete(treatmentId, doctorId).subscribe({
        next: () => {
          this.loadSelectedPatientTreatments();
          this.sessions = [];
          this.selectedTreatmentId = '';
        }
      })
    );
  }

  refreshStatuses(): void {
    const doctorId = this.doctorProfileId;
    if (!doctorId || !this.selectedPatient) return;
    if (!this.hasActiveLinkForSelectedPatient()) return;

    console.log('[DoctorTreatment] Refresh statuts autorise', {
      patientId: this.selectedPatient.patientProfileId,
      doctorId,
      linkStatus: 'ACTIVE'
    });

    this.subs.push(
      this.treatmentSvc.refreshStatuses(this.selectedPatient.patientProfileId, doctorId).subscribe({
        next: () => {
          this.loadSelectedPatientTreatments();
        }
      })
    );
  }

  /* ── Helpers ── */
  getSessionStatusClass(s: SessionStatus | string): string {
    switch (s) {
      case SessionStatus.DONE: return 'done';
      case SessionStatus.MISSED: return 'missed';
      case SessionStatus.CANCELLED: return 'cancelled';
      default: return 'planned';
    }
  }

  getSessionStatusLabel(s: SessionStatus | string): string {
    switch (s) {
      case SessionStatus.DONE: return 'Terminée';
      case SessionStatus.MISSED: return 'Manquée';
      case SessionStatus.CANCELLED: return 'Annulée';
      default: return 'Planifiée';
    }
  }

  getTreatmentTypeLabel(t: TreatmentType | string): string {
    const labels: Record<string, string> = {
      CHEMO: 'Chimiothérapie', RADIO: 'Radiothérapie', SURGERY: 'Chirurgie',
      HORMONAL: 'Hormonothérapie', IMMUNOTHERAPY: 'Immunothérapie'
    };
    return labels[t] || t;
  }

  getTreatmentTypeIcon(t: TreatmentType | string): string {
    const icons: Record<string, string> = {
      CHEMO: 'M8 6h13 M8 12h13 M8 18h13 M3 6h.01 M3 12h.01 M3 18h.01',
      RADIO: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z',
      SURGERY: 'M14.5 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V7.5L14.5 2z',
      HORMONAL: 'M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z',
      IMMUNOTHERAPY: 'M9 11l3 3L22 4 M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11'
    };
    return icons[t] || icons['CHEMO'];
  }

  getTreatmentStatusLabel(s: TreatmentStatus | string): string {
    const labels: Record<string, string> = {
      UPCOMING: 'À venir', ACTIVE: 'En cours', COMPLETED: 'Terminé', STOPPED: 'Arrêté'
    };
    return labels[s] || s;
  }

  getTreatmentStatusClass(s: TreatmentStatus | string): string {
    const classes: Record<string, string> = {
      UPCOMING: 'upcoming', ACTIVE: 'active', COMPLETED: 'completed', STOPPED: 'stopped'
    };
    return classes[s] || 'upcoming';
  }

  getCycleProgress(t: TreatmentFullResponseDto): number {
    if (!t.cyclesTotal || t.cyclesTotal === 0) return 0;
    return Math.min(((t.currentCycle ?? 0) / t.cyclesTotal) * 100, 100);
  }

  getPatientTreatmentCount(patient: PatientFullResponseDto): number {
    if (this.selectedPatient?.patientProfileId === patient.patientProfileId) {
      return this.selectedPatientTreatments.length;
    }
    return patient.treatments?.length ?? 0;
  }

  getDoneCount(): number {
    return this.sessions.filter(s => s.status === SessionStatus.DONE).length;
  }
  getMissedCount(): number {
    return this.sessions.filter(s => s.status === SessionStatus.MISSED).length;
  }
  getPlannedCount(): number {
    return this.sessions.filter(s => s.status === SessionStatus.PLANNED).length;
  }

  getInitials(p: PatientFullResponseDto): string {
    return (p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '');
  }

  getStatusClass(s: string | undefined): string {
    switch (s) { case 'CRITICAL': return 'critical'; case 'WARNING': return 'warning'; default: return 'stable'; }
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    try { return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }); }
    catch { return d; }
  }

  parsePaths(d: string): string[] {
    return d.split(' M ').map((s, i) => i === 0 ? s : 'M ' + s).filter(Boolean);
  }

  private loadActiveLinkedPatients(selectedPatientId?: string, showLoader = true): void {
    if (showLoader) {
      this.loading = true;
    }

    const preservedPatientId = selectedPatientId ?? this.selectedPatient?.patientProfileId;

    this.subs.push(
      forkJoin({
        links: this.linkSvc.getConnected('doctor', this.doctorProfileId),
        allPatients: this.userSvc.getAllPatients()
      }).subscribe({
        next: ({ links, allPatients }) => {
          this.activeLinks = links.filter((link) => link.status === 'ACTIVE');
          const linkedPatientIds = new Set(this.activeLinks.map((link) => link.patientProfileId));
          this.patients = allPatients.filter((patient) => linkedPatientIds.has(patient.patientProfileId));

          if (preservedPatientId) {
            this.selectedPatient = this.patients.find((patient) => patient.patientProfileId === preservedPatientId) ?? null;
          }

          if (this.selectedPatient && !linkedPatientIds.has(this.selectedPatient.patientProfileId)) {
            this.selectedPatient = null;
            this.selectedPatientTreatments = [];
            this.sessions = [];
            this.selectedTreatmentId = '';
          }

          if (this.selectedPatient) {
            this.loadSelectedPatientTreatments();
            console.log('[DoctorTreatment] Patient resynchronise apres action', {
              patientId: this.selectedPatient.patientProfileId,
              treatmentsCount: this.selectedPatientTreatments.length
            });
          }

          console.log('[DoctorTreatment] Patients filtres par lien ACTIVE', {
            doctorId: this.doctorProfileId,
            totalLinks: links.length,
            activeLinks: this.activeLinks.length,
            totalPatients: allPatients.length,
            visiblePatients: this.patients.length
          });

          if (showLoader) {
            this.loading = false;
          }

          // After loading, check deep-linking
          const userId = this.route.snapshot.queryParams['userId'];
          const profileId = this.route.snapshot.queryParams['id'];
          if (userId || profileId) {
            this.performAutoSelect(userId, profileId);
          }
        },
        error: () => {
          if (showLoader) {
            this.loading = false;
          }
        }
      })
    );
  }

  private hasActiveLinkForSelectedPatient(): boolean {
    if (!this.selectedPatient) {
      return false;
    }

    const activeLink = this.activeLinks.find((link) => link.patientProfileId === this.selectedPatient?.patientProfileId);
    const isActive = !!activeLink && activeLink.status === 'ACTIVE';

    if (!isActive) {
      console.warn('[DoctorTreatment] Action bloquee: lien patient-medecin non ACTIVE', {
        patientId: this.selectedPatient.patientProfileId,
        doctorId: this.doctorProfileId,
        linkStatus: activeLink?.status ?? 'NOT_FOUND'
      });
    }

    return isActive;
  }

  private loadSelectedPatientTreatments(status?: TreatmentQueryStatus): void {
    if (!this.selectedPatient || !this.doctorProfileId) {
      this.selectedPatientTreatments = [];
      return;
    }

    if (!this.hasActiveLinkForSelectedPatient()) {
      this.selectedPatientTreatments = [];
      return;
    }

    const patientId = (this.selectedPatient.patientProfileId || '').trim();
    const doctorId = (this.doctorProfileId || '').trim();

    if (!this.uuidRegex.test(patientId) || !this.uuidRegex.test(doctorId)) {
      console.warn('[DoctorTreatment] Chargement traitements bloque: patientId/doctorId invalide', {
        patientId,
        doctorId
      });
      this.selectedPatientTreatments = [];
      return;
    }

    const endpoint = `/api/treatment-management/patient/${patientId}/treatments?doctorId=${encodeURIComponent(doctorId)}${status ? `&status=${encodeURIComponent(status)}` : ''}`;
    console.log('[DoctorTreatment] Endpoint traitements appele', { endpoint });

    this.subs.push(
      this.treatmentSvc.getPatientTreatments(patientId, doctorId, status).subscribe({
        next: (treatments) => {
          this.selectedPatientTreatments = treatments;
          console.log('[DoctorTreatment] Traitements charges via endpoint patient', {
            patientId,
            doctorId,
            status: status ?? 'ALL',
            count: treatments.length
          });
        },
        error: (err) => {
          console.error('[DoctorTreatment] Erreur getPatientTreatments', {
            patientId,
            doctorId,
            status: status ?? 'ALL',
            error: err?.message ?? err
          });
          this.selectedPatientTreatments = [];
        }
      })
    );
  }

  getPatientPhoto(p: PatientFullResponseDto): string | null {
    if (!p.profilePhotoUrl) return null;
    return this.photoSvc.getPhotoUrl(p.profilePhotoUrl);
  }
}
