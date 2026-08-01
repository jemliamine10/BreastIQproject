import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { AuthService } from '../../services/auth.service';
import { AppointmentResponseDto } from '../../models/links-appointments.dto';
import { Subscription, forkJoin } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { PatientFullResponseDto } from '../../models/patient-full-response.dto';
import { LinkResponseDto } from '../../models/links-appointments.dto';

interface DayRdv {
  date: Date;
  appointments: AppointmentResponseDto[];
  isCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
}

@Component({
  selector: 'app-calendrierdocteur',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './calendrierdocteur.component.html',
  styleUrls: ['./calendrierdocteur.component.css']
})
export class CalendrierDocteurComponent implements OnInit, OnDestroy {
  appointments: AppointmentResponseDto[] = [];
  filteredAppointments: AppointmentResponseDto[] = [];
  calendarDays: DayRdv[] = [];
  weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  // Patients & Links
  patients: PatientFullResponseDto[] = [];
  activeLinks: LinkResponseDto[] = [];
  selectedPatient: PatientFullResponseDto | null = null;
  
  // Date Management
  viewDate: Date = new Date();
  selectedDate: Date = new Date();
  
  // History
  patientHistory: any[] = [];
  selectedHistoryApp: any | null = null;
  historyLoading = false;
  showHistoryDetails = false;

