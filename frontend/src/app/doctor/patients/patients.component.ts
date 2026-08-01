import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { MedicalRecordService } from '../../services/medical-record.service';
import { AlertService } from '../../services/alert.service';
import { TimelineService } from '../../services/timeline.service';
import { TrackerService } from '../../services/tracker.service';
import { LinkService } from '../../services/link.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { ImagingService, MammogramAnalysisHistory } from '../../services/imaging.service';

import { PatientFullResponseDto } from '../../models/patient-full-response.dto';
import { MedicalRecordResponseDto, ClinicalDataDto, MedicalHistoryDto } from '../../models/medical-record.dto';
import { AlertResponseDto } from '../../models/alert.dto';
import { MedicalEventResponseDto } from '../../models/timeline-event.dto';
import { TrackerEntryResponseDto } from '../../models/tracker-entry.dto';
import { LinkResponseDto } from '../../models/links-appointments.dto';
import { PatientStatus, BloodType, CancerStage, TumorType, AlertSeverity, EventType, HistoryType, Gender, AllergySeverity } from '../../models/enums';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './patients.component.html',
  styleUrl: './patients.component.css'
})
export class PatientsComponent implements OnInit, OnDestroy {
  loading = true;
  AllergySeverity = AllergySeverity;
  doctorProfileId = '';
  patients: PatientFullResponseDto[] = [];
  activeLinks: LinkResponseDto[] = [];
  searchTerm = '';

  // Selection
  selectedPatient: PatientFullResponseDto | null = null;
  selectedRecord: MedicalRecordResponseDto | null = null;
  selectedAlerts: AlertResponseDto[] = [];
  selectedTimeline: MedicalEventResponseDto[] = [];
  selectedLatestTracker: TrackerEntryResponseDto | null = null;
  selectedImagingHistory: MammogramAnalysisHistory[] = [];

  /* ── STAGING DATA (Sync with Patient View) ── */
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

  loadingDetail = false;
  activeTab: 'dossier' | 'overview' | 'clinical' | 'timeline' | 'alerts' | 'history' = 'dossier';

  /* ── Sub-tabs for Dossier (Sync with Patient View) ── */
  activeDossierTab: 'overview' | 'allergies' | 'treatments' | 'history' | 'antecedents' = 'overview';
  
