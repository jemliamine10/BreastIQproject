export interface PatientDashboardDto {
  healthScore: number;
  patientStatus: string;
  profileCompletion: number;
  activeTreatmentCount: number;
  unresolvedAlerts: number;
  unreadMessages: number;
  documentCount: number;

  nextAppointment: AppointmentInfo | null;
  trackerTrend: TrackerPoint[];
  currentTreatment: TreatmentProgress | null;
  recentTimeline: TimelineItem[];
  recentAlerts: AlertInfo[];
}

export interface AppointmentInfo {
  id: string;
  title: string;
  startAt: string;
  endAt: string;
  type: string;
  status: string;
  mode: string;
  doctorFirstName: string;
  doctorLastName: string;
  speciality: string;
  location: string;
}

export interface TrackerPoint {
  date: string;
  painLevel: number | null;
  fatigueLevel: number | null;
  moodLevel: number | null;
  temperature: number | null;
}

export interface TreatmentProgress {
  id: string;
  treatmentType: string;
  protocol: string;
  status: string;
  currentCycle: number;
  totalCycles: number;
  startDate: string;
  endDate: string;
  completedSessions: number;
  totalSessions: number;
}

export interface TimelineItem {
  id: string;
  eventType: string;
  title: string;
  description: string;
  severity: string;
  eventDate: string;
}

export interface AlertInfo {
  id: string;
  severity: string;
  alertType: string;
  message: string;
  createdAt: string;
}
