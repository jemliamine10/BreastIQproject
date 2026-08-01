import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';

import { UserService } from '../../services/user.service';
import { TrackerService } from '../../services/tracker.service';
import { AuthService } from '../../services/auth.service';
import { LinkService } from '../../services/link.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';

import { PatientFullResponseDto } from '../../models/patient-full-response.dto';
import { TrackerEntryResponseDto } from '../../models/tracker-entry.dto';
import { LinkResponseDto } from '../../models/links-appointments.dto';
import { PatientStatus } from '../../models/enums';

@Component({
  selector: 'app-tracker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tracker.component.html',
  styleUrl: './tracker.component.css'
})
export class TrackerComponent implements OnInit, OnDestroy {
  loading = true;
  doctorProfileId = '';
  patients: PatientFullResponseDto[] = [];
  activeLinks: LinkResponseDto[] = [];
  selectedPatient: PatientFullResponseDto | null = null;
  trackerHistory: TrackerEntryResponseDto[] = [];
  latestEntry: TrackerEntryResponseDto | null = null;
  previousEntry: TrackerEntryResponseDto | null = null;
  loadingTracker = false;
  searchTerm = '';

  // ── Chart Data ──
  painChartPoints: { x: number; y: number }[] = [];
  painChartLinePath = '';
  painChartFillPath = '';
  fatigueChartPoints: { x: number; y: number }[] = [];
  fatigueChartLinePath = '';
  fatigueChartFillPath = '';
  moodChartPoints: { x: number; y: number }[] = [];
  moodChartLinePath = '';
  moodChartFillPath = '';
  chartXLabels: string[] = [];

  private subs: Subscription[] = [];

