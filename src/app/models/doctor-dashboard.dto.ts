export interface DoctorDashboardDto {
  totalPatients: number;
  pendingRequests: number;
  unresolvedAlerts: number;
  criticalAlerts: number;
  appointmentsToday: number;
  avgHealthScore: number;
  unreadMessages: number;

  statusDistribution: Record<string, number>;
  stageDistribution: Record<string, number>;
  treatmentDistribution: Record<string, number>;

  healthTrend: HealthTrendPoint[];
  recentAlerts: AlertSummary[];
  todayAppointments: AppointmentSummary[];
  criticalPatients: PatientSummary[];
}

export interface HealthTrendPoint {
  date: string;
  avgScore: number;
}

export interface AlertSummary {
  id: string;
  severity: string;
  alertType: string;
  message: string;
  patientName: string;
  patientProfileId: string;
  createdAt: string;
}

export interface AppointmentSummary {
  id: string;
  title: string;
  patientName: string;
  patientProfileId: string;
  startAt: string;
  endAt: string;
  type: string;
  status: string;
  mode: string;
}

export interface PatientSummary {
  patientProfileId: string;
  firstName: string;
  lastName: string;
  healthScore: number;
  patientStatus: string;
  activeTreatment: string;
  cancerStage: string;
}
