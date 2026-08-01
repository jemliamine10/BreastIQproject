import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { PatientProfileService } from '../../services/patient-profile.service';
import { TrackerService } from '../../services/tracker.service';
import { TrackerEntryCreateDto, TrackerEntryResponseDto } from '../../models/tracker-entry.dto';

@Component({
  selector: 'app-tracker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tracker.component.html',
  styleUrl: './tracker.component.css'
})
export class TrackerComponent implements OnInit, OnDestroy {

  patientProfileId = '';
  assignedDoctorId = '';
  loading = true;
  submitting = false;
  showForm = false;
  submitSuccess = false;
  submitError = '';

  latestEntry: TrackerEntryResponseDto | null = null;
  history: TrackerEntryResponseDto[] = [];

  form: TrackerEntryCreateDto = {
    patientId: '',
    painLevel: 0,
    fatigueLevel: 0,
    moodLevel: 5,
    temperature: undefined,
    weight: undefined,
    vomiting: false,
    diarrhea: false,
    appetiteLoss: false,
    notes: ''
  };

  private subs: Subscription[] = [];

  constructor(
    private auth: AuthService,
    private profileSvc: PatientProfileService,
    private trackerSvc: TrackerService
  ) {}

  ngOnInit(): void {
    const userId = this.auth.currentUser?.id;
    if (!userId) { this.loading = false; return; }

    this.subs.push(
      this.profileSvc.getByUserId(userId).subscribe({
        next: (p) => {
          this.patientProfileId = p.id;
          this.assignedDoctorId = p.assignedDoctorProfileId || '';
          this.form.patientId = p.id;
          this.loadData();
        },
        error: () => { this.loading = false; }
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  private loadData(): void {
    if (this.assignedDoctorId) {
      this.subs.push(
        this.trackerSvc.getLatest(this.patientProfileId, this.assignedDoctorId).subscribe({
          next: (t) => this.latestEntry = t,
          error: () => {}
        })
      );
      this.subs.push(
        this.trackerSvc.getHistory(this.patientProfileId, this.assignedDoctorId).subscribe({
          next: (arr) => { this.history = arr; this.loading = false; },
          error: () => { this.loading = false; }
        })
      );
    } else {
      // Patient viewing their own data without a doctor link yet
      this.subs.push(
        this.trackerSvc.getMyLatest(this.patientProfileId).subscribe({
          next: (t) => this.latestEntry = t,
          error: () => {}
        })
      );
      this.subs.push(
        this.trackerSvc.getMyHistory(this.patientProfileId).subscribe({
          next: (arr) => { this.history = arr; this.loading = false; },
          error: () => { this.loading = false; }
        })
      );
    }
  }

  openForm(): void {
    this.form = {
      patientId: this.patientProfileId,
      painLevel: 0,
      fatigueLevel: 0,
      moodLevel: 5,
      temperature: undefined,
      weight: undefined,
      vomiting: false,
      diarrhea: false,
      appetiteLoss: false,
      notes: ''
    };
    this.showForm = true;
    this.submitSuccess = false;
    this.submitError = '';
  }

  closeForm(): void {
    this.showForm = false;
  }

  submitTracker(): void {
    if (this.submitting) return;
    this.submitting = true;
    this.submitError = '';

    this.subs.push(
      this.trackerSvc.submit(this.form, this.assignedDoctorId || undefined).subscribe({
        next: (response) => {
          this.latestEntry = response;
          this.history = [response, ...this.history];
          this.submitSuccess = true;
          this.submitting = false;
          setTimeout(() => { this.showForm = false; this.submitSuccess = false; }, 1500);
        },
        error: (err) => {
          this.submitError = err?.error?.message || 'Erreur lors de la soumission';
          this.submitting = false;
        }
      })
    );
  }

  /* ── Helpers ── */
  getBarWidth(val: number | undefined): string { return `${((val ?? 0) / 10) * 100}%`; }
  scoreClass(n: number | undefined, inverse = false): string {
    const v = n ?? 0;
    if (inverse) return v >= 7 ? 'good' : v >= 4 ? 'warn' : 'alert';
    return v <= 3 ? 'good' : v <= 6 ? 'warn' : 'alert';
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('fr-FR', { weekday: 'short', day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch { return iso; }
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `Il y a ${mins}min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    return `Il y a ${Math.floor(hours / 24)}j`;
  }

  formatDateShort(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
    } catch {
      return iso;
    }
  }

  getScoreClass(score: number): string {
    if (score >= 75) return 'optimal';
    if (score >= 50) return 'stable';
    if (score >= 30) return 'warning';
    return 'critical';
  }

  getScoreLabel(score: number): string {
    if (score >= 75) return 'État Optimal';
    if (score >= 50) return 'Stable';
    if (score >= 30) return 'À Surveiller';
    return 'Action Requise';
  }

  getRiskClass(risk: string): string {
    if (risk === 'LOW') return 'low';
    if (risk === 'MEDIUM') return 'medium';
    return 'high';
  }

  getRiskLabel(risk: string): string {
    if (risk === 'LOW') return 'Faible';
    if (risk === 'MEDIUM') return 'Modere';
    return 'Eleve';
  }

  hasGiSymptoms(entry: TrackerEntryResponseDto): boolean {
    return !!(entry.vomiting || entry.diarrhea || entry.appetiteLoss);
  }
}
