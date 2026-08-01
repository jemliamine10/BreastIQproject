import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
// @ts-ignore
import html2pdf from 'html2pdf.js';

import {
  ImagingService,
  MammogramAnalysisResponse,
  MammogramAnalysisHistory,
  MammogramAnalysisDetail,
  MammogramPrediction,
  RiskPredictionRequest,
  RiskPredictionResponse
} from '../../services/imaging.service';
import { PatientFullResponseDto } from '../../models/patient-full-response.dto';

type ViewState = 'select-patient' | 'uploading' | 'analyzing' | 'result' | 'history-detail' | 'risk-prediction' | 'risk-result' | 'error';

@Component({
  selector: 'app-doctor-imaging',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './imaging.component.html',
  styleUrl: './imaging.component.css'
})
export class ImagingComponent implements OnInit, OnDestroy {

  state: ViewState = 'select-patient';
  dragOver = false;
  errorMessage = '';

  // Doctor info
  doctorProfileId = '';

  // AI service status
  aiAvailable = false;
  aiChecking = true;

  // Patient selection
  patients: PatientFullResponseDto[] = [];
  patientsLoading = true;
  patientSearch = '';
  selectedPatient: PatientFullResponseDto | null = null;

  // Upload
  selectedFile: File | null = null;
  previewUrl: string | null = null;

  // Analysis progress
  progressMessages = [
    'Préparation de l\'image…',
    'Détection des anomalies…',
    'Segmentation des régions…',
    'Classification des lésions…',
    'Analyse morphologique…',
    'Calcul du niveau de confiance…'
  ];
  currentProgressIndex = 0;
  progressInterval: any = null;

  // Results
  result: MammogramAnalysisResponse | null = null;
  selectedPredictionIndex = 0;
  activeView: 'annotated' | 'original' | 'segmentation' = 'annotated';

  // Report
  aiReport: string | null = null;
  reportGenerating = false;
  currentDate = new Date();
  doctorLastName = '';

  get parsedAiReport(): SafeHtml | null {
    if (!this.aiReport) return null;
    return this.sanitizer.bypassSecurityTrustHtml(marked.parse(this.aiReport) as string);
  }

  // History
  historyItems: MammogramAnalysisHistory[] = [];
  historyLoading = false;
  showHistory = false;

  // History detail view
  historyDetail: MammogramAnalysisDetail | null = null;
  historyDetailLoading = false;

  // Risk Prediction
  riskPredictionActive = false;
  riskPredictionLoading = false;
  riskResult: RiskPredictionResponse | null = null;
  riskServiceAvailable = false;
  riskChecking = true;
  currentRiskStep = 0;
  riskSteps = ['Profil Clinique', 'Architecture Tumorale', 'Empreinte Moléculaire', 'Parcours Thérapeutique'];
  riskFormAnimating = false;

  riskForm: RiskPredictionRequest = {
    type_of_breast_surgery: 'Unknown',
    cellularity: 'Unknown',
    chemotherapy: 'Unknown',
    pam50_claudin_low_subtype: 'Unknown',
    er_status_measured_by_ihc: 'Unknown',
    er_status: 'Unknown',
    neoplasm_histologic_grade: 2,
    her2_status_measured_by_snp6: 'Unknown',
    her2_status: 'Unknown',
    tumor_other_histologic_subtype: 'Unknown',
    hormone_therapy: 'Unknown',
    inferred_menopausal_state: 'Unknown',
    integrative_cluster: 'Unknown',
    primary_tumor_laterality: 'Unknown',
    lymph_nodes_examined_positive: 0,
    mutation_count: 5,
    nottingham_prognostic_index: 4,
    pr_status: 'Unknown',
    radio_therapy: 'Unknown',
    '3_gene_classifier_subtype': 'Unknown',
    tumor_size: 25,
    tumor_stage: 2
  };

  riskStats: any = null;