  dossierTabs = [
    { id: 'overview', label: 'Aperçu', icon: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10' },
    { id: 'allergies', label: 'Allergies', icon: 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z M12 9v4 M12 17h.01' },
    { id: 'treatments', label: 'Traitements', icon: 'M8 6h13 M8 12h13 M8 18h13 M3 6h.01 M3 12h.01 M3 18h.01' },
    { id: 'antecedents', label: 'Antécédents', icon: 'M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M12 6v6l4 2' },
    { id: 'history', label: 'Historique', icon: 'M20 7H4a2 2 0 00-2 2v10a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z M16 3H8a2 2 0 00-2 2v2h12V5a2 2 0 00-2-2z' },
  ];

  get dossierKpis() {
    const rec = this.selectedRecord;
    const pat = this.selectedPatient;
    if (!rec || !pat) return [];
    
    return [
      { label: 'IMC', value: this.calcBMI(rec.heightCm, rec.weightKg), sub: `${(rec.heightCm || 0)} cm · ${(rec.weightKg || 0)} kg`, color: 'linear-gradient(135deg,#10b981,#34d399)', icon: 'M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z' },
      { label: 'Groupe sanguin', value: this.getBloodTypeLabel(rec.bloodType), sub: 'Rhésus positif', color: 'linear-gradient(135deg,#e04668,#ff6b8a)', icon: 'M12 2v14 M5 9l7 7 7-7' },
      { label: 'N° Dossier', value: pat.medicalRecordNumber || '—', sub: 'Identifiant unique', color: 'linear-gradient(135deg,#a95e92,#b8669f)', icon: 'M14.5 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V7.5L14.5 2z M14 2v6h6' },
      { label: 'Statut', value: this.getStatusLabel(pat.patientStatus), sub: 'Suivi oncologique', color: 'linear-gradient(135deg,#7c6cc4,#9b8ce6)', icon: 'M9 11l3 3L22 4 M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11' },
    ];
  }

  // Clinical Data Modal
  showClinicalModal = false;
  savingClinical = false;
  clinicalForm: any = {
    tumorSize: 0,
    grade: 0,
    lymphNodesInvolved: 0,
    metastasis: false,
    estrogenReceptor: 'UNKNOWN',
    progesteroneReceptor: 'UNKNOWN',
    her2Status: 'UNKNOWN',
    ki67: 0
  };

  private subs: Subscription[] = [];

  constructor(
    private auth: AuthService,
    private userSvc: UserService,
    private recordSvc: MedicalRecordService,
    private alertSvc: AlertService,
    private timelineSvc: TimelineService,
    private trackerSvc: TrackerService,
    private linkSvc: LinkService,
    private photoSvc: ProfilePhotoService,
    private imagingSvc: ImagingService,
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
          this.doctorProfileId = doctor.doctorProfileId || '';
          if (!this.doctorProfileId) {
            console.warn('[DoctorPatients] doctorProfileId manquant: patients non charges.');
            this.loading = false;
            return;
          }

          this.loadActiveLinkedPatients();
        },
        error: () => { this.loading = false; }
      })
    );

    // Deep-linking: check for ?userId= or ?id=
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
    // If patients are already loaded, select now
    if (this.patients.length > 0) {
      this.performAutoSelect(userId, profileId);
    } 
    // Otherwise, it will be handled after loadActiveLinkedPatients()
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

  deselectPatient(): void {
    this.selectedPatient = null;
    this.selectedRecord = null;
    this.selectedAlerts = [];
    this.selectedTimeline = [];
    this.selectedLatestTracker = null;
    this.selectedImagingHistory = [];
    this.resetStagingData();
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  get filteredPatients(): PatientFullResponseDto[] {
    if (!this.searchTerm) return this.patients;
    const term = this.searchTerm.toLowerCase();
    return this.patients.filter(p =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
      p.email.toLowerCase().includes(term)
    );
  }

  selectPatient(p: PatientFullResponseDto): void {
    const doctorId = this.doctorProfileId;
    if (!doctorId) return;

    const activeLink = this.activeLinks.find((link) => link.patientProfileId === p.patientProfileId);
    if (!activeLink || activeLink.status !== 'ACTIVE') {
      console.warn('[DoctorPatients] Action bloquee: lien patient-medecin non ACTIVE', {
        patientId: p.patientProfileId,
        doctorId,
        linkStatus: activeLink?.status ?? 'NOT_FOUND'
      });
      return;
    }

    this.selectedPatient = p;
    this.loadingDetail = true;
    this.activeTab = 'dossier';

    const profileId = p.patientProfileId;

    this.subs.push(
      forkJoin({
        record: this.recordSvc.getByPatient(profileId, doctorId).pipe(catchError(() => of(null))),
        alerts: this.alertSvc.getByPatient(profileId, doctorId).pipe(catchError(() => of([]))),
        timeline: this.timelineSvc.getByPatient(profileId, doctorId).pipe(catchError(() => of([]))),
        tracker: this.trackerSvc.getLatest(profileId, doctorId).pipe(catchError(() => of(null))),
        imaging: this.imagingSvc.getPatientHistory(doctorId, profileId).pipe(catchError(() => of([])))
      }).subscribe({
        next: (res: any) => {
          this.selectedRecord = res.record;
          this.selectedAlerts = res.alerts || [];
          this.selectedTimeline = res.timeline || [];
          this.selectedLatestTracker = res.tracker;
          this.selectedImagingHistory = res.imaging || [];

          if (this.selectedRecord) {
            this.selectedRecord.tumorGrade = this.selectedRecord.clinicalData?.grade?.toString();
            this.selectedRecord.tumorSizeMm = this.selectedRecord.clinicalData?.tumorSize;
            this.selectedRecord.diagnosisNotes = this.selectedRecord.notes;
            this.selectedRecord.history = this.selectedRecord.medicalHistories;
            if (this.selectedRecord.clinicalData) {
              this.selectedRecord.clinicalData.erStatus = this.selectedRecord.clinicalData.estrogenReceptor;
              this.selectedRecord.clinicalData.prStatus = this.selectedRecord.clinicalData.progesteroneReceptor;
              this.selectedRecord.clinicalData.ki67Index = this.selectedRecord.clinicalData.ki67;
            }
          }

          // Populate Staging Data
          this.populateStagingData();

          this.loadingDetail = false;
        },
        error: () => { this.loadingDetail = false; }
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
          this.loading = false;

          // Deep-link priority
          const qp = this.route.snapshot.queryParams;
          if (qp['userId'] || qp['id']) {
            this.performAutoSelect(qp['userId'], qp['id']);
          }
        },
        error: () => { this.loading = false; }
      })
    );
  }

  setActiveTab(tab: 'dossier' | 'overview' | 'clinical' | 'timeline' | 'alerts' | 'history'): void {
    this.activeTab = tab;
  }

  setDossierTab(tab: any): void {
    this.activeDossierTab = tab;
  }

  parsePaths(d: string): string[] { 
    if (!d) return [];
    return d.split(' M ').map((s, i) => i === 0 ? s : 'M ' + s).filter(Boolean); 
  }

  /* ── Counter Helpers ── */
  getActiveCount(): number {
    return this.patients.filter(p => p.patientStatus === PatientStatus.STABLE || !p.patientStatus).length;
  }

  getCriticalCount(): number {
    return this.patients.filter(p => p.patientStatus === PatientStatus.CRITICAL).length;
  }

  /* ── Label Helpers ── */
  getStatusLabel(s: PatientStatus | string | undefined): string {
    switch (s) {
      case PatientStatus.CRITICAL: return 'Critique';
      case PatientStatus.WARNING: return 'Attention';
      default: return 'Stable';
    }
  }

  getStageLabel(s?: CancerStage | string): string {
    if (!s) return 'Non défini';
    return s.replace('STAGE_', 'Stade ');
  }

  getTumorTypeLabel(t?: TumorType | string): string {
    if (!t) return 'Non défini';
    const labels: Record<string, string> = {
      HR_POSITIVE: 'HR+', HER2_POSITIVE: 'HER2+',
      TRIPLE_NEGATIVE: 'Triple Négatif', HR_POSITIVE_HER2_POSITIVE: 'HR+/HER2+',
      UNKNOWN: 'Inconnu'
    };
    return labels[t] || t;
  }

  getHistoryTypeLabel(t: HistoryType | string): string {
    const labels: Record<string, string> = {
      PERSONAL: 'Antécédents Personnels', FAMILY: 'Antécédents Familiaux', SURGICAL: 'Antécédents Chirurgicaux'
    };
    return labels[t] || t;
  }

  getBloodTypeLabel(b?: BloodType | string): string {
    if (!b) return '—';
    return b.replace('_POSITIVE', '+').replace('_NEGATIVE', '-');
  }

  getGenderLabel(g?: Gender | string): string {
    if (!g) return 'Non renseigné';
    const labels: Record<string, string> = {
      MALE: 'Homme', FEMALE: 'Femme', OTHER: 'Autre'
    };
    return labels[g] || g;
  }

  getAge(dateOfBirth?: string): number {
    if (!dateOfBirth) return 0;
    const dob = new Date(dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age;
  }

  getMetastasisLabel(metastasis?: boolean): string {
    if (metastasis === true) return 'Oui — Métastases détectées';
    if (metastasis === false) return 'Non — Pas de métastases';
    return 'Non évalué';
  }

  getLymphNodesLabel(count?: number): string {
    if (count === undefined || count === null) return 'Non évalué';
    if (count === 0) return 'Aucun (0)';
    return `${count} envahi${count > 1 ? 's' : ''}`;
  }

  getSeverityClass(sev: AlertSeverity | string): string {
    switch (sev) {
      case AlertSeverity.CRITICAL: return 'critical';
      case AlertSeverity.HIGH: return 'high';
      case AlertSeverity.MEDIUM: return 'medium';
      default: return 'low';
    }
  }

  getEventTypeClass(type: any): string {
    const t = type?.toString().toLowerCase() || '';
    if (t.includes('alert') || t.includes('alerte') || t.includes('urgence')) return 'alert';
    if (t.includes('treat') || t.includes('traite') || t.includes('chimio')) return 'treatment';
    if (t.includes('session_completed') || t.includes('termine')) return 'session-done';
    if (t.includes('session_missed') || t.includes('manque')) return 'session-missed';
    if (t.includes('tracker')) return 'tracker';
    if (t.includes('diagnosis') || t.includes('cancer') || t.includes('stade')) return 'diagnosis';
    return 'default';
  }

  /* ── UI Helpers ── */
  getStatusClass(s: PatientStatus | string | undefined): string {
    switch (s) {
      case PatientStatus.CRITICAL: return 'critical';
      case PatientStatus.WARNING: return 'warning';
      default: return 'stable';
    }
  }

  getInitials(p: PatientFullResponseDto): string {
    return (p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '');
  }

  getDonutDash(score: number): string {
    const circumference = 2 * Math.PI * 32; // r=32 from SVG
    const fill = (score / 100) * circumference;
    return `${fill} ${circumference - fill}`;
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    try { return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }); }
    catch { return d; }
  }

  formatDateTime(iso: string): string {
    try { return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }); }
    catch { return iso; }
  }

  getHistoryByType(type: string): MedicalHistoryDto[] {
    const entries = this.selectedRecord?.history || this.selectedRecord?.medicalHistories || [];
    return entries.filter((h: MedicalHistoryDto) => h.historyType === type || h.type === type);
  }

  /* ── BMI Helpers (Sync with Patient) ── */
  calcBMI(hCm?: number, wKg?: number): string {
    if (!hCm || !wKg) return '0';
    const h = hCm / 100;
    return (wKg / (h * h)).toFixed(1);
  }

  getBmiStatus(): { label: string; cls: string } {
    const rec = this.selectedRecord;
    if (!rec) return { label: '—', cls: '' };
    const b = parseFloat(this.calcBMI(rec.heightCm, rec.weightKg));
    if (b <= 0) return { label: '—', cls: '' };
    if (b < 18.5) return { label: 'Sous-poids', cls: 'moderate' };
    if (b < 25) return { label: 'Normal', cls: 'good' };
    if (b < 30) return { label: 'Surpoids', cls: 'moderate' };
    return { label: 'Obésité', cls: 'alert' };
  }

  /* ── Severity Helpers (Sync with Patient) ── */
  getSeverityLabel(s: any) {
    const labels: any = { LOW: 'Légère', MEDIUM: 'Modérée', HIGH: 'Sévère' };
    return labels[s] || s;
  }

  /* ── Treatment Helpers (Sync with Patient) ── */
  getTreatmentDisplayStatus(t: any): 'active' | 'completed' | 'ongoing' {
    if (!t.endDate) return 'ongoing';
    return new Date(t.endDate) >= new Date() ? 'active' : 'completed';
  }

  getTreatmentDisplayStatusLabel(t: any) {
    const s = this.getTreatmentDisplayStatus(t);
    return s === 'active' ? 'En cours' : s === 'ongoing' ? 'Indéfini' : 'Terminé';
  }

  getTreatmentDuration(t: any) {
    if (!t.startDate) return '—';
    const months = Math.round((new Date(t.endDate ?? Date.now()).getTime() - new Date(t.startDate).getTime()) / (1000 * 60 * 60 * 24 * 30));
    return months < 1 ? '< 1 mois' : `${months} mois`;
  }

  /* ── Category Helpers (Sync with Patient) ── */
  getCategoryLabel(c: string) {
    const labels: any = { PERSONAL: 'Personnel', FAMILY: 'Familial', SURGICAL: 'Chirurgical' };
    return labels[c] || c;
  }

  getCategoryClass(c: string) {
    const classes: any = { PERSONAL: 'personal', FAMILY: 'familial', SURGICAL: 'surgical' };
    return classes[c] || '';
  }

  /* ── Timeline Helpers (Sync with Patient) ── */
  getEventIcon(type: any): string {
    switch (type) {
      case 'DIAGNOSIS': return 'M12 2a4 4 0 014 4v1a1 1 0 001 1h1a4 4 0 010 8h-1a1 1 0 00-1 1v1a4 4 0 01-8 0v-1a1 1 0 00-1-1H6a4 4 0 010-8h1a1 1 0 001-1V6a4 4 0 014-4z';
      case 'TREATMENT_START': return 'M14.5 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V7.5L14.5 2z M14 2v6h6';
      case 'APPOINTMENT': return 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2';
      default: return 'M22 11.08V12a10 10 0 11-5.93-9.14';
    }
  }

  formatTimelineDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    const months = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];
    return `${months[d.getMonth()]} ${d.getFullYear()}` || '—';
  }

  getPatientPhoto(p: PatientFullResponseDto): string | null {
    if (!p.profilePhotoUrl) return null;
    return this.photoSvc.getPhotoUrl(p.profilePhotoUrl);
  }

  /* ── Staging Logic (Doctor Side) ── */
  private populateStagingData(): void {
    const record = this.selectedRecord;
    if (!record) {
      this.resetStagingData();
      return;
    }

    const cd = record.clinicalData;
    this.stagingData = {
      stage: record.cancerStage || '',
      stageLabel: this.getStageLabelText(record.cancerStage),
      tnmClassification: record.tnmClassification || '',
      tnmT: cd?.tnmT || this.computeT(cd?.tumorSize),
      tnmN: cd?.tnmN || this.computeN(cd?.lymphNodesInvolved),
      tnmM: cd?.tnmM || (cd?.metastasis ? 'M1' : 'M0'),
      tumorType: record.tumorType || '',
      tumorTypeLabel: this.getTumorTypeLabelText(record.tumorType),
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

  private resetStagingData(): void {
    this.stagingData = {
      stage: '', stageLabel: 'Non déterminé', tnmClassification: '',
      tnmT: '—', tnmN: '—', tnmM: '—',
      tumorType: '', tumorTypeLabel: 'Non défini',
      grade: null, tumorSizeMm: null, lymphNodes: null, metastasis: false,
      erStatus: 'UNKNOWN', prStatus: 'UNKNOWN', her2Status: 'UNKNOWN', ki67: null,
      autoComputed: false, computedStageLabel: ''
    };
  }

  getStageLabelText(stage?: string): string {
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

  getTumorTypeLabelText(t?: string): string {
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

  private computeT(tumorMm?: number): string {
    if (!tumorMm || tumorMm <= 0) return 'T0';
    if (tumorMm <= 1) return 'Tis';
    if (tumorMm <= 20) return 'T1';
    if (tumorMm <= 50) return 'T2';
    if (tumorMm <= 100) return 'T3';
    return 'T4';
  }

  private computeN(nodes?: number): string {
    if (!nodes || nodes <= 0) return 'N0';
    if (nodes <= 3) return 'N1';
    if (nodes <= 9) return 'N2';
    return 'N3';
  }

  get hasStagingData(): boolean {
    return !!this.stagingData.stage || !!this.stagingData.tumorSizeMm || !!this.stagingData.grade;
  }

  /* ── History Data Dynamic Extractors ── */

  getLastMammographyDateFromHistory(): string {
    const dates: number[] = [];

    // 1. Check in Medical Histories
    if (this.selectedRecord?.medicalHistories) {
      this.selectedRecord.medicalHistories
        .filter(h => h.title?.toLowerCase().includes('mammographie') || h.description?.toLowerCase().includes('mammographie'))
        .forEach(h => {
          if (h.eventDate) dates.push(new Date(h.eventDate).getTime());
        });
    }

    // 2. Check in Imaging history (Imagerie IA)
    if (this.selectedImagingHistory) {
      this.selectedImagingHistory.forEach(i => {
        if (i.analysisDate) dates.push(new Date(i.analysisDate).getTime());
      });
    }

    if (dates.length === 0) return '—';
    
    // Get the most recent date
    const mostRecent = new Date(Math.max(...dates));
    return this.formatDate(mostRecent.toISOString());
  }

  getSurgeryFromHistory(): string {
    if (!this.selectedRecord?.medicalHistories) return 'Initialisation requise';
    const surgeries = this.selectedRecord.medicalHistories
      .filter(h => h.historyType === HistoryType.SURGICAL || h.title?.toLowerCase().includes('chirurgie') || h.title?.toLowerCase().includes('mastectomie') || h.title?.toLowerCase().includes('tumorectomie'))
      .sort((a, b) => new Date(b.eventDate || 0).getTime() - new Date(a.eventDate || 0).getTime());
    return surgeries.length > 0 ? surgeries[0].title : 'A définir';
  }

  hasTherapyInHistory(keyword: string): boolean {
    if (!this.selectedRecord) return false;
    
    // Check in Medical Histories
    const inHistory = this.selectedRecord.medicalHistories?.some(h => 
      h.title?.toLowerCase().includes(keyword.toLowerCase()) || 
      h.description?.toLowerCase().includes(keyword.toLowerCase())
    );
    if (inHistory) return true;

    // Check in Active/Planned Treatments
    const mapping: Record<string, string> = {
      'radiothérapie': 'RADIO',
      'chimiothérapie': 'CHEMO',
      'hormonothérapie': 'HORMONAL'
    };
    const type = mapping[keyword.toLowerCase()];
    if (type && this.selectedRecord.treatments) {
      return this.selectedRecord.treatments.some(t => t.treatmentType === type);
    }

    return false;
  }

  /* ── Metadata & Form Actions ── */
  openClinicalModal(): void {
    if (!this.selectedRecord) return;
    const cd = this.selectedRecord.clinicalData;
    this.clinicalForm = {
      tumorSize: cd?.tumorSize ?? 0,
      grade: cd?.grade ?? 0,
      lymphNodesInvolved: cd?.lymphNodesInvolved ?? 0,
      metastasis: cd?.metastasis ?? false,
      estrogenReceptor: cd?.estrogenReceptor || 'UNKNOWN',
      progesteroneReceptor: cd?.progesteroneReceptor || 'UNKNOWN',
      her2Status: cd?.her2Status || 'UNKNOWN',
      ki67: cd?.ki67 ?? 0
    };
    this.showClinicalModal = true;
  }

  closeClinicalModal(): void {
    this.showClinicalModal = false;
  }

  saveClinicalData(): void {
    if (!this.selectedPatient || !this.doctorProfileId) return;
    this.savingClinical = true;
    
    this.subs.push(
      this.recordSvc.updateClinicalData(
        this.selectedPatient.patientProfileId,
        this.clinicalForm,
        this.doctorProfileId
      ).subscribe({
        next: (updatedCd) => {
          if (this.selectedRecord) {
            this.selectedRecord.clinicalData = updatedCd;
            // Trigger a re-fetch or manual populate to see the new stage
            alert('Données cliniques mises à jour. Le stade a été recalculé.');
            
            // Re-fetch full record to get the new computedStageLabel and stageAutoComputed from backend
            this.refreshAfterUpdate();
          }
          this.savingClinical = false;
          this.showClinicalModal = false;
        },
        error: () => {
          alert('Erreur lors de la mise à jour des données cliniques.');
          this.savingClinical = false;
        }
      })
    );
  }

  private refreshAfterUpdate(): void {
    if (!this.selectedPatient || !this.doctorProfileId) return;
    this.recordSvc.getByPatient(this.selectedPatient.patientProfileId, this.doctorProfileId).subscribe({
      next: (rec) => {
        this.selectedRecord = rec;
        this.populateStagingData();
      }
    });
  }
}


