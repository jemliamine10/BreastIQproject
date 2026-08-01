import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { PatientProfileService } from '../../services/patient-profile.service';
import { AlertService } from '../../services/alert.service';
import { WebSocketService } from '../../services/websocket.service';

import { AlertResponseDto } from '../../models/alert.dto';
import { AlertSeverity, AlertType } from '../../models/enums';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css'
})
export class NotificationsComponent implements OnInit, OnDestroy {
  loading = true;
  alerts: AlertResponseDto[] = [];
  patientProfileId = '';
  assignedDoctorId = '';

  private subs: Subscription[] = [];

  constructor(
    private auth: AuthService,
    private profileSvc: PatientProfileService,
    private alertSvc: AlertService,
    private wsSvc: WebSocketService
  ) {}

  ngOnInit(): void {
    const userId = this.auth.currentUser?.id;
    if (!userId) { this.loading = false; return; }

    this.subs.push(
      this.profileSvc.getByUserId(userId).subscribe({
        next: (p) => {
          this.patientProfileId = p.id;
          this.assignedDoctorId = p.assignedDoctorProfileId || '';
          this.loadAlerts();
          this.wsSvc.connect();
          this.wsSvc.subscribeToStatus(p.id);
        },
        error: () => { this.loading = false; }
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  private loadAlerts(): void {
    if (!this.assignedDoctorId) {
      this.loading = false;
      return;
    }
    this.subs.push(
      this.alertSvc.getByPatient(this.patientProfileId, this.assignedDoctorId).subscribe({
        next: (a) => { this.alerts = a; this.loading = false; },
        error: () => { this.loading = false; }
      })
    );
  }

  getSeverityClass(severity: AlertSeverity | string): string {
    switch (severity) {
      case AlertSeverity.CRITICAL: return 'critical';
      case AlertSeverity.HIGH: return 'high';
      case AlertSeverity.MEDIUM: return 'medium';
      default: return 'low';
    }
  }

  getSeverityLabel(severity: AlertSeverity | string): string {
    switch (severity) {
      case AlertSeverity.CRITICAL: return 'Critique';
      case AlertSeverity.HIGH: return 'Élevée';
      case AlertSeverity.MEDIUM: return 'Modérée';
      default: return 'Faible';
    }
  }

  getAlertTypeLabel(type: AlertType | string): string {
    const labels: Record<string, string> = {
      INFECTION_RISK: 'Risque d\'infection', SEVERE_PAIN: 'Douleur sévère',
      RAPID_WEIGHT_LOSS: 'Perte de poids', COMBINED_SYMPTOMS: 'Symptômes combinés',
      HIGH_TEMPERATURE: 'Température élevée', MISSED_SESSION: 'Séance manquée', CUSTOM: 'Personnalisée'
    };
    return labels[type] || type;
  }

  formatDate(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' }); }
    catch { return iso; }
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `Il y a ${mins} min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    return `Il y a ${Math.floor(hours / 24)}j`;
  }
}
