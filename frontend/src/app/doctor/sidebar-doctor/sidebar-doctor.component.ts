import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { AppointmentService } from '../../services/appointment.service';
import { UserService } from '../../services/user.service';
import { AppointmentResponseDto } from '../../models/links-appointments.dto';
import { Subscription } from 'rxjs';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number | null;
  accent?: string | null;
}

interface NavGroup {
  title: string;
  key: string;
  icon: string;
  items: NavItem[];
  collapsed: boolean;
}

@Component({
  selector: 'app-sidebar-doctor',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar-doctor.component.html',
  styleUrl: './sidebar-doctor.component.css'
})
export class SidebarDoctorComponent implements OnInit {
  mobileOpen = false;
  collapsed = false;
  profileCompletion = 85;

  nextAppointment: AppointmentResponseDto | null = null;
  nextRdvDate = 'Chargement...';
  nextRdvSub = 'Patientez un instant';
  
  private subs: Subscription[] = [];

  navGroups: NavGroup[] = [
    {
      title: 'Mon Espace', key: 'workspace', collapsed: false,
      icon: 'M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z',
      items: [
        { label: 'Tableau de bord', icon: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10', route: '/doctor/dashboard' },
        { label: 'Calendrier & RDV', icon: 'M19 4H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V6a2 2 0 00-2-2z M16 2v4 M8 2v4 M3 10h18 M8 7V3m8 4V3m-9 8h10', route: '/doctor/schedule' },
      ]
    },
    {
      title: 'Gestion Patients', key: 'patients', collapsed: false,
      icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2',
      items: [
        { label: 'Tous les patients', icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2 M9 11a4 4 0 100-8 4 4 0 000 8z M23 21v-2a4 4 0 00-3-3.87 M16 3.13a4 4 0 010 7.75', route: '/doctor/patients' },
        { label: 'Ordonnances',       icon: 'M10.5 1.5H8.25A2.25 2.25 0 006 3.75v16.5a2.25 2.25 0 002.25 2.25h7.5A2.25 2.25 0 0018 20.25V3.75a2.25 2.25 0 00-2.25-2.25H13.5 M12 12.75v3 M10.5 14.25h3 M10.5 1.5v3h3v-3', route: '/doctor/treatment' },
        { label: 'Imagerie',          icon: 'M2 7l4.41-4.41A2 2 0 017.83 2h8.34a2 2 0 011.42.59L22 7 M4 7v13a2 2 0 002 2h12a2 2 0 002-2V7 M12 11a3 3 0 100 6 3 3 0 000-6z', route: '/doctor/imaging', accent: 'ai' },
        { label: 'Suivi',             icon: 'M22 12h-4l-3 9L9 3l-3 9H2', route: '/doctor/tracker' },
      ]
    },
    {
      title: 'Collaboration', key: 'collab', collapsed: false,
      icon: 'M2 3h6a4 4 0 014 4 4 4 0 014-4h6v18h-6a4 4 0 00-4 4 4 4 0 00-4-4H2z',
      items: [
        { label: 'Messages',        icon: 'M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z', route: '/doctor/messages', badge: 5 },
        { label: 'Documents',       icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8', route: '/doctor/documents' },
        { label: 'Demandes (Lien)', icon: 'M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71 M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71', route: '/doctor/links', badge: 2 },
      ]
    },
  ];

  constructor(
    private authService: AuthService,
    private profilePhotoService: ProfilePhotoService,
    private appointmentService: AppointmentService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.fetchNextAppointment();
  }

  fetchNextAppointment(): void {
    const userId = this.authService.currentUser?.id;
    if (!userId) return;

    this.subs.push(
      this.userService.getDoctorByUserId(userId).subscribe({
        next: (doctor) => {
          // SYNC PHOTO: ensure the top-level auth user has the latest photo URL from the DB
          const currentUser = this.authService.currentUser;
          if (currentUser && doctor.profilePhotoUrl && currentUser.profilePhotoUrl !== doctor.profilePhotoUrl) {
            currentUser.profilePhotoUrl = doctor.profilePhotoUrl;
            this.authService.updateCurrentUser(currentUser);
          }

          const doctorId = doctor.doctorProfileId;
          if (!doctorId) return;

          // Fetch only future appointments starting from NOW
          this.appointmentService.getDoctorAppointments(doctorId, new Date()).subscribe({
            next: (appointments) => {
              if (appointments && appointments.length > 0) {
                // The backend already returns them sorted by startAt Asc
                this.nextAppointment = appointments[0];
                this.updateNextRdvDisplay();
              } else {
                this.nextAppointment = null;
                this.nextRdvDate = 'Aucun RDV à venir';
                this.nextRdvSub = 'Agenda vide';
              }
            },
            error: (err) => {
              console.error('[Sidebar] Error fetching appointments:', err);
              this.nextRdvDate = 'Erreur agenda';
              this.nextRdvSub = 'Impossible de charger';
            }
          });
        }
      })
    );
  }

  updateNextRdvDisplay(): void {
    if (!this.nextAppointment) return;

    const start = new Date(this.nextAppointment.startAt);
    const hour = start.getHours().toString().padStart(2, '0');
    const min = start.getMinutes().toString().padStart(2, '0');
    const name = (this.nextAppointment.patientFirstName || '') + ' ' + (this.nextAppointment.patientLastName || 'Inconnu');
    
    // Format: "14 h 30 — Jean Dupont"
    this.nextRdvDate = `${hour} h ${min} — ${name.trim()}`;
    
    // Sub: Reason or "Consultation"
    this.nextRdvSub = this.nextAppointment.reason || 'Consultation de suivi';
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  get userName(): string {
    const u = this.authService.currentUser;
    // Show full name for doctor
    if (u?.firstName && u?.lastName) return `Dr. ${u.lastName}`;
    if (u?.lastName) return `Dr. ${u.lastName}`;
    return 'Dr. Profile';
  }

  get userInitials(): string {
    const u = this.authService.currentUser;
    if (!u) return 'Dr';
    const f = u.firstName?.charAt(0)?.toUpperCase() ?? '';
    const l = u.lastName?.charAt(0)?.toUpperCase() ?? '';
    return (f + l) || 'Dr';
  }

  get userEmail(): string {
    return this.authService.currentUser?.email ?? 'doctor@breastiq.com';
  }

  get profilePhotoUrl(): string {
    const u = this.authService.currentUser;
    const photoUrl = u?.profilePhotoUrl;
    if (!photoUrl) return '';
    return this.profilePhotoService.getPhotoUrl(photoUrl);
  }

  toggleMobile(): void { this.mobileOpen = !this.mobileOpen; }
  closeMobile(): void  { this.mobileOpen = false; }
  toggleCollapse(): void { this.collapsed = !this.collapsed; }
  toggleGroup(group: NavGroup): void { if (!this.collapsed) group.collapsed = !group.collapsed; }

  getStatusLabel(status: string | undefined): string {
    if (!status) return 'Inconnu';
    const labels: Record<string, string> = {
      'REQUESTED': 'À confirmer',
      'CONFIRMED': 'Confirmé',
      'UPCOMING': 'À venir',
      'CANCELLED': 'Annulé',
      'COMPLETED': 'Terminé',
      'NO_SHOW': 'Absent'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return '';
    return status.toLowerCase();
  }

  logout(): void {
    this.authService.logout();
    window.location.href = '/';
  }

  parsePaths(d: string): string[] {
    return d.split(/(?= M)/g).map(s => s.trim()).filter(Boolean);
  }
}