  constructor(
    private userSvc: UserService,
    private trackerSvc: TrackerService,
    private auth: AuthService,
    private linkSvc: LinkService,
    private photoSvc: ProfilePhotoService
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
          this.doctorProfileId = doctor.doctorProfileId || '';
          if (!this.doctorProfileId) {
            console.warn('[DoctorTracker] doctorProfileId manquant: patients non charges.');
            this.loading = false;
            return;
          }
          this.loadActiveLinkedPatients();
        },
        error: () => { this.loading = false; }
      })
    );
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  get filteredPatients(): PatientFullResponseDto[] {
    if (!this.searchTerm) return this.patients;
    const term = this.searchTerm.toLowerCase();
    return this.patients.filter(p =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
      p.email?.toLowerCase().includes(term)
    );
  }

  selectPatient(p: PatientFullResponseDto): void {
    const doctorId = this.doctorProfileId;
    if (!doctorId) return;

    const activeLink = this.activeLinks.find((link) => link.patientProfileId === p.patientProfileId);
    if (!activeLink || activeLink.status !== 'ACTIVE') {
      console.warn('[DoctorTracker] Action bloquee: lien patient-medecin non ACTIVE', {
        patientId: p.patientProfileId,
        doctorId,
        linkStatus: activeLink?.status ?? 'NOT_FOUND'
      });
      return;
    }

    this.selectedPatient = p;
    this.loadingTracker = true;
    this.latestEntry = null;
    this.previousEntry = null;
    this.trackerHistory = [];
    this.clearCharts();

    this.subs.push(
      this.trackerSvc.getHistory(p.patientProfileId, doctorId).subscribe({
        next: (arr) => {
          this.trackerHistory = arr;
          if (arr.length > 0) this.latestEntry = arr[0];
          if (arr.length > 1) this.previousEntry = arr[1];
          this.buildCharts();
          this.loadingTracker = false;
        },
        error: () => { this.loadingTracker = false; }
      })
    );
  }

  private loadActiveLinkedPatients(): void {
    this.loading = true;

    this.subs.push(
      forkJoin({
        links: this.linkSvc.getConnected('doctor', this.doctorProfileId),
        allPatients: this.userSvc.getAllPatients()
      }).subscribe({
        next: ({ links, allPatients }) => {
          this.activeLinks = links.filter((link) => link.status === 'ACTIVE');
          const linkedPatientIds = new Set(this.activeLinks.map((link) => link.patientProfileId));
          this.patients = allPatients.filter((patient) => linkedPatientIds.has(patient.patientProfileId));

          if (this.selectedPatient && !linkedPatientIds.has(this.selectedPatient.patientProfileId)) {
            this.selectedPatient = null;
            this.latestEntry = null;
            this.previousEntry = null;
            this.trackerHistory = [];
          }

          this.loading = false;
        },
        error: () => { this.loading = false; }
      })
    );
  }

  /* ═══════════ CHART BUILDERS ═══════════ */

  /**
   * Builds SVG path data for pain, fatigue, and mood trend lines.
   * Uses real tracker history data (reversed to chronological order).
   * No static data — these charts display actual patient entries.
   */
  private buildCharts(): void {
    // Use up to 10 most recent entries in chronological order
    const entries = this.trackerHistory.slice(0, 10).reverse();
    if (entries.length < 2) return;

    const w = 500;
    const h = 180;
    const maxVal = 10; // pain/fatigue/mood are 0-10

    // X labels
    this.chartXLabels = entries.map(e => this.formatDateShort(e.recordedAt));

    // Build points for each metric
    const buildPoints = (getter: (e: TrackerEntryResponseDto) => number): { x: number; y: number }[] =>
      entries.map((e, i) => ({
        x: (i / (entries.length - 1)) * w,
        y: h - (getter(e) / maxVal) * h
      }));

    const buildPath = (points: { x: number; y: number }[]): string =>
      points.map((p, i) => (i === 0 ? `M${p.x},${p.y}` : `L${p.x},${p.y}`)).join(' ');

    const buildFill = (linePath: string): string =>
      `${linePath} L${w},${h} L0,${h} Z`;

    // Pain
    this.painChartPoints = buildPoints(e => e.painLevel ?? 0);
    this.painChartLinePath = buildPath(this.painChartPoints);
    this.painChartFillPath = buildFill(this.painChartLinePath);

    // Fatigue
    this.fatigueChartPoints = buildPoints(e => e.fatigueLevel ?? 0);
    this.fatigueChartLinePath = buildPath(this.fatigueChartPoints);
    this.fatigueChartFillPath = buildFill(this.fatigueChartLinePath);

    // Mood
    this.moodChartPoints = buildPoints(e => e.moodLevel ?? 0);
    this.moodChartLinePath = buildPath(this.moodChartPoints);
    this.moodChartFillPath = buildFill(this.moodChartLinePath);
  }

  private clearCharts(): void {
    this.painChartPoints = [];
    this.painChartLinePath = '';
    this.painChartFillPath = '';
    this.fatigueChartPoints = [];
    this.fatigueChartLinePath = '';
    this.fatigueChartFillPath = '';
    this.moodChartPoints = [];
    this.moodChartLinePath = '';
    this.moodChartFillPath = '';
    this.chartXLabels = [];
  }

  /* ═══════════ GAUGE HELPERS ═══════════ */
  getScoreStroke(score: number): number {
    const circumference = 2 * Math.PI * 54;
    return (score / 100) * circumference;
  }
  getScoreCircumference(): number {
    return 2 * Math.PI * 54;
  }

  /* ═══════════ TREND HELPERS ═══════════ */
  getTrend(current?: number, previous?: number): 'up' | 'down' | 'stable' {
    if (current == null || previous == null) return 'stable';
    if (current > previous) return 'up';
    if (current < previous) return 'down';
    return 'stable';
  }

  getTrendIcon(current?: number, previous?: number): string {
    const trend = this.getTrend(current, previous);
    if (trend === 'up') return '↑';
    if (trend === 'down') return '↓';
    return '→';
  }

  /* ═══════════ COMPUTED PROPERTIES ═══════════ */
  getHighRiskCount(): number {
    // Count patients that have a health score < 35
    return this.patients.filter(p => (p.healthScore || 100) < 35).length;
  }

  /* ═══════════ STATUS/SCORE HELPERS ═══════════ */
  getStatusClass(s: PatientStatus | string | undefined): string {
    switch (s) {
      case PatientStatus.CRITICAL: return 'critical';
      case PatientStatus.WARNING: return 'warning';
      default: return 'stable';
    }
  }
  getStatusLabel(s: PatientStatus | string | undefined): string {
    switch (s) {
      case PatientStatus.CRITICAL: return 'Critique';
      case PatientStatus.WARNING: return 'Attention';
      default: return 'Stable';
    }
  }

  getScoreClass(score: number): string {
    if (score >= 60) return 'stable';
    if (score >= 35) return 'warning';
    return 'critical';
  }

  getRiskClass(risk: string): string {
    if (risk === 'LOW') return 'low';
    if (risk === 'MEDIUM') return 'medium';
    return 'high';
  }

  getRiskLabel(risk: string): string {
    if (risk === 'LOW') return 'Faible';
    if (risk === 'MEDIUM') return 'Modéré';
    return 'Élevé';
  }

  getBarWidth(val: number | undefined): string { return `${((val ?? 0) / 10) * 100}%`; }

  scoreClass(n: number | undefined, inverse = false): string {
    const v = n ?? 0;
    if (inverse) return v >= 7 ? 'good' : v >= 4 ? 'warn' : 'alert';
    return v <= 3 ? 'good' : v <= 6 ? 'warn' : 'alert';
  }

  getInitials(p: PatientFullResponseDto): string { return (p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? ''); }

  formatDate(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }); }
    catch { return iso; }
  }
  formatDateShort(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }); }
    catch { return iso; }
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `il y a ${mins}min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `il y a ${days}j`;
    return `il y a ${Math.floor(days / 7)} sem.`;
  }

  getPatientPhoto(p: PatientFullResponseDto): string | null {
    if (!p.profilePhotoUrl) return null;
    return this.photoSvc.getPhotoUrl(p.profilePhotoUrl);
  }

  deselectPatient(): void {
    this.selectedPatient = null;
    this.latestEntry = null;
    this.previousEntry = null;
    this.trackerHistory = [];
    this.clearCharts();
  }
}
