export type DocumentCategory = 'compte-rendu' | 'ordonnance' | 'bilan' | 'imagerie' | 'autre';

export type DocumentStatus = 'validated' | 'pending' | 'archived';

export interface DocumentResponseDto {
  id: string;
  name: string;
  category: DocumentCategory;
  date: string;
  doctor: string;
  size: string;
  pages?: number;
  status: DocumentStatus;
  uploadedBy: 'patient' | 'doctor';
}

export interface DocumentUploadDto {
  name: string;
  category: DocumentCategory;
  pageCount: number;
}

export interface DocumentStatusUpdateDto {
  status: DocumentStatus;
}

export type DocumentEventType =
  | 'DOCUMENT_ADDED'
  | 'DOCUMENT_SHARED'
  | 'DOCUMENT_DELETED'
  | 'DOCUMENT_UPDATED';

export interface DocumentEventDto {
  type: DocumentEventType;
  document: DocumentResponseDto;
}

export type DocumentCountsDto = Partial<Record<DocumentCategory, number>>;

export interface DocumentPageResponseDto {
  content: DocumentResponseDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
