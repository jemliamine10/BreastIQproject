export interface AiChatRequest {
  sessionId: string;
  message: string;
  patientUserId?: string;
}

export interface AiDoctorSuggestion {
  doctorProfileId: string;
  userId: string;
  fullName: string;
  speciality: string;
  imageUrl?: string;
  consultationMode?: string;
  availableToday: boolean;
}

export interface AiChatAppointment {
  id: string;
  title: string;
  startAt: string;
  endAt: string;
  doctorName?: string;
  type: string;
  status: string;
  location?: string;
}

export interface AiChatTreatment {
  id: string;
  treatmentType: string;
  protocol?: string;
  status: string;
  currentCycle?: number;
  totalCycles?: number;
  startDate?: string;
  endDate?: string;
}

export interface AiChatAlert {
  id: string;
  severity: string;
  alertType: string;
  message: string;
  createdAt: string;
}

export interface AiChatResponse {
  reply: string;
  doctors?: AiDoctorSuggestion[];
  nextAppointment?: AiChatAppointment;
  activeTreatments?: AiChatTreatment[];
  recentAlerts?: AiChatAlert[];
  connectedDoctors?: AiDoctorSuggestion[];
}