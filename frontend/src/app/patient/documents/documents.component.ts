import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  DocumentCategory,
  DocumentCountsDto,
  DocumentEventDto,
  DocumentResponseDto,
  DocumentUploadDto
} from '../../models/document.dto';
import { DocumentService } from '../../services/document.service';
import { AuthService } from '../../services/auth.service';
import { WebSocketService } from '../../services/websocket.service';
import { PatientProfileService } from '../../services/patient-profile.service';
import { LinkService } from '../../services/link.service';
import { UserService } from '../../services/user.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { DoctorFullResponseDto } from '../../models/doctor-full-response.dto';

interface LinkedDoctorOption {
  id: string;
  name: string;
  speciality: string;
  photo?: string | null;
}

@Component({
  selector: 'app-documents',
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
  uploadError = '';

  linkedDoctors: LinkedDoctorOption[] = [];
  selectedShareDoctorId = '';
  selectedFilterDoctorId = 'all';
  viewMode: 'all' | 'sent' | 'received' = 'all';
  loadingLinkedDoctors = false;
  linkedDoctorsError = '';
  sharing = false;

  loadingPreview = false;
  previewError = '';
  previewUrl: string | null = null;
  previewSafeUrl: SafeResourceUrl | null = null;
  previewMimeType = '';

  private patientId = '';
  private requesterId = '';
  private subs: Subscription[] = [];

  categories: Array<{ value: 'all' | DocumentCategory; label: string; icon: string }> = [
    { value: 'all', label: 'Tous', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2' },
    { value: 'compte-rendu', label: 'Comptes-rendus', icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z M14 2v6h6' },
    { value: 'ordonnance', label: 'Ordonnances', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2 M9 5a2 2 0 002 2h2a2 2 0 002-2' },
    { value: 'bilan', label: 'Bilans', icon: 'M22 12h-4l-3 9L9 3l-3 9H2' },
    { value: 'imagerie', label: 'Imagerie', icon: 'M2 7l4.41-4.41A2 2 0 017.83 2h8.34a2 2 0 011.42.59L22 7 M4 7v13a2 2 0 002 2h12a2 2 0 002-2V7' },
    { value: 'autre', label: 'Autres', icon: 'M4 6h16 M4 12h16 M4 18h7' },
  ];

  constructor(
    private readonly documentService: DocumentService,
    private readonly authService: AuthService,
    private readonly wsService: WebSocketService,
    private readonly patientProfileService: PatientProfileService,
    private readonly linkService: LinkService,
    private readonly userService: UserService,
    private readonly photoService: ProfilePhotoService,
    private readonly sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) {
      this.errorMessage = 'Impossible de charger les documents: utilisateur non connecté.';
      return;
    }

    this.requesterId = userId;
    this.resolvePatientProfileId(userId);
  }

  ngOnDestroy(): void {
    this.clearPreviewState();
    this.subs.forEach((s) => s.unsubscribe());
  }

  get filteredDocs(): DocumentResponseDto[] {
    return this.documents.filter((d) => {
      // Category filter
      const matchesCat = this.filterCategory === 'all' || d.category === this.filterCategory;
      
      // Search query filter
      const query = this.searchQuery.trim().toLowerCase();
      const matchesSearch = !query
        || d.name.toLowerCase().includes(query)
        || d.doctor.toLowerCase().includes(query);

      // Doctor filter
      let matchesDoctor = true;
      if (this.selectedFilterDoctorId !== 'all') {
        const selectedDoc = this.linkedDoctors.find(doc => doc.id === this.selectedFilterDoctorId);
        if (selectedDoc) {
          matchesDoctor = d.doctor.toLowerCase() === selectedDoc.name.toLowerCase();
        }
      }

      // Origin (Sent/Received) filter
      let matchesOrigin = true;
      if (this.viewMode === 'sent') {
        matchesOrigin = d.uploadedBy === 'patient';
      } else if (this.viewMode === 'received') {
        matchesOrigin = d.uploadedBy === 'doctor';
      }

      return matchesCat && matchesSearch && matchesDoctor && matchesOrigin;
    });
  }

  get sentCount(): number {
    return this.documents.filter(d => d.uploadedBy === 'patient').length;
  }

  get receivedCount(): number {
    return this.documents.filter(d => d.uploadedBy === 'doctor').length;
  }

  get selectedFilterDoctorData(): LinkedDoctorOption | null {
    if (this.selectedFilterDoctorId === 'all') return null;
    return this.linkedDoctors.find(d => d.id === this.selectedFilterDoctorId) ?? null;
  }

  get selectedFilterDoctorPhoto(): string | null {
    const doc = this.selectedFilterDoctorData;
    if (!doc?.photo) return null;
    return this.photoService.getPhotoUrl(doc.photo);
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

  openDocument(d: DocumentResponseDto): void {
    this.selectedDocument = d;
    this.selectedShareDoctorId = this.linkedDoctors[0]?.id ?? '';
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
    if (!this.patientId) {
      this.uploadError = 'Patient non identifié.';
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
      this.documentService.uploadPatientDocument(this.patientId, this.uploadFile, payload).subscribe({
        next: () => {
          this.uploading = false;
          this.showUploadModal = false;
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
    if (!this.requesterId) return;
    this.subs.push(
      this.documentService.downloadPatientDocument(doc.id).subscribe({
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
    this.subs.push(
      this.documentService.downloadPatientDocument(doc.id).subscribe({
        next: (response) => this.saveBlobResponse(response, doc.name),
        error: () => { this.actionMessage = 'Échec du téléchargement.'; }
      })
    );
  }

  deleteDocument(doc: DocumentResponseDto): void {
    if (!this.requesterId) {
      return;
    }

    const confirmed = window.confirm(`Supprimer le document "${doc.name}" ?`);
    if (!confirmed) {
      return;
    }

    this.subs.push(
      this.documentService.deletePatientDocument(doc.id, this.requesterId).subscribe({
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

  shareDocument(doc: DocumentResponseDto): void {
    if (!this.patientId || !this.selectedShareDoctorId) {
      return;
    }

    this.sharing = true;

    this.subs.push(
      this.documentService.sharePatientDocument(this.patientId, doc.id, this.selectedShareDoctorId).subscribe({
        next: () => {
          this.sharing = false;
          this.actionMessage = 'Document partagé avec succès.';
          this.refreshData();
        },
        error: () => {
          this.sharing = false;
          this.actionMessage = 'Partage impossible.';
        }
      })
    );
  }

  private refreshData(): void {
    this.loadDocuments();
    this.loadCounts();
  }

  private loadDocuments(): void {
    if (!this.patientId) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.subs.push(
      this.documentService.getPatientDocuments(this.patientId, this.page, this.size).subscribe({
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
    if (!this.patientId) {
      return;
    }

    this.subs.push(
      this.documentService.getPatientDocumentCounts(this.patientId).subscribe({
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
    if (!this.patientId) {
      return;
    }

    this.wsService.connect();
    this.wsService.subscribeToPatientDocuments(this.patientId);

    this.subs.push(
      this.wsService.documentEvents$.subscribe((event: DocumentEventDto) => {
        this.actionMessage = this.buildRealtimeMessage(event.type);
        this.refreshData();
      })
    );
  }

  private resolvePatientProfileId(userId: string): void {
    this.subs.push(
      this.patientProfileService.getByUserId(userId).subscribe({
        next: (profile) => {
          this.patientId = profile.id;
          this.refreshData();
          this.loadLinkedDoctors();
          this.initWebSocket();
        },
        error: () => {
          this.errorMessage = 'Profil patient introuvable.';
        }
      })
    );
  }

  private loadLinkedDoctors(): void {
    if (!this.patientId) {
      return;
    }

    this.loadingLinkedDoctors = true;
    this.linkedDoctorsError = '';

    this.subs.push(
      forkJoin({
        links: this.linkService.getConnected('patient', this.patientId),
        doctors: this.userService.getAllDoctors()
      }).subscribe({
        next: ({ links, doctors }) => {
          const linkedDoctorIds = new Set(links.map((l) => l.doctorProfileId));
          this.linkedDoctors = doctors
            .filter((d: DoctorFullResponseDto) => linkedDoctorIds.has(d.doctorProfileId))
            .map((d: DoctorFullResponseDto) => ({
              id: d.doctorProfileId,
              name: `Dr. ${d.firstName} ${d.lastName}`,
              speciality: d.speciality || this.formatDoctorType(d.doctorType),
              photo: d.profilePhotoUrl
            }));

          this.selectedShareDoctorId = this.linkedDoctors[0]?.id ?? '';
          this.loadingLinkedDoctors = false;
        },
        error: () => {
          this.loadingLinkedDoctors = false;
          this.linkedDoctors = [];
          this.selectedShareDoctorId = '';
          this.linkedDoctorsError = 'Impossible de charger la liste des médecins liés.';
        }
      })
    );
  }

  private formatDoctorType(type: string): string {
    const map: Record<string, string> = {
      GENERALIST: 'Généraliste',
      SURGEON: 'Chirurgien',
      ONCOLOGIST: 'Oncologue',
      RADIOLOGIST: 'Radiologue',
      PATHOLOGIST: 'Pathologiste',
      OTHER: 'Médecin'
    };
    return map[type] ?? 'Médecin';
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

    this.subs.push(
      this.documentService.downloadPatientDocument(doc.id).subscribe({
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

