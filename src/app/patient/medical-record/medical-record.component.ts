import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AllergyResponseDto } from '../../models/allergy-response.dto';
import { TreatmentResponseDto } from '../../models/treatment-response.dto';
import { AllergySeverity, HistoryType, BloodType, EventType, CancerStage, TumorType, ReceptorStatus } from '../../models/enums';
import { PatientProfileService } from '../../services/patient-profile.service';
import { AuthService } from '../../services/auth.service';
import { TimelineService } from '../../services/timeline.service';
import { TrackerService } from '../../services/tracker.service';
import { MedicalRecordResponseDto, MedicalHistoryDto } from '../../models/medical-record.dto';
import { MedicalEventResponseDto } from '../../models/timeline-event.dto';
import { TrackerEntryResponseDto } from '../../models/tracker-entry.dto';
import { catchError, of, forkJoin } from 'rxjs';

export type Tab = 'overview' | 'allergies' | 'treatments' | 'history' | 'antecedents' | 'monitoring';

export interface Antecedent {
  id: string;
  category: 'personnel' | 'familial' | 'chirurgical';
  title: string;
  detail: string;
  date?: string;
}

@Component({
  selector: 'app-medical-record',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medical-record.component.html',
  styleUrl: './medical-record.component.css'
})
export class MedicalRecordComponent implements OnInit {

  activeTab: Tab = 'overview';

  /* ── State ── */
  profileId: string | null = null;
  loading = false;

  /* ── Patient profile (dynamic) ── */
  patient: any = {
    medicalRecordNumber: '—',
    heightCm: 0,
    weightKg: 0,
    bloodGroup: '—',
    emergencyContactName: '—',
    emergencyContactPhone: '—',
    medicalConsent: false,
    consentTimestamp: null,
    diagnosis: 'Chargement...',
    diagnosisDetail: '...',
    status: '...'
  };

  /* ── ALLERGIES ── */
  allergies: AllergyResponseDto[] = [];

  showAllergyForm = false;
  editingAllergy: AllergyResponseDto | null = null;
  allergyForm: Partial<AllergyResponseDto> = {};
  allergyToDelete: AllergyResponseDto | null = null;

  /* ── TREATMENTS ── */
  treatments: TreatmentResponseDto[] = [];

  showTreatmentForm = false;
  editingTreatment: TreatmentResponseDto | null = null;
  treatmentForm: Partial<TreatmentResponseDto> = {};
  treatmentToDelete: TreatmentResponseDto | null = null;

  /* ── ANTECEDENTS ── */
  antecedents: Antecedent[] = [];

  showAntecedentForm = false;
  editingAntecedent: Antecedent | null = null;
  antecedentForm: Partial<Antecedent> = {};
  antecedentToDelete: Antecedent | null = null;

  /* ── HISTORY EVENTS (timeline) ── */
  historyEvents: any[] = [];

  /* ── MONITORING HISTORY (tracker) ── */
  monitoringHistory: TrackerEntryResponseDto[] = [];

  /* ── STAGING DATA ── */
  stagingData: {
    stage: string;
    stageLabel: string;
    tnmClassification: string;
    tnmT: string;
    tnmN: string;
    tnmM: string;
    tumorType: string;
    tumorTypeLabel: string;
    grade: number | null;
    tumorSizeMm: number | null;
    lymphNodes: number | null;
    metastasis: boolean;
    erStatus: string;
    prStatus: string;
    her2Status: string;
    ki67: number | null;
    autoComputed: boolean;
    computedStageLabel: string;
  } = {
    stage: '', stageLabel: 'Non déterminé', tnmClassification: '',
    tnmT: '—', tnmN: '—', tnmM: '—',
    tumorType: '', tumorTypeLabel: 'Non défini',
    grade: null, tumorSizeMm: null, lymphNodes: null, metastasis: false,
    erStatus: 'UNKNOWN', prStatus: 'UNKNOWN', her2Status: 'UNKNOWN', ki67: null,
    autoComputed: false, computedStageLabel: ''
  };

