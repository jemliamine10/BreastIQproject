import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { DoctorDashboardService } from '../../services/doctor-dashboard.service';
import {
  DoctorDashboardDto, HealthTrendPoint, AlertSummary,
  AppointmentSummary, PatientSummary
} from '../../models/doctor-dashboard.dto';

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
  doctorProfileId = '';

  data: DoctorDashboardDto | null = null;

  // SVG chart computed values
  healthLinePath = '';
  healthFillPath = '';
  healthPoints: { x: number; y: number; val: number }[] = [];
  healthMonths: string[] = [];

  donutSegments: { color: string; dashArray: string; dashOffset: number; label: string; pct: number }[] = [];
  treatmentBars: { label: string; pct: number; value: number; color: string }[] = [];

  private subs: Subscription[] = [];

  constructor(
    private auth: AuthService,
    private userSvc: UserService,
    private dashSvc: DoctorDashboardService
  ) { }

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
      this.userSvc.getDoctorByUserId(userId).subscribe({
        next: (doc) => {
          this.doctorProfileId = doc.doctorProfileId || '';
          if (this.doctorProfileId) {
            this.loadDashboard();
          } else {
            this.loading = false;
          }
        },
        error: () => { this.loading = false; }
      })
    );
  }

  private loadDashboard(): void {
    this.subs.push(
      this.dashSvc.getDashboard(this.doctorProfileId).subscribe({
        next: (d) => {
          this.data = d;
          this.buildHealthChart(d.healthTrend);
          this.buildDonut(d.stageDistribution);
          this.buildTreatmentBars(d.treatmentDistribution);
          this.loading = false;
        },
        error: () => { this.loading = false; }
      })
    );
  }

  /* ═══════════ CHART BUILDERS ═══════════ */

  private buildHealthChart(points: HealthTrendPoint[]): void {
    if (!points || points.length === 0) return;
    const w = 500, h = 200;
    this.healthMonths = points.map(p => p.date);
    this.healthPoints = points.map((p, i) => ({
      x: (i / Math.max(points.length - 1, 1)) * w,
      y: h - (p.avgScore / 100) * h,
      val: Math.round(p.avgScore)
    }));

    const line = this.healthPoints.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ');
    this.healthLinePath = line;
    this.healthFillPath = `${line} L${w},${h} L0,${h} Z`;
  }

  private buildDonut(dist: Record<string, number>): void {
    if (!dist) return;
    const total = Object.values(dist).reduce((a, b) => a + b, 0);
    if (total === 0) return;
    const circumference = 2 * Math.PI * 40;
    const colors: Record<string, string> = {
      STAGE_0: '#6366f1', STAGE_I: '#10b981', STAGE_II: '#0ea5e9',
      STAGE_III: '#f59e0b', STAGE_IV: '#dc2626', UNKNOWN: '#8888aa'
    };
    const labels: Record<string, string> = {
      STAGE_0: 'Stade 0', STAGE_I: 'Stade I', STAGE_II: 'Stade II',
      STAGE_III: 'Stade III', STAGE_IV: 'Stade IV', UNKNOWN: 'Non classé'
    };
    let offset = 0;
    this.donutSegments = Object.entries(dist)
      .filter(([, v]) => v > 0)
      .map(([key, val]) => {
        const pct = (val / total) * 100;
        const length = (val / total) * circumference;
        const seg = {
          color: colors[key] || '#888',
          dashArray: `${length} ${circumference - length}`,
          dashOffset: -offset,
          label: labels[key] || key,
          pct: Math.round(pct)
        };
        offset += length;
        return seg;
      });
  }

  private buildTreatmentBars(dist: Record<string, number>): void {
    if (!dist) return;
    const max = Math.max(...Object.values(dist), 1);
    const colors: Record<string, string> = {
      CHEMO: '#3b82f6', RADIO: '#10b981', SURGERY: '#f59e0b',
      HORMONAL: '#a95e92', IMMUNOTHERAPY: '#6366f1'
    };
    const labels: Record<string, string> = {
      CHEMO: 'Chimio', RADIO: 'Radio', SURGERY: 'Chirurgie',
      HORMONAL: 'Hormonal', IMMUNOTHERAPY: 'Immuno'
    };
    this.treatmentBars = Object.entries(dist).map(([key, val]) => ({
      label: labels[key] || key,
      pct: (val / max) * 90,
      value: val,
      color: colors[key] || '#888'
    }));
  }

  /* ═══════════ HELPERS ═══════════ */

  getStatusClass(status: string): string {
    switch (status) { case 'CRITICAL': return 'critical'; case 'WARNING': return 'warning'; default: return 'stable'; }
  }

  getStatusLabel(status: string): string {
    switch (status) { case 'CRITICAL': return 'Critique'; case 'WARNING': return 'Attention'; default: return 'Stable'; }
  }

  getSeverityClass(severity: string): string {
    switch (severity) { case 'CRITICAL': return 'critical'; case 'HIGH': return 'high'; case 'MEDIUM': return 'medium'; default: return 'low'; }
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

  formatTime(iso: string): string {
    try { return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
    catch { return ''; }
  }

  getInitials(p: PatientSummary): string {
    return (p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '');
  }

  private setGreeting(): void {
    const h = new Date().getHours();
    const name = this.auth.currentUser?.firstName ?? 'Docteur';
    if (h < 12) this.greeting = `Bonjour, Dr. ${name}`;
    else if (h < 18) this.greeting = `Bon après-midi, Dr. ${name}`;
    else this.greeting = `Bonsoir, Dr. ${name}`;
  }

  private setToday(): void {
    this.today = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }
}