  get currentMonthTitle(): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.viewDate);
  }

  get currentDayTitle(): string {
    return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(this.selectedDate);
  }
  
  // States
  isLoading = true;
  error = '';
  viewMode: 'month' | 'list' = 'month';

  // Filters
  filterStatus: string = '';
  filterPatientId: string = '';

  // Stats
  stats = {
    total: 0,
    upcoming: 0,
    done: 0,
    cancelled: 0
  };

  private sub: Subscription = new Subscription();

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private userSvc: UserService,
    private linkSvc: LinkService,
    private photoSvc: ProfilePhotoService
  ) { }

  ngOnInit(): void {
    this.sub.add(
      this.route.queryParams.subscribe(params => {
        const userId = params['userId'] || params['id'];
        if (userId) {
          this.filterPatientId = userId;
        }
        this.loadInitialData();
      })
    );
  }

  private loadInitialData(): void {
    const user = this.authService.currentUser;
    if (!user) return;
    const doctorId = (user as any).doctorProfileId || (user as any).profileId || user.id;
    if (!doctorId) return;

    this.isLoading = true;
    this.loadPatients(doctorId);
    this.loadAppointments();
  }

  private loadPatients(doctorId: string): void {
    this.sub.add(
      forkJoin({
        links: this.linkSvc.getConnected('doctor', doctorId),
        allPatients: this.userSvc.getAllPatients()
      }).subscribe({
        next: ({ links, allPatients }) => {
          this.activeLinks = links.filter(l => l.status === 'ACTIVE');
          const linkedIds = new Set(this.activeLinks.map(l => l.patientProfileId));
          this.patients = allPatients.filter(p => linkedIds.has(p.patientProfileId));
          
          if (this.filterPatientId) {
            this.selectedPatient = this.patients.find(p => p.patientProfileId === this.filterPatientId || p.userId === this.filterPatientId) || null;
          }
        },
        error: () => { /* fallback gracefully */ }
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  loadAppointments(): void {
    const user = this.authService.currentUser;
    if (!user) {
      this.error = "Utilisateur non connecté.";
      this.isLoading = false;
      return;
    }

    const doctorId = (user as any).doctorProfileId || (user as any).profileId || user.id;
    if (!doctorId) {
      this.error = 'Identifiant du médecin introuvable.';
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    this.error = '';

    // fetch appointments for the surrounding months to be safe
    const from = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() - 1, 1);
    const to = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + 2, 0);

    this.sub.add(
      this.appointmentService.getDoctorAppointments(doctorId, from, to).subscribe({
        next: (data: AppointmentResponseDto[]) => {
          this.appointments = data || [];
          this.applyFilters();
          this.isLoading = false;
        },
        error: (err: any) => {
          console.error(err);
          this.error = 'Impossible de charger vos rendez-vous.';
          this.isLoading = false;
        }
      })
    );
  }

  applyFilters(): void {
    this.filteredAppointments = this.appointments.filter(app => {
      let matches = true;
      if (this.filterStatus && app.status !== this.filterStatus) matches = false;
      
      // Filter by selection or MRN search
      if (this.selectedPatient) {
        if (app.patientProfileId !== this.selectedPatient.patientProfileId) matches = false;
      } else if (this.filterPatientId) {
        const term = this.filterPatientId.toLowerCase();
        const p = this.patients.find(pt => pt.patientProfileId === app.patientProfileId);
        const pName = p ? `${p.firstName} ${p.lastName}`.toLowerCase() : '';
        if (!pName.includes(term) && !app.patientProfileId?.toLowerCase().includes(term)) {
          matches = false;
        }
      }
      return matches;
    });

    this.calculateStats();
    this.generateCalendar();
  }

  calculateStats(): void {
    this.stats.total = this.filteredAppointments.length;
    this.stats.done = this.filteredAppointments.filter(a => a.status === 'COMPLETED').length;
    this.stats.cancelled = this.filteredAppointments.filter(a => a.status === 'CANCELLED').length;
    this.stats.upcoming = this.filteredAppointments.filter(a => a.status === 'CONFIRMED' || a.status === 'REQUESTED' || a.status === 'UPCOMING').length;
  }

  generateCalendar(): void {
    const year = this.viewDate.getFullYear();
    const month = this.viewDate.getMonth();
    
    // First day of month
    const firstDayOfMonth = new Date(year, month, 1);
    // Day of week of first day (0 = Sunday, we want 0 = Monday)
    let firstDayOfWeek = firstDayOfMonth.getDay();
    firstDayOfWeek = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1;
    
    // Start of calendar (might be previous month)
    const startDate = new Date(year, month, 1 - firstDayOfWeek);
    
    const days: DayRdv[] = [];
    const today = new Date();
    today.setHours(0,0,0,0);

    for (let i = 0; i < 42; i++) {
      const currentDate = new Date(startDate);
      currentDate.setDate(startDate.getDate() + i);
      
      const dayApps = this.filteredAppointments.filter(app => {
        if (!app.startAt) return false;
        const appDate = new Date(app.startAt);
        return appDate.getDate() === currentDate.getDate() &&
               appDate.getMonth() === currentDate.getMonth() &&
               appDate.getFullYear() === currentDate.getFullYear();
      }).sort((a,b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());

      days.push({
        date: currentDate,
        appointments: dayApps,
        isCurrentMonth: currentDate.getMonth() === month,
        isToday: currentDate.getTime() === today.getTime(),
        isSelected: currentDate.getTime() === this.selectedDate.getTime()
      });
    }
    
    this.calendarDays = days;
  }

  nextMonth(): void {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() + 1, 1);
    this.loadAppointments();
  }

  prevMonth(): void {
    this.viewDate = new Date(this.viewDate.getFullYear(), this.viewDate.getMonth() - 1, 1);
    this.loadAppointments();
  }

  goToToday(): void {
    this.viewDate = new Date();
    this.selectedDate = new Date();
    this.selectedDate.setHours(0,0,0,0);
    this.loadAppointments();
  }

  selectDate(day: DayRdv): void {
    this.selectedDate = new Date(day.date);
    this.calendarDays.forEach(d => d.isSelected = d.date.getTime() === this.selectedDate.getTime());
  }

  get selectedDateAppointments(): AppointmentResponseDto[] {
    const day = this.calendarDays.find(d => d.isSelected);
    return day ? day.appointments : [];
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED':
      case 'UPCOMING': 
        return 'status-scheduled';
      case 'COMPLETED': return 'status-completed';
      case 'CANCELLED': return 'status-cancelled';
      case 'REQUESTED': return 'status-rescheduled';
      case 'NO_SHOW': return 'status-cancelled';
      default: return 'status-default';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'CONFIRMED':
      case 'UPCOMING':
        return 'Prévu';
      case 'COMPLETED': return 'Terminé';
      case 'CANCELLED': return 'Annulé';
      case 'REQUESTED': return 'À confirmer';
      case 'NO_SHOW': return 'Absent';
      default: return status || 'Inconnu';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'CONFIRMED':
      case 'UPCOMING':
        return '#4f46e5';
      case 'COMPLETED': return '#10b981';
      case 'CANCELLED': return '#ef4444';
      case 'REQUESTED': return '#f59e0b';
      case 'NO_SHOW': return '#64748b';
      default: return '#94a3b8';
    }
  }

  formatTime(dateString: string | Date | undefined | null): string {
    if (!dateString) return '--:--';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return '--:--';
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${hours}:${minutes}`;
    } catch (e) {
      return '--:--';
    }
  }

  // --- UI Helpers ---
  selectPatient(p: PatientFullResponseDto | null): void {
    if (this.selectedPatient?.patientProfileId === p?.patientProfileId) {
      this.selectedPatient = null;
      this.patientHistory = [];
    } else {
      this.selectedPatient = p;
      if (p) {
        this.loadPatientHistory(p.patientProfileId);
      } else {
        this.patientHistory = [];
      }
    }
    this.applyFilters();
  }

  loadPatientHistory(patientProfileId: string): void {
    const user = this.authService.currentUser;
    const doctorId = (user as any)?.doctorProfileId || (user as any)?.profileId || user?.id;
    if (!doctorId || !patientProfileId) return;

    this.historyLoading = true;
    this.sub.add(
      this.appointmentService.getPatientAppointments({
        patientId: patientProfileId,
        doctorId: doctorId,
        size: 50 // Get a good history
      }).subscribe({
        next: (res) => {
          this.patientHistory = res.content || [];
          this.historyLoading = false;
        },
        error: (err) => {
          console.error('Error loading history:', err);
          this.historyLoading = false;
        }
      })
    );
  }

  viewHistoryDetails(app: any): void {
    this.selectedHistoryApp = app;
    this.showHistoryDetails = true;
  }

  closeHistoryDetails(): void {
    this.showHistoryDetails = false;
    this.selectedHistoryApp = null;
  }

  getPatientName(profileId?: string): string {
    if (!profileId) return 'Patient inconnu';
    const p = this.patients.find(pt => pt.patientProfileId === profileId);
    return p ? `${p.firstName} ${p.lastName}` : `Patient (${profileId.substring(0,8)})`;
  }

  getInitials(p: PatientFullResponseDto): string {
    return `${p.firstName?.[0] || ''}${p.lastName?.[0] || ''}`.toUpperCase();
  }

  getPatientPhoto(p: PatientFullResponseDto | string): string | null {
    if (typeof p === 'string') {
      const found = this.patients.find(pt => pt.patientProfileId === p);
      return found ? this.photoSvc.getPhotoUrl(found.userId) : null;
    }
    return this.photoSvc.getPhotoUrl(p.userId);
  }

  viewDetails(app: AppointmentResponseDto): void {
    const notes = app.patientNotes ? `\nNotes : ${app.patientNotes}` : '';
    // Optional: open a premium modal here
    console.log('Viewing details for:', app);
  }
}