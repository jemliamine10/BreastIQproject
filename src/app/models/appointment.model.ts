export enum AppointmentType {
  CONSULTATION = 'CONSULTATION',
  EXAM = 'EXAM',
  TREATMENT = 'TREATMENT',
  FOLLOW_UP = 'FOLLOW_UP',
  OTHER = 'OTHER'
}

export enum AppointmentStatus {
  SCHEDULED = 'SCHEDULED',
  CANCELLED = 'CANCELLED',
  DONE = 'DONE',
  RESCHEDULED = 'RESCHEDULED'
}

export enum AppointmentMode {
  VIDEO = 'VIDEO',
  IN_PERSON = 'IN_PERSON'
}

export enum SlotStatus {
  AVAILABLE = 'AVAILABLE',
  BOOKED = 'BOOKED',
  BLOCKED = 'BLOCKED'
}

export enum TimelineStatus {
  COMPLETED = 'COMPLETED',
  ACTIVE = 'ACTIVE',
  UPCOMING = 'UPCOMING'
}

export interface AppointmentDoctor {
  id: string;
  firstName: string;
  lastName: string;
  specialty: string;
  contact: string;
  structure: string;
  timezone?: string;
}

export interface PatientAppointment {
  id: string;
  type: AppointmentType;
  title: string;
  description: string;
  date: string; // ISO 8601
  endDate: string; // ISO 8601
  status: AppointmentStatus;
  location: string;
  doctor: AppointmentDoctor;
  notes: string[];
  doctorTimezone?: string;
  rescheduledFrom?: string;
}

export interface AppointmentCreateRequestDto {
  linkId: string;
  startAt: string; // ISO 8601 UTC
  endAt: string; // ISO 8601 UTC
  mode: AppointmentMode;
}

export interface UpdatePatientAppointment {
  patientId?: string;
  title?: string;
  description?: string;
  date?: string; // ISO 8601
  endDate?: string; // ISO 8601
  location?: string;
  notes?: string[];
  status?: AppointmentStatus;
  timezone?: string;
}

export interface AppointmentStats {
  totalAppointments: number;
  totalDoctors: number;
  totalExams: number;
  progressPercentage: number;
}

export interface TimelineEvent {
  date: string; // ISO 8601
  type: AppointmentType;
  label: string;
  description: string;
  status: TimelineStatus;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size?: number;
}

export interface CalendarSlot {
  id: string;
  doctorId: string;
  date: string; // YYYY-MM-DD
  startAt: string; // ISO 8601
  endAt: string; // ISO 8601
  status: SlotStatus;
}

export interface DoctorAvailability {
  id: string;
  doctorId: string;
  dayOfWeek: string;
  startHour: string; // HH:mm
  endHour: string; // HH:mm
  slotDuration: number;
  isActive: boolean;
}

export interface AvailabilityException {
  id: string;
  doctorId: string;
  startDate: string; // YYYY-MM-DD
  startHour?: string; // HH:mm
  endHour?: string; // HH:mm
  reason?: string;
  isActive: boolean;
}

export interface StandardApiError {
  code?: string;
  message?: string;
  timestamp?: string;
  path?: string;
  details?: unknown;
}

export interface NextAppointmentResponse {
  nextAppointment: PatientAppointment | null;
}
