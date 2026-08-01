import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription, forkJoin } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import {
  DocumentCategory,
  DocumentCountsDto,
  DocumentEventDto,
  DocumentResponseDto,
  DocumentStatus,
  DocumentUploadDto
} from '../../models/document.dto';
import { DocumentService } from '../../services/document.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { WebSocketService } from '../../services/websocket.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { PatientFullResponseDto } from '../../models/patient-full-response.dto';

@Component({
  selector: 'app-doctor-documents',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.css'
})
export class DocumentsComponent implements OnInit, OnDestroy {

  searchQuery = '';
  filterCategory = 'all';
  selectedDocument: DocumentResponseDto | null = null;

  documents: DocumentResponseDto[] = [];
  documentCounts: DocumentCountsDto = {};

  viewMode: 'all' | 'sent' | 'received' = 'all';
  loading = false;
  uploading = false;
  errorMessage = '';
  actionMessage = '';

  page = 0;
  size = 9;
  totalPages = 0;
  totalElements = 0;

  showUploadModal = false;
  uploadName = '';
  uploadCategory: DocumentCategory = 'autre';
  uploadPageCount = 1;
  uploadFile: File | null = null;
  uploadPatientId = '';
  uploadError = '';

  linkedPatients: Array<{ id: string; name: string; mrn: string; photo?: string | null; userId?: string }> = [];
  selectedPatientId = '';
  loadingLinkedPatients = false;
  linkedPatientsError = '';

  loadingPreview = false;
  previewError = '';
  previewUrl: string | null = null;
  previewSafeUrl: SafeResourceUrl | null = null;
  previewMimeType = '';

  private doctorId = '';
  private subs: Subscription[] = [];

