import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import {
  AppointmentCreateRequestDto,
  AppointmentMode,
  AppointmentStats,
  AppointmentStatus,
  AppointmentType,
  CalendarSlot,
  PatientAppointment,
  SlotStatus,
  StandardApiError,
  TimelineEvent,
  TimelineStatus,
  UpdatePatientAppointment
} from '../../models/appointment.model';
import { PatientAppointmentService } from '../../services/patient-appointment.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { DoctorFullResponseDto } from '../../models/doctor-full-response.dto';
import { DoctorCalendarService } from '../../services/doctor-calendar.service';
import { PatientAppointmentManagementComponent } from './patient-appointment-management.component';
import { LinkResponseDto } from '../../models/links-appointments.dto';

interface CalendarDay {
  value: number;
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  events: PatientAppointment[];
}

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.css'
})
export class AppointmentsComponent implements OnInit {
  // Data
  appointments: PatientAppointment[] = [];
  nextAppointment: PatientAppointment | null = null;
  stats: AppointmentStats | null = null;
  timeline: TimelineEvent[] = [];

  // UI State
  viewMode: 'month' | 'list' = 'month';
  selectedDate: Date = new Date();
  activeMonth: Date = new Date();
  isLoading = false;
  loading = false; // legacy sync
  error: string | null = null;
  search = '';

  // Pagination
  page = 0;
  size = 50;
  totalElements = 0;
  totalPages = 0;

  // Calendar
  calendarDays: CalendarDay[] = [];
  readonly weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  // Appointment Creation & Links
  selectedDoctorId: string | null = null;
  selectedAction: string | null = null;
  patientId: string | null = null;
  isCreateModalOpen = false;
  connectedDoctors: DoctorFullResponseDto[] = [];
  connectedLinks: LinkResponseDto[] = [];
  selectedDoctorTimezone = 'UTC';
  selectedSlotId: string | null = null;
  dailySlots: CalendarSlot[] = [];
  slotsLoading = false;
  
  createForm = {
    doctorId: '',
    mode: AppointmentMode.VIDEO,
    type: AppointmentType.CONSULTATION,
    title: 'Nouveau rendez-vous',
    date: '',
    location: 'En ligne',
    description: ''
  };

  // Filters
  filterStatus = '';
  filterReason = '';

  constructor(
    private readonly appointmentService: PatientAppointmentService,
    private readonly doctorCalendarService: DoctorCalendarService,
    private readonly linkService: LinkService,
    private readonly userService: UserService,
    private readonly route: ActivatedRoute,
    private readonly authService: AuthService
  ) {
    this.activeMonth = new Date();
    this.activeMonth.setHours(0,0,0,0);
    this.selectedDate = new Date();
    this.selectedDate.setHours(0,0,0,0);
  }

