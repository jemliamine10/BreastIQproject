import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { PatientProfileService } from '../../services/patient-profile.service';
import { PatientDashboardService } from '../../services/patient-dashboard.service';
import {
  PatientDashboardDto, TrackerPoint, TimelineItem,
  TreatmentProgress, AppointmentInfo
} from '../../models/patient-dashboard.dto';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  loading = true;
  greeting = '';
  today = '';
  patientProfileId = '';

  data: PatientDashboardDto | null = null;

  // SVG Health circle
  scoreStroke = 0;
  scoreCircumference = 2 * Math.PI * 80; // r=80

  // SVG Tracker chart
  trackerLinePath = '';
  trackerFillPath = '';
  trackerPoints: { x: number; y: number; val: number }[] = [];
  trackerLabels: string[] = [];

  // Treatment progress
  treatmentPct = 0;

  private subs: Subscription[] = [];

  constructor(
    private auth: AuthService,
    private profileSvc: PatientProfileService,
    private dashSvc: PatientDashboardService
  ) {}

  ngOnInit(): void {
    this.setGreeting();
    this.setToday();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  private loadData(): void {
    const userId = this.auth.currentUser?.id;
    if (!userId) { this.loading = false; return; }

    this.subs.push(
      this.profileSvc.getByUserId(userId).subscribe({
        next: (p) => {
          this.patientProfileId = p.id;
          this.loadDashboard();
        },
        error: () => { this.loading = false; }
      })
    );
  }

  private loadDashboard(): void {
    this.subs.push(
      this.dashSvc.getDashboard(this.patientProfileId).subscribe({
        next: (d) => {
          this.data = d;
          this.scoreStroke = (d.healthScore / 100) * this.scoreCircumference;
          this.buildTrackerChart(d.trackerTrend);
          if (d.currentTreatment) {
            const total = d.currentTreatment.totalSessions || d.currentTreatment.totalCycles || 1;
            const done = d.currentTreatment.completedSessions || d.currentTreatment.currentCycle || 0;
            this.treatmentPct = Math.round((done / total) * 100);
          }
          this.loading = false;
        },
        error: () => { this.loading = false; }
      })
    );
  }

  private buildTrackerChart(points: TrackerPoint[]): void {
    console.log('--- [Courbe Bien-être] Données brutes du Backend ---', points);
    if (!points || points.length === 0) {
      console.log('--- [Courbe Bien-être] Aucune donnée reçue ---');
      return;
    }

    const validPoints = points.filter(p => p.moodLevel !== null || p.painLevel !== null);
    if (validPoints.length === 0) {
      console.log('--- [Courbe Bien-être] Aucun niveau d\'humeur/douleur valide ---');
      return;
    }

    const w = 600, h = 200;
    this.trackerLabels = points.map(p => p.date);

    this.trackerPoints = points.map((p, i) => {
      // Compose a "wellbeing" score: high mood + low pain = high score
      const mood = p.moodLevel ?? 5;
      const pain = p.painLevel ?? 3;
      const score = Math.max(0, Math.min(100, (mood * 10) - (pain * 5) + 30));
      return {
        x: (i / Math.max(points.length - 1, 1)) * w,
        y: h - (score / 100) * h,
        val: Math.round(score)
      };
    });

    console.log('--- [Courbe Bien-être] Points calculés pour le SVG ---', this.trackerPoints);

    const line = this.trackerPoints.map((p, i) => `${i === 0 ? 'M ' : ' L '}${p.x} ${p.y}`).join('');
    this.trackerLinePath = line;
    this.trackerFillPath = `${line} L ${w} ${h} L 0 ${h} Z`;
  }

  /* ═══ HELPERS ═══ */

  get statusClass(): string {
    const s = this.data?.patientStatus;
    switch (s) { case 'CRITICAL': return 'critical'; case 'WARNING': return 'warning'; default: return 'stable'; }
  }

  get statusLabel(): string {
    const s = this.data?.patientStatus;
    switch (s) { case 'CRITICAL': return 'Critique'; case 'WARNING': return 'Attention'; default: return 'Stable'; }
  }

  getTimelineClass(type: string): string {
    switch (type) {
      case 'TREATMENT_START': case 'TREATMENT_END': return 'treatment';
      case 'SESSION_COMPLETED': return 'success';
      case 'SESSION_MISSED': case 'ALERT_GENERATED': return 'danger';
      case 'TRACKER_ENTRY': return 'tracker';
      case 'DIAGNOSIS': return 'diagnosis';
      default: return 'info';
    }
  }

  getSeverityClass(severity: string): string {
    switch (severity) { case 'CRITICAL': return 'critical'; case 'HIGH': return 'high'; case 'MEDIUM': return 'medium'; default: return 'low'; }
  }

  formatDate(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }); }
    catch { return ''; }
  }

  formatTime(iso: string): string {
    try { return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
    catch { return ''; }
  }

  formatDay(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit' }); }
    catch { return ''; }
  }

  formatMonth(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { month: 'short' }).toUpperCase(); }
    catch { return ''; }
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `Il y a ${mins}min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    return `Il y a ${days}j`;
  }

  getTreatmentLabel(type: string): string {
    const map: Record<string, string> = { CHEMO: 'Chimiothérapie', RADIO: 'Radiothérapie', SURGERY: 'Chirurgie', HORMONAL: 'Hormonothérapie', IMMUNOTHERAPY: 'Immunothérapie' };
    return map[type] || type;
  }

  get userName(): string {
    return this.auth.currentUser?.firstName ?? 'Patient';
  }

  private setGreeting(): void {
    const h = new Date().getHours();
    const name = this.userName;
    if (h < 12) this.greeting = `Bonjour, ${name} 👋`;
    else if (h < 18) this.greeting = `Bon après-midi, ${name} 👋`;
    else this.greeting = `Bonsoir, ${name} 👋`;
  }

  private setToday(): void {
    this.today = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }
}