  /* ── KPIs (Reactive) ── */
  get kpis() {
    return [
      { label: 'IMC', value: this.calcBMI(), sub: `${this.patient.heightCm} cm · ${this.patient.weightKg} kg`, color: 'linear-gradient(135deg,#10b981,#34d399)', icon: 'M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z' },
      { label: 'Groupe sanguin', value: this.patient.bloodGroup, sub: 'Rhésus positif', color: 'linear-gradient(135deg,#e04668,#ff6b8a)', icon: 'M12 2v14 M5 9l7 7 7-7' },
      { label: 'N° Dossier', value: this.patient.medicalRecordNumber, sub: 'Identifiant unique', color: 'linear-gradient(135deg,#a95e92,#b8669f)', icon: 'M14.5 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V7.5L14.5 2z M14 2v6h6' },
      { label: 'Statut', value: this.patient.status, sub: 'Protocole BC-ADJ-2022', color: 'linear-gradient(135deg,#7c6cc4,#9b8ce6)', icon: 'M9 11l3 3L22 4 M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11' },
    ];
  }

  /* ── Tabs ── */
  tabs: { id: Tab; label: string; icon: string; count?: number }[] = [
    { id: 'overview', label: 'Aperçu', icon: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10' },
    { id: 'allergies', label: 'Allergies', icon: 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z M12 9v4 M12 17h.01' },
    { id: 'treatments', label: 'Traitements', icon: 'M8 6h13 M8 12h13 M8 18h13 M3 6h.01 M3 12h.01 M3 18h.01' },
    { id: 'monitoring', label: 'Suivi Quotidien', icon: 'M13 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V9z M13 2v7h7' },
    { id: 'antecedents', label: 'Antécédents', icon: 'M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M12 6v6l4 2' },
    { id: 'history', label: 'Historique/Timeline', icon: 'M20 7H4a2 2 0 00-2 2v10a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z M16 3H8a2 2 0 00-2 2v2h12V5a2 2 0 00-2-2z' },
  ];

  constructor(
    private profileService: PatientProfileService,
    private authService: AuthService,
    private timelineService: TimelineService,
    private trackerService: TrackerService
  ) { }

  ngOnInit(): void {
    const user = this.authService.currentUser;
    if (user && user.id) {
      this.loadData(user.id);
    }
  }

  loadData(userId: string) {
    this.loading = true;
    this.profileService.getByUserId(userId).subscribe({
      next: (profile) => {
        this.profileId = profile.id;
        this.patient = {
          ...this.patient,
          medicalRecordNumber: profile.medicalRecordNumber || '—',
          heightCm: profile.heightCm || 0,
          weightKg: profile.weightKg || 0,
          bloodGroup: this.getBloodTypeLabel(profile.bloodType as BloodType),
          emergencyContactName: profile.emergencyContactName || '—',
          emergencyContactPhone: profile.emergencyContactPhone || '—',
          medicalConsent: profile.medicalConsent,
          consentTimestamp: profile.consentTimestamp,
          status: profile.patientStatus === 'STABLE' ? 'Rémission / Stable' : profile.patientStatus || 'Suivi'
        };
        this.allergies = profile.allergies || [];
        this.treatments = (profile.treatments || []).map(t => ({
          ...t,
          name: t.medicationName || 'Sans nom',
          description: t.notes || t.protocol || ''
        }));

        // Fetch medical record for diagnosis and antecedents
        this.fetchFullRecord();
        this.fetchTimeline();
        this.fetchMonitoring();
      },
      error: () => this.loading = false
    });
  }

  fetchMonitoring() {
    if (!this.profileId) return;
    this.trackerService.getMyHistory(this.profileId).subscribe({
      next: (history) => {
        this.monitoringHistory = history;
      },
      error: (err) => {
        console.warn('Erreur lors du chargement du suivi quotidien', err);
      }
    });
  }

  fetchFullRecord() {
    if (!this.profileId) return;
    this.profileService.getMyMedicalRecord(this.profileId).subscribe({
      next: (record) => {
        this.patient.diagnosis = record.diagnosis || 'Aucun diagnostic enregistré';
        this.patient.diagnosisDetail = this.formatDiagnosisDetail(record);
        this.antecedents = (record.medicalHistories || []).map(h => ({
          id: h.id!,
          category: this.mapBackendCategory(h.historyType!),
          title: h.title,
          detail: h.description,
          date: h.eventDate ? String(h.eventDate).substring(0, 4) : ''
        }));

        // Populate staging data
        this.populateStagingData(record);

        this.loading = false;
      },
      error: (err) => {
        this.patient.diagnosis = 'Aucun compte-rendu médical';
        this.patient.diagnosisDetail = 'En attente de remplissage par votre médecin.';
        this.antecedents = [];
        this.loading = false;
        console.warn('Dossier médical non trouvé', err);
      }
    });
  }

  fetchTimeline() {
    if (!this.profileId) return;
    this.timelineService.getMyTimeline(this.profileId).subscribe(events => {
      this.historyEvents = events.map(e => ({
        date: this.formatTimelineDate(e.eventDate),
        type: this.mapEventType(e.eventType),
        title: e.title,
        detail: e.description,
        icon: this.getEventIcon(e.eventType)
      }));
    });
  }

  setTab(t: Tab) { this.activeTab = t; }
  parsePaths(d: string): string[] { return d.split(' M ').map((s, i) => i === 0 ? s : 'M ' + s).filter(Boolean); }

  /* ─── ALLERGY CRUD ─── */
  openAddAllergy() {
    this.editingAllergy = null;
    this.allergyForm = { severity: AllergySeverity.MEDIUM };
    this.showAllergyForm = true;
  }
  openEditAllergy(a: AllergyResponseDto) {
    this.editingAllergy = a;
    this.allergyForm = { ...a };
    this.showAllergyForm = true;
  }
  saveAllergy() {
    if (!this.allergyForm.substance || !this.allergyForm.reaction || !this.allergyForm.severity || !this.profileId) return;

    if (this.editingAllergy) {
      this.profileService.updateAllergy(this.editingAllergy.id, this.allergyForm as any).subscribe(updated => {
        const idx = this.allergies.findIndex(x => x.id === updated.id);
        if (idx !== -1) this.allergies[idx] = updated;
        this.closeAllergyForm();
      });
    } else {
      const newAllergy = { ...this.allergyForm, patientProfileId: this.profileId } as any;
      this.profileService.addAllergy(newAllergy).subscribe(created => {
        this.allergies = [...this.allergies, created];
        this.closeAllergyForm();
      });
    }
  }
  closeAllergyForm() { this.showAllergyForm = false; this.editingAllergy = null; this.allergyForm = {}; }
  confirmDeleteAllergy(a: AllergyResponseDto) { this.allergyToDelete = a; }
  deleteAllergy() {
    if (this.allergyToDelete) {
      this.profileService.deleteAllergy(this.allergyToDelete.id).subscribe(() => {
        this.allergies = this.allergies.filter(x => x.id !== this.allergyToDelete!.id);
        this.allergyToDelete = null;
      });
    }
  }

  /* ─── TREATMENT CRUD ─── */
  openAddTreatment() {
    this.editingTreatment = null;
    this.treatmentForm = {};
    this.showTreatmentForm = true;
  }
  openEditTreatment(t: TreatmentResponseDto) {
    this.editingTreatment = t;
    this.treatmentForm = { ...t };
    this.showTreatmentForm = true;
  }
  saveTreatment() {
    if (!this.treatmentForm.name || !this.profileId) return;

    const payload = {
      ...this.treatmentForm,
      medicationName: this.treatmentForm.name,
      notes: this.treatmentForm.description,
      patientProfileId: this.profileId
    } as any;

    if (this.editingTreatment) {
      this.profileService.updateTreatment(this.editingTreatment.id, payload).subscribe(updated => {
        const idx = this.treatments.findIndex(x => x.id === updated.id);
        if (idx !== -1) {
          this.treatments[idx] = {
            ...updated,
            name: updated.medicationName || 'Sans nom',
            description: updated.notes || updated.protocol || ''
          };
        }
        this.closeTreatmentForm();
      });
    } else {
      this.profileService.addTreatment(payload).subscribe(created => {
        this.treatments = [...this.treatments, {
          ...created,
          name: created.medicationName || 'Sans nom',
          description: created.notes || created.protocol || ''
        }];
        this.closeTreatmentForm();
      });
    }
  }
  closeTreatmentForm() { this.showTreatmentForm = false; this.editingTreatment = null; this.treatmentForm = {}; }
  confirmDeleteTreatment(t: TreatmentResponseDto) { this.treatmentToDelete = t; }
  deleteTreatment() {
    if (this.treatmentToDelete) {
      this.profileService.deleteTreatment(this.treatmentToDelete.id).subscribe(() => {
        this.treatments = this.treatments.filter(x => x.id !== this.treatmentToDelete!.id);
        this.treatmentToDelete = null;
      });
    }
  }

  /* ─── ANTECEDENT CRUD ─── */
  openAddAntecedent() {
    this.editingAntecedent = null;
    this.antecedentForm = { category: 'personnel' };
    this.showAntecedentForm = true;
  }
  openEditAntecedent(a: Antecedent) {
    this.editingAntecedent = a;
    this.antecedentForm = { ...a };
    this.showAntecedentForm = true;
  }
  saveAntecedent() {
    if (!this.antecedentForm.title || !this.antecedentForm.detail || !this.profileId) return;

    const backendDto: MedicalHistoryDto = {
      title: this.antecedentForm.title,
      description: this.antecedentForm.detail,
      historyType: this.mapFrontendCategory(this.antecedentForm.category!),
      eventDate: this.antecedentForm.date ? `${this.antecedentForm.date}-01-01` : undefined
    };

    if (this.editingAntecedent) {
      this.profileService.updateMyMedicalHistory(this.editingAntecedent.id, backendDto).subscribe(updated => {
        const idx = this.antecedents.findIndex(x => x.id === updated.id);
        if (idx !== -1) {
          this.antecedents[idx] = {
            id: updated.id!,
            category: this.mapBackendCategory(updated.historyType!),
            title: updated.title,
            detail: updated.description,
            date: updated.eventDate ? String(updated.eventDate).substring(0, 4) : ''
          };
        }
        this.closeAntecedentForm();
      });
    } else {
      this.profileService.addMyMedicalHistory(this.profileId, backendDto).subscribe(created => {
        this.antecedents = [...this.antecedents, {
          id: created.id!,
          category: this.mapBackendCategory(created.historyType!),
          title: created.title,
          detail: created.description,
          date: created.eventDate ? String(created.eventDate).substring(0, 4) : ''
        }];
        this.closeAntecedentForm();
      });
    }
  }
  closeAntecedentForm() { this.showAntecedentForm = false; this.editingAntecedent = null; this.antecedentForm = {}; }
  confirmDeleteAntecedent(a: Antecedent) { this.antecedentToDelete = a; }
  deleteAntecedent() {
    if (this.antecedentToDelete) {
      this.profileService.deleteMyMedicalHistory(this.antecedentToDelete.id).subscribe(() => {
        this.antecedents = this.antecedents.filter(x => x.id !== this.antecedentToDelete!.id);
        this.antecedentToDelete = null;
      });
    }
  }

  /* ─── HELPERS ─── */
  AllergySeverity = AllergySeverity;

  getSeverityLabel(s: AllergySeverity) {
    return { [AllergySeverity.LOW]: 'Légère', [AllergySeverity.MEDIUM]: 'Modérée', [AllergySeverity.HIGH]: 'Sévère' }[s] ?? s;
  }
  getSeverityClass(s: AllergySeverity) {
    return { [AllergySeverity.LOW]: 'mild', [AllergySeverity.MEDIUM]: 'moderate', [AllergySeverity.HIGH]: 'severe' }[s] ?? '';
  }

  getTreatmentStatus(t: TreatmentResponseDto): 'active' | 'completed' | 'ongoing' {
    if (!t.endDate) return 'ongoing';
    return new Date(t.endDate) >= new Date() ? 'active' : 'completed';
  }
  getTreatmentStatusLabel(t: TreatmentResponseDto) {
    const s = this.getTreatmentStatus(t);
    return s === 'active' ? 'En cours' : s === 'ongoing' ? 'Indéfini' : 'Terminé';
  }
  formatDate(d?: string) {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }
  getDuration(t: TreatmentResponseDto) {
    if (!t.startDate) return '—';
    const months = Math.round((new Date(t.endDate ?? Date.now()).getTime() - new Date(t.startDate).getTime()) / (1000 * 60 * 60 * 24 * 30));
    return months < 1 ? '< 1 mois' : `${months} mois`;
  }

  getCategoryLabel(c: string) {
    return { personnel: 'Personnel', familial: 'Familial', chirurgical: 'Chirurgical' }[c] ?? c;
  }
  getCategoryClass(c: string) {
    return { personnel: 'personal', familial: 'familial', chirurgical: 'surgical' }[c] ?? '';
  }

  get severeCnt() { return this.allergies.filter(a => a.severity === AllergySeverity.HIGH).length; }
  get moderateCnt() { return this.allergies.filter(a => a.severity === AllergySeverity.MEDIUM).length; }
  get mildCnt() { return this.allergies.filter(a => a.severity === AllergySeverity.LOW).length; }
  get activeTreat() { return this.treatments.filter(t => this.getTreatmentStatus(t) !== 'completed').length; }

  get personalsCount() { return this.antecedents.filter(a => a.category === 'personnel').length; }
  get familialCount() { return this.antecedents.filter(a => a.category === 'familial').length; }
  get surgicalCount() { return this.antecedents.filter(a => a.category === 'chirurgical').length; }

  private formatDiagnosisDetail(record: MedicalRecordResponseDto): string {
    const parts = [];
    if (record.cancerStage) parts.push(record.cancerStage.replace('STAGE_', 'Stade '));
    if (record.tumorType) parts.push(record.tumorType.replace(/_/g, ' '));
    if (record.clinicalData?.grade) parts.push(`Grade ${record.clinicalData.grade}`);
    return parts.join(' · ') || 'Détails non renseignés';
  }

  private formatTimelineDate(dateStr: string): string {
    const d = new Date(dateStr);
    const months = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];
    return `${months[d.getMonth()]} ${d.getFullYear()}`;
  }

  private mapEventType(type: EventType): string {
    switch (type) {
      case EventType.DIAGNOSIS: return 'exam';
      case EventType.TREATMENT_START:
      case EventType.TREATMENT_END:
      case EventType.SESSION_COMPLETED: return 'treatment';
      case EventType.APPOINTMENT: return 'appointment';
      case EventType.TRACKER_ENTRY: return 'tracker';
      default: return 'note';
    }
  }

  private getEventIcon(type: EventType): string {
    switch (type) {
      case EventType.DIAGNOSIS: return 'M12 2a4 4 0 014 4v1a1 1 0 001 1h1a4 4 0 010 8h-1a1 1 0 00-1 1v1a4 4 0 01-8 0v-1a1 1 0 00-1-1H6a4 4 0 010-8h1a1 1 0 001-1V6a4 4 0 014-4z';
      case EventType.TREATMENT_START:
      case EventType.TREATMENT_END:
      case EventType.SESSION_COMPLETED: return 'M14.5 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V7.5L14.5 2z';
      case EventType.APPOINTMENT: return 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2';
      default: return 'M22 11.08V12a10 10 0 11-5.93-9.14';
    }
  }

  private mapBackendCategory(type: HistoryType): 'personnel' | 'familial' | 'chirurgical' {
    switch (type) {
      case HistoryType.FAMILY: return 'familial';
      case HistoryType.SURGICAL: return 'chirurgical';
      default: return 'personnel';
    }
  }

  private mapFrontendCategory(cat: string): HistoryType {
    switch (cat) {
      case 'familial': return HistoryType.FAMILY;
      case 'chirurgical': return HistoryType.SURGICAL;
      default: return HistoryType.PERSONAL;
    }
  }

  private getBloodTypeLabel(bt: BloodType): string {
    if (!bt) return '—';
    return bt.replace('_POSITIVE', '+').replace('_NEGATIVE', '-');
  }

  private calcBMI(): string {
    if (!this.patient.heightCm || !this.patient.weightKg) return '0';
    const h = (this.patient.heightCm) / 100;
    return (this.patient.weightKg / (h * h)).toFixed(1);
  }
  getBmiStatus(): { label: string; cls: string } {
    const b = parseFloat(this.calcBMI());
    if (b <= 0) return { label: '—', cls: '' };
    if (b < 18.5) return { label: 'Sous-poids', cls: 'moderate' };
    if (b < 25) return { label: 'Normal', cls: 'good' };
    if (b < 30) return { label: 'Surpoids', cls: 'moderate' };
    return { label: 'Obésité', cls: 'alert' };
  }

  /* ── Monitoring Helpers ── */
  formatDateTime(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('fr-FR', {
        day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
      });
    } catch { return iso; }
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

  /* ── Staging Helpers ── */
  private populateStagingData(record: MedicalRecordResponseDto): void {
    const cd = record.clinicalData;
    this.stagingData = {
      stage: record.cancerStage || '',
      stageLabel: this.getStageLabel(record.cancerStage),
      tnmClassification: record.tnmClassification || '',
      tnmT: cd?.tnmT || this.computeLocalT(cd?.tumorSize),
      tnmN: cd?.tnmN || this.computeLocalN(cd?.lymphNodesInvolved),
      tnmM: cd?.tnmM || (cd?.metastasis ? 'M1' : 'M0'),
      tumorType: record.tumorType || '',
      tumorTypeLabel: this.getTumorTypeLabel(record.tumorType),
      grade: cd?.grade ?? null,
      tumorSizeMm: cd?.tumorSize ?? null,
      lymphNodes: cd?.lymphNodesInvolved ?? null,
      metastasis: cd?.metastasis ?? false,
      erStatus: cd?.estrogenReceptor || 'UNKNOWN',
      prStatus: cd?.progesteroneReceptor || 'UNKNOWN',
      her2Status: cd?.her2Status || 'UNKNOWN',
      ki67: cd?.ki67 ?? null,
      autoComputed: record.stageAutoComputed ?? false,
      computedStageLabel: record.computedStageLabel || ''
    };
  }

  getStageLabel(stage?: string): string {
    if (!stage) return 'Non déterminé';
    const labels: Record<string, string> = {
      STAGE_0: 'Stade 0', STAGE_I: 'Stade I', STAGE_II: 'Stade II',
      STAGE_III: 'Stade III', STAGE_IV: 'Stade IV'
    };
    return labels[stage] || stage;
  }

  getStageColor(stage?: string): string {
    if (!stage) return '#6b7280';
    const colors: Record<string, string> = {
      STAGE_0: '#10b981', STAGE_I: '#34d399', STAGE_II: '#f59e0b',
      STAGE_III: '#f97316', STAGE_IV: '#ef4444'
    };
    return colors[stage] || '#6b7280';
  }

  getStageNumber(stage?: string): number {
    if (!stage) return 0;
    const nums: Record<string, number> = {
      STAGE_0: 0, STAGE_I: 1, STAGE_II: 2, STAGE_III: 3, STAGE_IV: 4
    };
    return nums[stage] ?? 0;
  }

  getStageProgress(stage?: string): number {
    return (this.getStageNumber(stage) / 4) * 100;
  }

  getTumorTypeLabel(t?: string): string {
    if (!t) return 'Non défini';
    const labels: Record<string, string> = {
      HR_POSITIVE: 'HR+ (Hormonodépendant)', HER2_POSITIVE: 'HER2+',
      TRIPLE_NEGATIVE: 'Triple Négatif', HR_POSITIVE_HER2_POSITIVE: 'HR+/HER2+',
      UNKNOWN: 'Inconnu'
    };
    return labels[t] || t;
  }

  getReceptorLabel(status?: string): string {
    if (status === 'POSITIVE') return 'Positif';
    if (status === 'NEGATIVE') return 'Négatif';
    return 'Non évalué';
  }

  getReceptorClass(status?: string): string {
    if (status === 'POSITIVE') return 'receptor-positive';
    if (status === 'NEGATIVE') return 'receptor-negative';
    return 'receptor-unknown';
  }

  private computeLocalT(tumorMm?: number): string {
    if (!tumorMm || tumorMm <= 0) return 'T0';
    if (tumorMm <= 1) return 'Tis';
    if (tumorMm <= 20) return 'T1';
    if (tumorMm <= 50) return 'T2';
    if (tumorMm <= 100) return 'T3';
    return 'T4';
  }

  private computeLocalN(nodes?: number): string {
    if (!nodes || nodes <= 0) return 'N0';
    if (nodes <= 3) return 'N1';
    if (nodes <= 9) return 'N2';
    return 'N3';
  }

  get hasStagingData(): boolean {
    return !!this.stagingData.stage || !!this.stagingData.tumorSizeMm || !!this.stagingData.grade;
  }
}