  private subs: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private linkService: LinkService,
    private imagingService: ImagingService,
    private photoService: ProfilePhotoService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.doctorLastName = this.authService.currentUser?.lastName || '';
    this.loadDoctorProfile();
    this.checkAiService();
    this.checkRiskService();
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.stopProgress();
  }

  // ═══════════ INIT ═══════════

  private loadDoctorProfile(): void {
    const userId = this.authService.currentUser?.id;
    console.log('[ImagingComponent] Loading doctor profile for userId:', userId);
    if (!userId) {
      console.warn('[ImagingComponent] No user ID found in AuthService');
      return;
    }

    this.subs.push(
      this.userService.getDoctorByUserId(userId).subscribe({
        next: (doc) => {
          console.log('[ImagingComponent] Doctor profile loaded:', doc);
          this.doctorProfileId = doc.doctorProfileId || '';
          if (this.doctorProfileId) {
            this.loadPatients();
          } else {
            console.error('[ImagingComponent] doctorProfileId is missing in profile!');
          }
        },
        error: (err) => console.error('[ImagingComponent] Error loading doctor profile:', err)
      })
    );
  }

  private checkAiService(): void {
    this.aiChecking = true;
    this.subs.push(
      this.imagingService.checkAiHealth().subscribe({
        next: (res) => {
          this.aiAvailable = res.aiServiceAvailable;
          this.aiChecking = false;
        },
        error: () => {
          this.aiAvailable = false;
          this.aiChecking = false;
        }
      })
    );
  }

  private checkRiskService(): void {
    this.riskChecking = true;
    this.subs.push(
      this.imagingService.checkRiskHealth().subscribe({
        next: (res) => {
          this.riskServiceAvailable = res.riskServiceAvailable;
          this.riskChecking = false;
        },
        error: () => {
          this.riskServiceAvailable = false;
          this.riskChecking = false;
        }
      })
    );
  }

  private loadPatients(): void {
    this.patientsLoading = true;
    this.subs.push(
      forkJoin({
        links: this.linkService.getConnected('doctor', this.doctorProfileId),
        allPatients: this.userService.getAllPatients()
      }).subscribe({
        next: ({ links, allPatients }) => {
          const activeLinks = links.filter(l => l.status === 'ACTIVE');
          const linkedIds = new Set(activeLinks.map(l => l.patientProfileId));
          this.patients = allPatients.filter(p => linkedIds.has(p.patientProfileId));
          this.patientsLoading = false;
        },
        error: () => { this.patientsLoading = false; }
      })
    );
  }

  // ═══════════ PATIENT SELECTION ═══════════

  get filteredPatients(): PatientFullResponseDto[] {
    if (!this.patientSearch) return this.patients;
    const term = this.patientSearch.toLowerCase();
    return this.patients.filter(p =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
      p.email?.toLowerCase().includes(term)
    );
  }

  selectPatient(patient: PatientFullResponseDto): void {
    console.log('[ImagingComponent] Patient selected:', patient);
    this.selectedPatient = patient;
    this.loadPatientHistory(patient.patientProfileId);
  }

  private loadPatientHistory(patientId: string): void {
    if (!this.doctorProfileId) return;
    this.historyLoading = true;
    this.subs.push(
      this.imagingService.getPatientHistory(this.doctorProfileId, patientId).subscribe({
        next: (items) => {
          this.historyItems = items;
          this.historyLoading = false;
        },
        error: () => {
          this.historyItems = [];
          this.historyLoading = false;
        }
      })
    );
  }

  deselectPatient(): void {
    this.selectedPatient = null;
    this.historyItems = [];
    this.resetAnalysis();
  }

  goToUpload(): void {
    this.state = 'uploading';
  }

  getPatientPhoto(p: PatientFullResponseDto): string | null {
    if (!p.profilePhotoUrl) return null;
    return this.photoService.getPhotoUrl(p.profilePhotoUrl);
  }

  getPatientInitials(p: PatientFullResponseDto): string {
    return (p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '');
  }

  getAge(dob?: string): number {
    if (!dob) return 0;
    const d = new Date(dob);
    const today = new Date();
    let age = today.getFullYear() - d.getFullYear();
    const m = today.getMonth() - d.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < d.getDate())) age--;
    return age;
  }

  // ═══════════ DRAG & DROP ═══════════

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  private handleFile(file: File): void {
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    const allowed = ['.png', '.jpg', '.jpeg', '.dcm', '.dicom'];
    if (!allowed.includes(ext)) {
      this.errorMessage = 'Format non supporté. Formats acceptés : PNG, JPG, JPEG, DICOM';
      this.state = 'error';
      return;
    }

    if (file.size > 60 * 1024 * 1024) {
      this.errorMessage = 'Le fichier est trop volumineux (max 60 Mo)';
      this.state = 'error';
      return;
    }

    this.selectedFile = file;

    if (ext !== '.dcm' && ext !== '.dicom') {
      const reader = new FileReader();
      reader.onload = () => { this.previewUrl = reader.result as string; };
      reader.readAsDataURL(file);
    } else {
      this.previewUrl = null;
    }

    this.startAnalysis();
  }

  // ═══════════ ANALYSIS ═══════════

  startAnalysis(): void {
    console.log('[ImagingComponent] startAnalysis triggered');
    if (!this.selectedFile || !this.doctorProfileId || !this.selectedPatient) {
      console.warn('[ImagingComponent] Missing required data for analysis:', {
        file: !!this.selectedFile,
        doctorId: this.doctorProfileId,
        patientId: this.selectedPatient?.patientProfileId
      });
      return;
    }

    this.state = 'analyzing';
    this.currentProgressIndex = 0;
    this.aiReport = null;
    this.startProgress();

    this.subs.push(
      this.imagingService.analyzeMammogram(
        this.doctorProfileId,
        this.selectedPatient.patientProfileId,
        this.selectedFile
      ).subscribe({
        next: (result) => {
          console.log('[ImagingComponent] Analysis success:', result);
          this.stopProgress();
          this.result = result;
          this.selectedPredictionIndex = 0;
          this.state = 'result';
          // Refresh history
          if (this.selectedPatient) {
            this.loadPatientHistory(this.selectedPatient.patientProfileId);
          }
          // ✅ AUTO-GENERATE REPORT after analysis completes
          if (result.analysisId && !this.aiReport) {
            this.generateReport();
          }
        },
        error: (err) => {
          console.error('[ImagingComponent] Analysis error:', err);
          this.stopProgress();
          this.errorMessage = err.error?.message || 'Une erreur est survenue lors de l\'analyse. Vérifiez que le service IA est démarré.';
          this.state = 'error';
        }
      })
    );
  }

  private startProgress(): void {
    this.progressInterval = setInterval(() => {
      if (this.currentProgressIndex < this.progressMessages.length - 1) {
        this.currentProgressIndex++;
      }
    }, 2500);
  }

  private stopProgress(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  // ═══════════ REPORT GENERATION ═══════════

  generateReport(): void {
    const analysisId = this.result?.analysisId || this.historyDetail?.id;
    if (!analysisId) return;

    this.reportGenerating = true;
    this.subs.push(
      this.imagingService.generateReport(analysisId).subscribe({
        next: (res) => {
          this.aiReport = res.report;
          this.reportGenerating = false;
          // Update history detail if in that view
          if (this.historyDetail) {
            this.historyDetail.aiReport = res.report;
          }
        },
        error: () => {
          this.aiReport = 'Erreur lors de la génération du rapport. Veuillez réessayer.';
          this.reportGenerating = false;
        }
      })
    );
  }

  async downloadPDF() {
    const reportElement = document.getElementById('pdf-real-content');
    if (!reportElement) {
      console.error('Template PDF introuvable.');
      return;
    }

    const opt = {
      margin:       10,
      filename:     `Rapport_SafeScan_${this.currentPatientLastName || 'patient'}.pdf`,
      image:        { type: 'jpeg' as const, quality: 0.98 },
      html2canvas:  { scale: 2, useCORS: true },
      jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' as const }
    };

    try {
      await html2pdf().set(opt).from(reportElement).save();
    } catch (err) {
      console.error('Erreur lors de la génération du PDF:', err);
    }
  }

  // ═══════════ HISTORY ═══════════

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory && !this.historyItems.length && this.selectedPatient) {
      this.loadPatientHistory(this.selectedPatient.patientProfileId);
    }
  }

  viewHistoryDetail(item: MammogramAnalysisHistory): void {
    this.historyDetailLoading = true;
    this.state = 'history-detail';
    this.subs.push(
      this.imagingService.getAnalysisDetail(item.id).subscribe({
        next: (detail) => {
          this.historyDetail = detail;
          this.aiReport = detail.aiReport;
          this.selectedPredictionIndex = 0;
          this.activeView = 'annotated';
          this.historyDetailLoading = false;
        },
        error: () => {
          this.errorMessage = 'Impossible de charger les détails de cette analyse.';
          this.state = 'error';
          this.historyDetailLoading = false;
        }
      })
    );
  }

  // ═══════════ RESULT HELPERS ═══════════

  get currentPredictions(): MammogramPrediction[] {
    if (this.state === 'history-detail' && this.historyDetail) {
      return this.historyDetail.individualPredictions || [];
    }
    return this.result?.individualPredictions || [];
  }

  get selectedPrediction(): MammogramPrediction | null {
    const preds = this.currentPredictions;
    if (!preds.length) return null;
    return preds[this.selectedPredictionIndex] || null;
  }

  get currentFullImage(): string | null {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.fullImage;
    return this.result?.fullImage ?? null;
  }

  get currentNormalImage(): string | null {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.fullNormalImage;
    return this.result?.fullNormalImage ?? null;
  }

  get currentSegImage(): string | null {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.segmentationImage;
    return this.result?.segmentationImage ?? null;
  }

  get currentVerdict(): string {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.globalVerdict;
    return this.result?.globalVerdict ?? '';
  }

  get currentConfidence(): number {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.globalConfidence;
    return this.result?.globalConfidence ?? 0;
  }

  get currentDetections(): boolean {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.detectionsCount > 0;
    return this.result?.detections ?? false;
  }

  get currentPatientFirstName(): string {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.patientFirstName;
    if (this.result?.patientFirstName) return this.result.patientFirstName;
    return this.selectedPatient?.firstName || 'Patient';
  }

  get currentPatientLastName(): string {
    if (this.state === 'history-detail' && this.historyDetail) return this.historyDetail.patientLastName;
    if (this.result?.patientLastName) return this.result.patientLastName;
    return this.selectedPatient?.lastName || '';
  }

  selectPrediction(index: number): void {
    this.selectedPredictionIndex = index;
  }

  get confidencePercent(): number {
    return Math.round(this.currentConfidence * 100);
  }

  get verdictClass(): string {
    switch (this.currentVerdict) {
      case 'Normal': return 'verdict-normal';
      case 'Bénin': return 'verdict-benign';
      case 'Malin': return 'verdict-malignant';
      case 'Mixte': return 'verdict-mixed';
      default: return 'verdict-unknown';
    }
  }

  get verdictIcon(): string {
    switch (this.currentVerdict) {
      case 'Normal': return 'M22 11.08V12a10 10 0 11-5.93-9.14 M22 4L12 14.01l-3-3';
      case 'Bénin': return 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z';
      case 'Malin': return 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z M12 9v4 M12 17h.01';
      case 'Mixte': return 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z';
      default: return 'M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z M12 16v-4 M12 8h.01';
    }
  }

  getScorePercent(score: number): number { return Math.round(score * 100); }

  getLabelFr(label: string): string {
    if (label === 'mass') return 'Masse';
    if (label === 'calc') return 'Calcification';
    return label;
  }

  getClassificationFr(cls: string): string {
    if (cls === 'Benign') return 'Bénin';
    if (cls === 'Malignant') return 'Malin';
    return cls;
  }

  getClassificationClass(cls: string): string {
    if (cls === 'Benign') return 'cls-benign';
    if (cls === 'Malignant') return 'cls-malignant';
    return '';
  }

  formatFeature(value: any): string {
    if (value === null || value === undefined || value !== value) return 'N/A';
    if (typeof value === 'number') return value.toFixed(3);
    return String(value);
  }

  parsePaths(d: string): string[] {
    return d.split(/(?= M)/g).map(s => s.trim()).filter(Boolean);
  }

  getVerdictClass(verdict: string): string {
    switch (verdict) {
      case 'Normal': return 'verdict-normal';
      case 'Bénin': return 'verdict-benign';
      case 'Malin': return 'verdict-malignant';
      case 'Mixte': return 'verdict-mixed';
      default: return 'verdict-unknown';
    }
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    try {
      return new Date(iso).toLocaleDateString('fr-FR', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return iso; }
  }

  // ═══════════ ACTIONS ═══════════

  resetAnalysis(): void {
    this.state = this.selectedPatient ? 'select-patient' : 'select-patient';
    this.selectedFile = null;
    this.previewUrl = null;
    this.result = null;
    this.historyDetail = null;
    this.aiReport = null;
    this.errorMessage = '';
    this.selectedPredictionIndex = 0;
    this.showHistory = false;
    this.stopProgress();
  }

  backToPatientView(): void {
    this.state = 'select-patient';
    this.selectedFile = null;
    this.previewUrl = null;
    this.result = null;
    this.historyDetail = null;
    this.aiReport = null;
    this.errorMessage = '';
    this.selectedPredictionIndex = 0;
    this.stopProgress();
  }

  retryAnalysis(): void {
    if (this.selectedFile) {
      this.startAnalysis();
    } else {
      this.backToPatientView();
    }
  }

  newAnalysis(): void {
    this.selectedFile = null;
    this.previewUrl = null;
    this.result = null;
    this.historyDetail = null;
    this.aiReport = null;
    this.selectedPredictionIndex = 0;
    this.state = 'uploading';
  }

  // ═══════════ RISK PREDICTION ACTIONS ═══════════

  goToRiskPrediction(): void {
    if (!this.selectedPatient) return;
    this.state = 'risk-prediction';
    this.currentRiskStep = 0;
    this.resetRiskForm();
    // Auto-fill logic is handled by the backend, but we could pre-fill UI fields if needed.
  }

  nextRiskStep(): void {
    if (this.currentRiskStep < this.riskSteps.length - 1) {
      this.riskFormAnimating = true;
      setTimeout(() => {
        this.currentRiskStep++;
        this.riskFormAnimating = false;
      }, 200);
    }
  }

  prevRiskStep(): void {
    if (this.currentRiskStep > 0) {
      this.riskFormAnimating = true;
      setTimeout(() => {
        this.currentRiskStep--;
        this.riskFormAnimating = false;
      }, 200);
    }
  }

  goToRiskStep(step: number): void {
    if (step >= 0 && step < this.riskSteps.length) {
      this.riskFormAnimating = true;
      setTimeout(() => {
        this.currentRiskStep = step;
        this.riskFormAnimating = false;
      }, 200);
    }
  }

  resetRiskForm(): void {
    this.riskForm = {
      type_of_breast_surgery: 'Unknown',
      cellularity: 'Unknown',
      chemotherapy: 'Unknown',
      pam50_claudin_low_subtype: 'Unknown',
      er_status_measured_by_ihc: 'Unknown',
      er_status: 'Unknown',
      neoplasm_histologic_grade: 2,
      her2_status_measured_by_snp6: 'Unknown',
      her2_status: 'Unknown',
      tumor_other_histologic_subtype: 'Unknown',
      hormone_therapy: 'Unknown',
      inferred_menopausal_state: 'Unknown',
      integrative_cluster: 'Unknown',
      primary_tumor_laterality: 'Unknown',
      lymph_nodes_examined_positive: 0,
      mutation_count: 5,
      nottingham_prognostic_index: 4,
      pr_status: 'Unknown',
      radio_therapy: 'Unknown',
      '3_gene_classifier_subtype': 'Unknown',
      tumor_size: 25,
      tumor_stage: 2
    };
  }

  submitRiskPrediction(): void {
    if (!this.selectedPatient || !this.riskServiceAvailable) return;

    this.riskPredictionLoading = true;
    this.subs.push(
      this.imagingService.predictRiskForPatient(this.selectedPatient.patientProfileId, this.riskForm).subscribe({
        next: (res) => {
          this.riskResult = res;
          this.riskPredictionLoading = false;
          this.state = 'risk-result';
        },
        error: (err) => {
          this.riskPredictionLoading = false;
          this.errorMessage = err.error?.message || 'Erreur lors de la prédiction de risque.';
          this.state = 'error';
        }
      })
    );
  }

  backToRiskForm(): void {
    this.state = 'risk-prediction';
  }
}