  get currentMonthTitle(): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.activeMonth);
  }

  get currentDayTitle(): string {
    return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(this.selectedDate);
  }

  get filteredAppointments(): PatientAppointment[] {
    return this.appointments.filter(app => {
      let matches = true;
      if (this.filterStatus && app.status !== this.filterStatus) matches = false;
      if (this.filterReason && app.title && !app.title.toLowerCase().includes(this.filterReason.toLowerCase())) {
        matches = false;
      }
      return matches;
    });
  }

  get selectedDateAppointments(): PatientAppointment[] {
    const day = this.calendarDays.find(d => this.isSameDay(d.date, this.selectedDate));
    return day ? day.events : [];
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.selectedDoctorId = this.isUuid(params.get('doctorId') ?? '') ? params.get('doctorId') : null;
      this.selectedAction = params.get('action');
      this.resolvePatientId(params.get('patientId'));
    });
  }

  private resolvePatientId(routePatientId: string | null): void {
    if (routePatientId && this.isUuid(routePatientId)) {
      this.patientId = routePatientId;
      this.loadAll();
      this.loadConnectedDoctors();
      return;
    }

    const currentUser = this.authService.currentUser;
    if (!currentUser) {
      this.patientId = null;
      this.error = 'Identifiant patient invalide ou manquant.';
      return;
    }

    // Attempt to get patientProfileId from user object if available
    const profileId = (currentUser as any).patientProfileId || (currentUser as any).profileId;
    if (profileId) {
      this.patientId = profileId;
      this.loadAll();
      this.loadConnectedDoctors();
    } else {
      this.userService.getPatientByUserId(currentUser.id).subscribe({
        next: (patient) => {
          this.patientId = patient.patientProfileId;
          this.loadAll();
          this.loadConnectedDoctors();
        },
        error: (err) => {
          this.patientId = null;
          this.error = this.mapError(err);
        }
      });
    }
  }

  loadAll(): void {
    if (!this.patientId) {
      this.error = 'Identifiant patient invalide ou manquant.';
      this.isLoading = false;
      this.appointments = [];
      this.timeline = [];
      this.nextAppointment = null;
      this.stats = null;
      this.generateCalendar();
      return;
    }

    this.isLoading = true;
    this.error = '';

    forkJoin({
      appointmentsPage: this.appointmentService.getAppointments({
        patientId: this.patientId,
        page: this.page,
        size: this.size
      }),
      stats: this.appointmentService.getStats(this.patientId),
      timeline: this.appointmentService.getTimeline(this.patientId),
      nextAppointment: this.appointmentService.getNextAppointment(this.patientId).pipe(catchError(() => of(null)))
    }).pipe(
      finalize(() => {
        this.isLoading = false;
        this.loading = false; // sync with old property just in case
      })
    ).subscribe({
      next: ({ appointmentsPage, stats, timeline, nextAppointment }) => {
        this.appointments = appointmentsPage.content ?? [];
        this.totalElements = appointmentsPage.totalElements ?? 0;
        this.totalPages = appointmentsPage.totalPages ?? 0;
        this.page = appointmentsPage.number ?? 0;

        this.stats = stats;
        this.timeline = timeline ?? [];
        this.nextAppointment = nextAppointment?.nextAppointment ?? null;

        this.generateCalendar();
        this.loading = false;

        if (this.selectedAction === 'book') {
          this.openCreateModal();
          this.selectedAction = null;
        }
      },
      error: (err: any) => {
        this.error = this.mapError(err);
        this.appointments = [];
        this.timeline = [];
        this.nextAppointment = null;
        this.stats = null;
        this.generateCalendar();
        this.loading = false;
      }
    });
  }

  onSearch(value: string): void {
    this.search = value.trim();
    this.page = 0;
    this.loadAll();
  }

  previousMonth(): void {
    this.activeMonth = new Date(this.activeMonth.getFullYear(), this.activeMonth.getMonth() - 1, 1);
    this.loadAll();
  }

  nextMonth(): void {
    this.activeMonth = new Date(this.activeMonth.getFullYear(), this.activeMonth.getMonth() + 1, 1);
    this.loadAll();
  }

  goToToday(): void {
    this.activeMonth = new Date();
    this.selectedDate = new Date();
    this.selectedDate.setHours(0,0,0,0);
    this.loadAll();
  }

  selectDate(day: CalendarDay): void {
    this.selectedDate = new Date(day.date);
    this.calendarDays.forEach(d => d.isToday = this.isSameDay(d.date, new Date())); // Ensure today status is correct
    // In our system, selection is visual through isSameDay(d.date, selectedDate) in HTML
  }

  createQuickAppointment(): void {
    this.error = 'Utilisez le formulaire de création avec un créneau disponible.';
  }

  openCreateModal(): void {
    const firstDoctorId = this.availableDoctors[0]?.id ?? '';
    const now = new Date();
    now.setMinutes(0, 0, 0);

    this.createForm = {
      doctorId: this.selectedDoctorId ?? firstDoctorId,
      mode: AppointmentMode.VIDEO,
      type: AppointmentType.CONSULTATION,
      title: 'Nouveau rendez-vous',
      date: this.toIsoDate(now),
      location: 'En ligne',
      description: ''
    };
    this.selectedSlotId = null;
    this.dailySlots = [];
    this.syncDoctorTimezone();
    this.isCreateModalOpen = true;
    this.loadSlotsForModal();
  }

  closeCreateModal(): void {
    this.isCreateModalOpen = false;
    this.selectedSlotId = null;
    this.dailySlots = [];
  }

  createFromForm(payload: AppointmentCreateRequestDto): void {
    if (!this.patientId) {
      this.error = 'Identifiant patient invalide ou manquant.';
      return;
    }

    this.loading = true;
    this.error = null;

    this.appointmentService.createAppointment(payload)
      .pipe(finalize(() => {
        this.loading = false;
        this.isLoading = false;
      }))
      .subscribe({
        next: () => {
          this.closeCreateModal();
          this.loadAll();
        },
        error: (err: any) => {
          this.error = this.mapError(err);
        }
      });
  }

  submitCreateForm(): void {
    const trimmedDoctorId = this.createForm.doctorId.trim();
    const linkId = this.getSelectedLinkId(trimmedDoctorId);
    const selectedSlot = this.dailySlots.find((slot) => slot.id === this.selectedSlotId);

    if (!trimmedDoctorId || !this.createForm.date || !this.createForm.mode) {
      this.error = 'Veuillez completer les champs requis du rendez-vous.';
      return;
    }

    if (!selectedSlot || selectedSlot.status !== SlotStatus.AVAILABLE) {
      this.error = 'Veuillez choisir un creneau disponible pour continuer.';
      return;
    }

    if (!linkId || !this.isUuid(linkId)) {
      this.error = 'Lien patient-medecin invalide ou introuvable.';
      return;
    }

    const allowedDoctor = this.connectedDoctors.some((doctor) => doctor.doctorProfileId === trimmedDoctorId);
    if (!allowedDoctor) {
      this.error = 'Le medecin selectionne doit etre connecte a votre profil.';
      return;
    }

    if (!this.patientId) {
      this.error = 'Identifiant patient invalide ou manquant.';
      return;
    }

    this.loading = true;
    this.error = null;

    const startAt = this.toUtcIsoString(selectedSlot.startAt);
    const endAt = this.toUtcIsoString(selectedSlot.endAt);

    if (!startAt || !endAt) {
      this.error = 'Format de date invalide pour le creneau selectionne.';
      return;
    }

    if (new Date(startAt).getTime() >= new Date(endAt).getTime()) {
      this.error = 'Le creneau selectionne doit avoir une fin apres le debut.';
      return;
    }

    const payload: AppointmentCreateRequestDto = {
      linkId,
      startAt,
      endAt,
      mode: this.createForm.mode
    };

    this.appointmentService.createAppointment(payload)
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: () => {
          this.closeCreateModal();
          this.loadAll();
        },
      error: (err: any) => {
        this.error = this.mapError(err);
      }
    });
  }

  get appointmentTypes(): AppointmentType[] {
    return Object.values(AppointmentType);
  }

  get availableDoctors(): { id: string; firstName: string; lastName: string; specialty?: string }[] {
    return this.connectedDoctors.map((doctor) => ({
      id: doctor.doctorProfileId,
      firstName: doctor.firstName ?? 'Dr.',
      lastName: doctor.lastName ?? doctor.doctorProfileId,
      specialty: doctor.speciality
    }));
  }

  viewDetailsAndConfirm(appointment: PatientAppointment): void {
    this.loading = true;
    this.error = null;

    this.appointmentService.getAppointmentDetails(appointment.id).subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.error = this.mapError(err);
      }
    });
  }

  cancelFromFeed(appointment: PatientAppointment): void {
    this.loading = true;
    this.error = null;

    this.appointmentService.cancelAppointment(appointment.id)
      .pipe(finalize(() => {
        this.isLoading = false;
        this.loading = false;
      }))
      .subscribe({
        next: () => this.loadAll(),
        error: (err: any) => {
          this.error = this.mapError(err);
        }
      });
  }

  private updateAppointment(id: string, payload: UpdatePatientAppointment): void {
    if (!this.patientId) {
      this.error = 'Identifiant patient invalide ou manquant.';
      return;
    }

    this.appointmentService.updateAppointment(id, payload)
      .pipe(finalize(() => {
        this.isLoading = false;
        this.loading = false;
      }))
      .subscribe({
        next: () => this.loadAll(),
        error: (err: any) => {
          this.error = this.mapError(err);
        }
      });
  }

  private generateCalendar(): void {
    const year = this.activeMonth.getFullYear();
    const month = this.activeMonth.getMonth();
    
    // First day of month
    const firstDayOfMonth = new Date(year, month, 1);
    // Day of week of first day (0 = Sunday, we want 0 = Monday)
    let firstDayOfWeek = firstDayOfMonth.getDay();
    firstDayOfWeek = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1;
    
    // Start of calendar (might be previous month)
    const startDate = new Date(year, month, 1 - firstDayOfWeek);
    
    const days: CalendarDay[] = [];
    const today = new Date();
    today.setHours(0,0,0,0);

    for (let i = 0; i < 42; i++) {
      const currentDate = new Date(startDate);
      currentDate.setDate(startDate.getDate() + i);
      
      const dayApps = this.filteredAppointments.filter(app => {
        if (!app.date) return false;
        const appDate = new Date(app.date);
        return appDate.getDate() === currentDate.getDate() &&
               appDate.getMonth() === currentDate.getMonth() &&
               appDate.getFullYear() === currentDate.getFullYear();
      }).sort((a,b) => new Date(a.date).getTime() - new Date(b.date).getTime());

      days.push({
        value: currentDate.getDate(),
        date: currentDate,
        isCurrentMonth: currentDate.getMonth() === month,
        isToday: currentDate.getTime() === today.getTime(),
        events: dayApps
      });
    }
    
    this.calendarDays = days;
  }

  private getEventsForDay(date: Date): PatientAppointment[] {
    return this.appointments
      .filter((appointment) => this.isSameDay(new Date(appointment.date), date))
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
      .slice(0, 2);
  }

  private normalizeWeekDay(day: number): number {
    return day === 0 ? 6 : day - 1;
  }

  private isSameDay(left: Date, right: Date): boolean {
    return left.getFullYear() === right.getFullYear()
      && left.getMonth() === right.getMonth()
      && left.getDate() === right.getDate();
  }

  private isUpcoming(appointment: PatientAppointment): boolean {
    const date = new Date(appointment.date).getTime();
    return date >= new Date().setHours(0, 0, 0, 0) && appointment.status === AppointmentStatus.SCHEDULED;
  }

  getBadgeText(type: AppointmentType): string {
    switch (type) {
      case AppointmentType.CONSULTATION:
        return 'Consultation';
      case AppointmentType.EXAM:
        return 'Examen';
      case AppointmentType.TREATMENT:
        return 'Traitement';
      case AppointmentType.FOLLOW_UP:
        return 'Contrôle';
      default:
        return 'Rendez-vous';
    }
  }

  getBadgeStyle(type: AppointmentType): { background: string; color: string } {
    switch (type) {
      case AppointmentType.CONSULTATION:
        return { background: '#ede9fe', color: '#5b21b6' };
      case AppointmentType.EXAM:
        return { background: '#e0f2fe', color: '#075985' };
      case AppointmentType.TREATMENT:
        return { background: '#fee2e2', color: '#991b1b' };
      case AppointmentType.FOLLOW_UP:
        return { background: '#dcfce7', color: '#166534' };
      default:
        return { background: '#f3f4f6', color: '#374151' };
    }
  }

  getEventClass(type: AppointmentType): string {
    switch (type) {
      case AppointmentType.CONSULTATION:
        return 'ev-vio';
      case AppointmentType.EXAM:
        return 'ev-blue';
      case AppointmentType.TREATMENT:
        return 'ev-pink';
      default:
        return 'ev-vio';
    }
  }

  getTimelineClass(status: TimelineStatus): string {
    if (status === TimelineStatus.COMPLETED) return 'completed';
    if (status === TimelineStatus.ACTIVE) return 'active';
    return '';
  }

  formatDateTime(isoDate: string | undefined): string {
    if (!isoDate) {
      return '—';
    }
    return new Date(isoDate).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short'
    }) + ' · ' + new Date(isoDate).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  formatDateLabel(isoDate: string): string {
    return new Date(isoDate).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long'
    });
  }

  formatDayTime(isoDate: string): string {
    const date = new Date(isoDate);
    const now = new Date();
    const sameDay = this.isSameDay(date, now);
    const time = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    if (sameDay) {
      return `Auj · ${time}`;
    }
    return `${this.formatDateLabel(isoDate)} · ${time}`;
  }

  formatSlotTime(isoDate: string): string {
    return new Date(isoDate).toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: this.selectedDoctorTimezone
    });
  }

  onDoctorChange(): void {
    this.syncDoctorTimezone();
    this.selectedSlotId = null;
    this.loadSlotsForModal();
  }

  onCreateDateChange(): void {
    this.selectedSlotId = null;
    this.loadSlotsForModal();
  }

  selectSlot(slot: CalendarSlot): void {
    if (slot.status !== SlotStatus.AVAILABLE) {
      return;
    }
    this.selectedSlotId = slot.id;
  }

  isSlotSelected(slot: CalendarSlot): boolean {
    return slot.id === this.selectedSlotId;
  }

  slotStatusLabel(status: SlotStatus): string {
    if (status === SlotStatus.AVAILABLE) return 'Disponible';
    if (status === SlotStatus.BOOKED) return 'Occupe';
    return 'Bloque';
  }

  slotStatusClass(status: SlotStatus): string {
    if (status === SlotStatus.AVAILABLE) return 'slot-available';
    if (status === SlotStatus.BOOKED) return 'slot-booked';
    return 'slot-blocked';
  }

  private startOfVisibleMonthIso(): string {
    const date = new Date(this.activeMonth.getFullYear(), this.activeMonth.getMonth(), 1, 0, 0, 0, 0);
    return date.toISOString();
  }

  private endOfVisibleMonthIso(): string {
    const date = new Date(this.activeMonth.getFullYear(), this.activeMonth.getMonth() + 1, 0, 23, 59, 59, 999);
    return date.toISOString();
  }

  private mapError(err: any): string {
    const apiError = err?.error as StandardApiError | undefined;
    if (err?.status === 0) return 'Réseau indisponible. Vérifiez votre connexion.';
    if (err?.status === 400 && apiError?.message) return apiError.message;
    if (err?.status >= 500) return 'Erreur serveur. Veuillez réessayer.';
    return 'Une erreur inattendue est survenue.';
  }

  private isUuid(value: string): boolean {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidRegex.test(value);
  }

  private toDateTimeLocal(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private loadConnectedDoctors(): void {
    if (!this.patientId) {
      return;
    }

    forkJoin({
      links: this.linkService.getConnected('patient', this.patientId),
      doctors: this.userService.getAllDoctors()
    }).subscribe({
      next: ({ links, doctors }) => {
        this.connectedLinks = links;
        const linkedDoctorIds = new Set(links.map((link) => link.doctorProfileId));
        this.connectedDoctors = doctors.filter((doctor) => linkedDoctorIds.has(doctor.doctorProfileId));

        if (this.selectedDoctorId && !linkedDoctorIds.has(this.selectedDoctorId)) {
          this.selectedDoctorId = null;
        }

        this.syncDoctorTimezone();
      },
      error: (err) => {
        this.connectedLinks = [];
        this.connectedDoctors = [];
        this.error = this.mapError(err);
      }
    });
  }

  private getSelectedLinkId(doctorProfileId: string): string | null {
    const link = this.connectedLinks.find((item) => item.doctorProfileId === doctorProfileId);
    return link?.id ?? null;
  }

  private syncDoctorTimezone(): void {
    const selected = this.connectedDoctors.find((doctor) => doctor.doctorProfileId === this.createForm.doctorId)
      ?? this.connectedDoctors.find((doctor) => doctor.doctorProfileId === this.selectedDoctorId);
    this.selectedDoctorTimezone = selected?.timezone || 'UTC';
  }

  private loadSlotsForModal(): void {
    const doctorId = this.createForm.doctorId?.trim();
    const date = this.createForm.date;
    if (!doctorId || !date) {
      this.dailySlots = [];
      return;
    }

    this.slotsLoading = true;
    this.doctorCalendarService.getCalendarSlots(doctorId, date)
      .pipe(finalize(() => {
        this.slotsLoading = false;
      }))
      .subscribe({
        next: (slots) => {
          this.dailySlots = this.normalizeCalendarSlots(slots)
            .sort((a, b) => a.startAt.localeCompare(b.startAt));
        },
        error: (err) => {
          this.dailySlots = [];
          this.error = this.mapError(err);
        }
      });
  }

  private normalizeCalendarSlots(payload: unknown): CalendarSlot[] {
    const items = this.extractArray(payload, ['slots', 'content', 'data']);
    return items
      .map((item, index) => this.toCalendarSlot(item, index))
      .filter((slot): slot is CalendarSlot => slot !== null);
  }

  private extractArray(payload: unknown, keys: string[]): unknown[] {
    if (Array.isArray(payload)) {
      return payload;
    }

    if (!payload || typeof payload !== 'object') {
      return [];
    }

    const source = payload as Record<string, unknown>;
    for (const key of keys) {
      const candidate = source[key];
      if (Array.isArray(candidate)) {
        return candidate;
      }
    }

    return [];
  }

  private toCalendarSlot(item: unknown, index: number): CalendarSlot | null {
    if (!item || typeof item !== 'object') {
      return null;
    }

    const source = item as Record<string, unknown>;
    const rawDate = this.readString(source, 'date') ?? this.createForm.date;
    const startAt = this.readString(source, 'startAt')
      ?? this.readString(source, 'startTime')
      ?? this.buildIsoDateTime(rawDate, this.readString(source, 'startHour'));
    const endAt = this.readString(source, 'endAt')
      ?? this.readString(source, 'endTime')
      ?? this.buildIsoDateTime(rawDate, this.readString(source, 'endHour'));

    if (!startAt || !endAt) {
      return null;
    }

    const statusValue = (this.readString(source, 'status') ?? SlotStatus.BLOCKED).toUpperCase();
    const status = statusValue === SlotStatus.AVAILABLE
      ? SlotStatus.AVAILABLE
      : statusValue === SlotStatus.BOOKED
        ? SlotStatus.BOOKED
        : SlotStatus.BLOCKED;

    return {
      id: this.readString(source, 'id') ?? `slot-${index}-${startAt}`,
      doctorId: this.readString(source, 'doctorId') ?? this.createForm.doctorId,
      date: rawDate ?? startAt.slice(0, 10),
      startAt,
      endAt,
      status
    };
  }

  private readString(source: Record<string, unknown>, key: string): string | undefined {
    const value = source[key];
    return typeof value === 'string' && value.trim() ? value : undefined;
  }

  private buildIsoDateTime(date: string | undefined, time: string | undefined): string | undefined {
    if (!date || !time) {
      return undefined;
    }

    const normalizedTime = time.length === 5 ? `${time}:00` : time;
    return `${date}T${normalizedTime}`;
  }

  private toUtcIsoString(value: string): string | null {
    if (!value || typeof value !== 'string') {
      return null;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }

    return date.toISOString();
  }

}