  categories: Array<{ value: 'all' | DocumentCategory; label: string; icon: string }> = [
    { value: 'all', label: 'Tous', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2' },
    { value: 'compte-rendu', label: 'Comptes-rendus', icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z M14 2v6h6' },
    { value: 'ordonnance', label: 'Ordonnances', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2 M9 5a2 2 0 002 2h2a2 2 0 002-2' },
    { value: 'bilan', label: 'Bilans', icon: 'M22 12h-4l-3 9L9 3l-3 9H2' },
    { value: 'imagerie', label: 'Imagerie', icon: 'M2 7l4.41-4.41A2 2 0 017.83 2h8.34a2 2 0 011.42.59L22 7 M4 7v13a2 2 0 002 2h12a2 2 0 002-2V7' },
    { value: 'autre', label: 'Autres', icon: 'M4 6h16 M4 12h16 M4 18h7' }
  ];

  constructor(
    private readonly documentService: DocumentService,
    private readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly linkService: LinkService,
    private readonly wsService: WebSocketService,
    private readonly photoService: ProfilePhotoService,
    private readonly sanitizer: DomSanitizer,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) {
      this.errorMessage = 'Impossible de charger les documents: utilisateur non connecté.';
      return;
    }

    this.resolveDoctorProfileId(userId);
  }

  ngOnDestroy(): void {
    this.clearPreviewState();
    this.subs.forEach((s) => s.unsubscribe());
  }

  get filteredDocs(): DocumentResponseDto[] {
    return this.documents.filter((d) => {
      const matchesCat = this.filterCategory === 'all' || d.category === this.filterCategory;
      const query = this.searchQuery.trim().toLowerCase();
      const matchesSearch = !query
        || d.name.toLowerCase().includes(query)
        || d.category.toLowerCase().includes(query);

      // Origin filter (Doctor side: 'sent' = uploaded by doctor, 'received' = uploaded by patient)
      const matchesOrigin = this.viewMode === 'all' ||
        (this.viewMode === 'sent' && d.uploadedBy === 'doctor') ||
        (this.viewMode === 'received' && d.uploadedBy === 'patient');

      return matchesCat && matchesSearch && matchesOrigin;
    });
  }

  get sentCount(): number {
    return this.documents.filter(d => d.uploadedBy === 'doctor').length;
  }

  get receivedCount(): number {
    return this.documents.filter(d => d.uploadedBy === 'patient').length;
  }

  get selectedPatientName(): string {
    return this.linkedPatients.find((p) => p.id === this.selectedPatientId)?.name ?? 'Aucun patient';
  }

  get selectedPatientPhoto(): string | null {
    const p = this.linkedPatients.find((p) => p.id === this.selectedPatientId);
    if (!p?.photo) return null;
    return this.photoService.getPhotoUrl(p.photo);
  }

  get selectedPatientMrn(): string {
    const p = this.linkedPatients.find((p) => p.id === this.selectedPatientId);
    return p?.mrn ?? '—';
  }

  get validatedCount(): number {
    return this.documents.filter((d) => d.status === 'validated').length;
  }

  get pendingCount(): number {
    return this.documents.filter((d) => d.status === 'pending').length;
  }

  get imagerieCount(): number {
    return this.documentCounts['imagerie'] ?? this.getCategoryCount('imagerie');
  }

  get totalSizeLabel(): string {
    return `${this.totalElements} fichiers`;
  }

  get canPreviousPage(): boolean {
    return this.page > 0;
  }

  get canNextPage(): boolean {
    return this.page + 1 < this.totalPages;
  }

  get isPreviewPdf(): boolean {
    return this.previewMimeType === 'application/pdf';
  }

  get isPreviewImage(): boolean {
    return this.previewMimeType.startsWith('image/');
  }

  getCategoryLabel(c: string): string {
    const map: Record<string, string> = {
      'compte-rendu': 'Compte-rendu',
      ordonnance: 'Ordonnance',
      bilan: 'Bilan',
      imagerie: 'Imagerie',
      autre: 'Autre'
    };
    return map[c] ?? c;
  }

  getCategoryClass(c: string): string {
    return c.replace('-', '_');
  }

  getStatusLabel(s: string): string {
    return { validated: 'Validé', pending: 'En attente', archived: 'Archivé' }[s] ?? s;
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  getCategoryCount(c: 'all' | DocumentCategory): number {
    if (c === 'all') {
      return this.totalElements;
    }

    if (this.documentCounts[c] != null) {
      return this.documentCounts[c] as number;
    }

    return this.documents.filter((d) => d.category === c).length;
  }

  parsePaths(d: string): string[] {
    return d.split(' M ').map((s, i) => (i === 0 ? s : `M ${s}`)).filter(Boolean);
  }

  onPatientSelectionChange(): void {
    this.page = 0;
    this.refreshData();
  }

  openDocument(d: DocumentResponseDto): void {
    this.selectedDocument = d;
    this.loadDocumentPreview(d);
  }

  closeDocument(): void {
    this.selectedDocument = null;
    this.clearPreviewState();
  }

  retryLoad(): void {
    this.refreshData();
  }

  previousPage(): void {
    if (!this.canPreviousPage) {
      return;
    }
    this.page -= 1;
    this.loadDocuments();
  }

  nextPage(): void {
    if (!this.canNextPage) {
      return;
    }
    this.page += 1;
    this.loadDocuments();
  }

  goToPage(targetPage: number): void {
    if (targetPage < 0 || targetPage >= this.totalPages || targetPage === this.page) {
      return;
    }
    this.page = targetPage;
    this.loadDocuments();
  }

  pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  openUploadModal(): void {
    this.uploadError = '';
    this.uploadFile = null;
    this.uploadName = '';
    this.uploadPageCount = 1;
    this.uploadCategory = 'autre';
    this.uploadPatientId = this.selectedPatientId;
    this.showUploadModal = true;
  }

  closeUploadModal(): void {
    if (this.uploading) {
      return;
    }
    this.showUploadModal = false;
  }

  onUploadFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.uploadFile = file;
    if (file && !this.uploadName.trim()) {
      this.uploadName = this.stripExtension(file.name);
    }
  }

  submitUpload(): void {
    if (!this.doctorId) {
      this.uploadError = 'Médecin non identifié.';
      return;
    }
    if (!this.uploadPatientId) {
      this.uploadError = 'Veuillez sélectionner un patient.';
      return;
    }
    if (!this.uploadFile) {
      this.uploadError = 'Veuillez sélectionner un fichier.';
      return;
    }
    if (!this.uploadName.trim()) {
      this.uploadError = 'Le nom du document est requis.';
      return;
    }
    if (this.uploadPageCount < 1) {
      this.uploadError = 'Le nombre de pages doit être supérieur ou égal à 1.';
      return;
    }

    this.uploading = true;
    this.uploadError = '';

    const payload: DocumentUploadDto = {
      name: this.uploadName.trim(),
      category: this.uploadCategory,
      pageCount: this.uploadPageCount
    };

    this.subs.push(
      this.documentService.uploadDoctorPatientDocument(
        this.doctorId,
        this.uploadPatientId,
        this.uploadFile,
        payload
      ).subscribe({
        next: () => {
          this.uploading = false;
          this.showUploadModal = false;
          this.selectedPatientId = this.uploadPatientId;
          this.actionMessage = 'Document ajouté avec succès.';
          this.page = 0;
          this.refreshData();
        },
        error: () => {
          this.uploading = false;
          this.uploadError = 'Échec de l\'upload du document.';
        }
      })
    );
  }

  openDocumentInNewTab(doc: DocumentResponseDto): void {
    if (!this.doctorId) return;
    this.subs.push(
      this.documentService.downloadDoctorDocument(this.doctorId, doc.id).subscribe({
        next: (response) => {
          const blob = response.body;
          if (blob) {
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
            setTimeout(() => window.URL.revokeObjectURL(url), 30000); // 30s to be safe
          }
        },
        error: () => {
          this.actionMessage = 'Impossible de consulter le document.';
        }
      })
    );
  }

  downloadDocument(doc: DocumentResponseDto): void {
    if (!this.doctorId) return;
    this.subs.push(
      this.documentService.downloadDoctorDocument(this.doctorId, doc.id).subscribe({
        next: (response) => this.saveBlobResponse(response, doc.name),
        error: () => { this.actionMessage = 'Échec du téléchargement.'; }
      })
    );
  }

  deleteDocument(doc: DocumentResponseDto): void {
    if (!this.doctorId) {
      return;
    }

    const confirmed = window.confirm(`Supprimer le document "${doc.name}" ?`);
    if (!confirmed) {
      return;
    }

    this.subs.push(
      this.documentService.deleteDoctorDocument(this.doctorId, doc.id).subscribe({
        next: () => {
          this.actionMessage = 'Document supprimé.';
          if (this.selectedDocument?.id === doc.id) {
            this.closeDocument();
          }
          this.refreshData();
        },
        error: () => {
          this.actionMessage = 'Suppression impossible.';
        }
      })
    );
  }

  updateDocumentStatus(doc: DocumentResponseDto, status: DocumentStatus): void {
    if (!this.doctorId || doc.status === status) {
      return;
    }

    this.subs.push(
      this.documentService.updateDoctorDocumentStatus(this.doctorId, doc.id, status).subscribe({
        next: () => {
          this.actionMessage = status === 'validated'
            ? 'Document validé.'
            : 'Document archivé.';
          this.refreshData();
        },
        error: () => {
          this.actionMessage = 'Mise à jour du statut impossible.';
        }
      })
    );
  }

  private refreshData(): void {
    this.loadDocuments();
    this.loadCounts();
  }

  private loadDocuments(): void {
    if (!this.doctorId || !this.selectedPatientId) {
      this.documents = [];
      this.totalElements = 0;
      this.totalPages = 0;
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.subs.push(
      this.documentService.getDoctorPatientDocuments(this.doctorId, this.selectedPatientId, this.page, this.size).subscribe({
        next: (response) => {
          this.documents = response.content ?? [];
          this.totalElements = response.totalElements ?? 0;
          this.totalPages = response.totalPages ?? 0;
          this.page = response.number ?? this.page;
          this.loading = false;

          if (this.totalPages > 0 && this.page >= this.totalPages) {
            this.page = this.totalPages - 1;
            this.loadDocuments();
          }
        },
        error: () => {
          this.loading = false;
          this.errorMessage = 'Impossible de charger les documents.';
        }
      })
    );
  }

  private loadCounts(): void {
    if (!this.doctorId || !this.selectedPatientId) {
      this.documentCounts = {};
      return;
    }

    this.subs.push(
      this.documentService.getDoctorPatientDocumentCounts(this.doctorId, this.selectedPatientId).subscribe({
        next: (counts) => {
          this.documentCounts = counts || {};
        },
        error: () => {
          this.documentCounts = {};
        }
      })
    );
  }

  private initWebSocket(): void {
    if (!this.doctorId) {
      return;
    }

    this.wsService.connect();
    this.wsService.subscribeToDoctorDocuments(this.doctorId);

    this.subs.push(
      this.wsService.documentEvents$.subscribe((event: DocumentEventDto) => {
        this.actionMessage = this.buildRealtimeMessage(event.type);
        this.refreshData();
      })
    );
  }

  private resolveDoctorProfileId(userId: string): void {
    this.subs.push(
      this.userService.getDoctorByUserId(userId).subscribe({
        next: (doctor) => {
          this.doctorId = doctor.doctorProfileId;
          this.loadLinkedPatients();
          this.initWebSocket();
        },
        error: () => {
          this.errorMessage = 'Profil médecin introuvable.';
        }
      })
    );
  }

  private loadLinkedPatients(): void {
    if (!this.doctorId) {
      return;
    }

    this.loadingLinkedPatients = true;
    this.linkedPatientsError = '';

    this.subs.push(
      forkJoin({
        links: this.linkService.getConnected('doctor', this.doctorId),
        patients: this.userService.getAllPatients()
      }).subscribe({
        next: ({ links, patients }) => {
          const linkedPatientIds = new Set(links.map((l) => l.patientProfileId));
          this.linkedPatients = patients
            .filter((p: PatientFullResponseDto) => linkedPatientIds.has(p.patientProfileId))
            .map((p: PatientFullResponseDto) => ({
              id: p.patientProfileId,
              name: `${p.firstName} ${p.lastName}`,
              mrn: p.medicalRecordNumber ?? 'MRN non renseigné',
              photo: p.profilePhotoUrl,
              userId: p.userId
            }));

          // Deep-link check
          const qp = this.route.snapshot.queryParams;
          const targetUserId = qp['userId'];
          const targetId = qp['id'];

          if (targetUserId || targetId) {
            const found = this.linkedPatients.find(p => p.id === targetId || p.userId === targetUserId);
            if (found) {
              this.selectedPatientId = found.id;
            } else {
              this.selectedPatientId = this.linkedPatients[0]?.id ?? '';
            }
          } else {
            this.selectedPatientId = this.linkedPatients[0]?.id ?? '';
          }

          this.loadingLinkedPatients = false;
          this.refreshData();
        },
        error: () => {
          this.loadingLinkedPatients = false;
          this.linkedPatients = [];
          this.selectedPatientId = '';
          this.linkedPatientsError = 'Impossible de charger la liste des patients liés.';
          this.refreshData();
        }
      })
    );
  }

  private buildRealtimeMessage(type: DocumentEventDto['type']): string {
    switch (type) {
      case 'DOCUMENT_ADDED':
        return 'Nouveau document ajouté.';
      case 'DOCUMENT_SHARED':
        return 'Document partagé.';
      case 'DOCUMENT_DELETED':
        return 'Document supprimé.';
      case 'DOCUMENT_UPDATED':
        return 'Document mis à jour.';
      default:
        return 'Documents mis à jour.';
    }
  }

  private saveBlobResponse(response: HttpResponse<Blob>, fallbackName: string): void {
    const blob = response.body;
    if (!blob) {
      this.actionMessage = 'Fichier vide.';
      return;
    }

    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const matched = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(contentDisposition);
    const fileName = decodeURIComponent(matched?.[1] || matched?.[2] || `${fallbackName}.pdf`);

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }

  private loadDocumentPreview(doc: DocumentResponseDto): void {
    this.clearPreviewState();
    this.loadingPreview = true;
    if (!this.doctorId) return;

    this.subs.push(
      this.documentService.downloadDoctorDocument(this.doctorId, doc.id).subscribe({
        next: (response) => {
          const body = response.body;
          if (!body) {
            this.loadingPreview = false;
            this.previewError = 'Aperçu indisponible: fichier vide.';
            return;
          }

          const contentType = (response.headers.get('content-type') || body.type || '').toLowerCase();
          const fileName = this.extractFileName(response, doc.name);
          this.previewMimeType = this.resolvePreviewMimeType(contentType, fileName);

          const url = window.URL.createObjectURL(body);
          this.previewUrl = url;
          this.previewSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
          this.loadingPreview = false;
        },
        error: () => {
          this.loadingPreview = false;
          this.previewError = 'Impossible de charger l\'aperçu du document.';
        }
      })
    );
  }

  private clearPreviewState(): void {
    this.loadingPreview = false;
    this.previewError = '';
    this.previewMimeType = '';
    this.previewSafeUrl = null;

    if (this.previewUrl) {
      window.URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }

  private extractFileName(response: HttpResponse<Blob>, fallbackName: string): string {
    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const matched = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(contentDisposition);
    return decodeURIComponent(matched?.[1] || matched?.[2] || fallbackName);
  }

  private resolvePreviewMimeType(contentType: string, fileName: string): string {
    if (contentType && contentType !== 'application/octet-stream') {
      return contentType;
    }

    const extension = fileName.split('.').pop()?.toLowerCase() ?? '';
    const map: Record<string, string> = {
      pdf: 'application/pdf',
      png: 'image/png',
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
      gif: 'image/gif',
      webp: 'image/webp'
    };
    return map[extension] ?? contentType;
  }

  private stripExtension(fileName: string): string {
    const index = fileName.lastIndexOf('.');
    if (index <= 0) {
      return fileName;
    }
    return fileName.substring(0, index);
  }

}
