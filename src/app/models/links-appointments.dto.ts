export type RequestedBy = 'PATIENT' | 'DOCTOR';
export type LinkStatus = 'REQUESTED' | 'ACTIVE' | 'REJECTED' | 'BLOCKED' | 'ENDED';
export type AppointmentTypeRDV = 'CONSULTATION' | 'EXAM' | 'TREATMENT' | 'FOLLOW_UP' | 'OTHER';
export type AppointmentStatusRDV = 'REQUESTED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW' | 'UPCOMING';
export type CalendarSlotStatus = 'AVAILABLE' | 'BOOKED' | 'BLOCKED';

export interface LinkRequestCreateDto {
  patientId?: string;
  doctorId?: string;
  patientProfileId?: string;
  doctorProfileId?: string;
  requestedBy: RequestedBy;
  requestNote?: string;
}

export interface LinkActionRequestDto {
  linkId: string;
  decisionByUserId?: string;
  rejectionReason?: string;
}

export interface LinkResponseDto {
  id: string;
  patientProfileId: string;
  doctorProfileId: string;
  status: LinkStatus;
  requestedBy: RequestedBy;
  requestNote?: string;
  decisionByUserId?: string;
  rejectionReason?: string;
  requestedAt?: string;
  activatedAt?: string;
  endedAt?: string;
  lastUpdatedAt?: string;
}

export interface AppointmentAvailabilityResponseDto {
  available: boolean;
  message: string;
}

export interface AppointmentCreateFrontRequestDto {
  patientId: string;
  doctorId: string;
  date: string;
  heure: string;
  typeRDV: AppointmentTypeRDV;
  title?: string;
  description?: string;
  location?: string;
  durationMinutes?: number;
}

export interface AppointmentResponseDto {
  id: string;
  linkId?: string;
  patientId?: string;
  doctorId?: string;
  patientProfileId?: string;
  doctorProfileId?: string;
  startAt: string;
  endAt: string;
  mode?: string;
  status: AppointmentStatusRDV;
  rescheduledFrom?: string;
  reason?: string;
  patientFirstName?: string;
  patientLastName?: string;
  patientNotes?: string;
  doctorNotes?: string;
  timezone?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AppointmentDoctorDto {
  id: string;
  firstName?: string;
  lastName?: string;
  specialty?: string;
  contact?: string;
  structure?: string;
}

export interface PatientAppointmentDto {
  id: string;
  type: AppointmentTypeRDV;
  title?: string;
  description?: string;
  date: string;
  endDate: string;
  status: AppointmentStatusRDV;
  location?: string;
  doctor?: AppointmentDoctorDto;
  notes: string[];
  rescheduledFrom?: string;
  doctorTimezone?: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CalendarSlotDto {
  id: string;
  doctorId: string;
  date: string;
  startAt: string;
  endAt: string;
  status: CalendarSlotStatus;
}

export interface DoctorAvailabilityDto {
  id: string;
  doctorId: string;
  dayOfWeek: string;
  startHour: string;
  endHour: string;
  slotDuration: number;
  isActive: boolean;
}

export interface AvailabilityExceptionDto {
  id: string;
  doctorId: string;
  startDate: string;
  startHour?: string;
  endHour?: string;
  reason?: string;
  isActive: boolean;
}
