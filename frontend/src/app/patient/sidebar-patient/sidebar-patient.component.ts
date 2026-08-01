import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfilePhotoService } from '../../services/profile-photo.service';
import { PatientAppointmentService } from '../../services/patient-appointment.service';
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
  icon: string;          // small section icon
  items: NavItem[];
  collapsed: boolean;
}

@Component({
  selector: 'app-sidebar-patient',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar-patient.component.html',
  styleUrl: './sidebar-patient.component.css'
})
export class SidebarPatientComponent implements OnInit, OnDestroy {
  mobileOpen = false;
  collapsed = false;
  profileCompletion = 70;
  
  nextRdvDate = 'Chargement...';
  nextRdvSub = 'Patientez un instant';
  nextRdvStatus = '...';

  private subs: Subscription = new Subscription();

  navGroups: NavGroup[] = [
    {
      title: 'Suivi médical', key: 'medical', collapsed: false,
      icon: 'M4.8 2.62L2 5v14a2 2 0 002 2h16a2 2 0 002-2V5l-2.8-2.38A2 2 0 0017.92 2H6.08a2 2 0 00-1.28.62z',
      items: [
        { label: 'Tableau de bord', icon: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10', route: '/patient/dashboard' },
        { label: 'Dossier médical', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2 M9 5a2 2 0 002 2h2a2 2 0 002-2 M9 5a2 2 0 012-2h2a2 2 0 012 2 M9 14h6 M9 18h6 M9 10h6', route: '/patient/medical-record' },
        { label: 'Documents', icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8', route: '/patient/documents' },
        { label: 'Suivi', icon: 'M22 12h-4l-3 9L9 3l-3 9H2', route: '/patient/tracker' },
      ]
    },
    {
      title: 'Interaction', key: 'interaction', collapsed: false,
      icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2',
      items: [
        { label: 'Mes Médecins', icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2 M9 11a4 4 0 100-8 4 4 0 000 8z M23 21v-2a4 4 0 00-3-3.87 M16 3.13a4 4 0 010 7.75', route: '/patient/my-doctors' },
        { label: 'Rendez-vous', icon: 'M19 4H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V6a2 2 0 00-2-2z M16 2v4 M8 2v4 M3 10h18 M8 14h.01 M12 14h.01 M16 14h.01 M8 18h.01 M12 18h.01 M16 18h.01', route: '/patient/appointments' },
        { label: 'Messages', icon: 'M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z', route: '/patient/messages', badge: 2 },
      ]
    },
    {
      title: 'Ressources', key: 'resources', collapsed: false,
      icon: 'M2 3h6a4 4 0 014 4 4 4 0 014-4h6v18h-6a4 4 0 00-4 4 4 4 0 00-4-4H2z',
      items: [
        { label: 'Éducation', icon: 'M2 3h6a4 4 0 014 4 4 4 0 014-4h6v18h-6a4 4 0 00-4 4 4 4 0 00-4-4H2z', route: '/patient/education' },
        { label: 'Notifications', icon: 'M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 01-3.46 0', route: '/patient/notifications', badge: 3 },
        { label: 'Facturation', icon: 'M21 4H3a2 2 0 00-2 2v12a2 2 0 002 2h18a2 2 0 002-2V6a2 2 0 00-2-2z M1 10h22', route: '/patient/billing' },
      ]
    },
  ];

  constructor(
    private authService: AuthService, 
    private profilePhotoService: ProfilePhotoService,
    private appointmentService: PatientAppointmentService
  ) { }

  ngOnInit(): void {
    this.fetchNextAppointment();
  }

  fetchNextAppointment(): void {
    const patientId = (this.authService.currentUser as any)?.patientProfileId || (this.authService.currentUser as any)?.id;
    if (!patientId) return;

    this.subs.add(
      this.appointmentService.getNextAppointment(patientId).subscribe({
        next: (resp) => {
          if (resp && resp.nextAppointment) {
            const next = resp.nextAppointment;
            const start = new Date(next.date);
            const day = start.getDate().toString().padStart(2, '0');
            const monthNames = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];
            const month = monthNames[start.getMonth()];
            
            const doctorName = next.doctor ? `Dr. ${next.doctor.lastName}` : 'Médecin';
            this.nextRdvDate = `${day} ${month} — ${doctorName}`;
            
            // Sub message: dynamic "Dans X jours" or reason
            const diffDays = Math.ceil((start.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
            if (diffDays === 0) {
              this.nextRdvSub = "Aujourd'hui";
            } else if (diffDays === 1) {
              this.nextRdvSub = "Demain";
            } else if (diffDays > 1) {
              this.nextRdvSub = `Dans ${diffDays} jours`;
            } else {
              this.nextRdvSub = next.type || 'Consultation';
            }

            this.nextRdvStatus = this.getStatusLabel(next.status);
          } else {
            this.nextRdvDate = 'Aucun RDV à venir';
            this.nextRdvSub = 'Prenez rendez-vous';
            this.nextRdvStatus = 'Agenda vide';
          }
        },
        error: (err) => {
          console.error('[Sidebar] Error fetching next appointment:', err);
          this.nextRdvDate = 'Erreur agenda';
          this.nextRdvSub = 'Réessayez plus tard';
          this.nextRdvStatus = 'Erreur';
        }
      })
    );
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return 'Inconnu';
    const labels: Record<string, string> = {
      'REQUESTED': 'À confirmer',
      'CONFIRMED': 'Confirmé',
      'UPCOMING': 'À venir',
      'CANCELLED': 'Annulé',
      'DONE': 'Terminé',
      'SCHEDULED': 'Confirmé'
    };
    return labels[status] || status;
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  get userName(): string {
    const u = this.authService.currentUser;
    if (u?.firstName && u?.lastName) return `${u.firstName} ${u.lastName}`;
    if (u?.firstName) return u.firstName;
    return 'Mon espace';
  }

  get userInitials(): string {
    const u = this.authService.currentUser;
    if (!u) return 'P';
    const f = u.firstName?.charAt(0)?.toUpperCase() ?? '';
    const l = u.lastName?.charAt(0)?.toUpperCase() ?? '';
    return (f + l) || 'P';
  }

  get userEmail(): string {
    return this.authService.currentUser?.email ?? 'patient@breastiq.com';
  }

  get profilePhotoUrl(): string {
    const u = this.authService.currentUser;
    const photoUrl = (u as any)?.profilePhotoUrl;
    if (!photoUrl) return '';
    return this.profilePhotoService.getPhotoUrl(photoUrl);
  }

  toggleMobile(): void { this.mobileOpen = !this.mobileOpen; }
  closeMobile(): void { this.mobileOpen = false; }
  toggleCollapse(): void { this.collapsed = !this.collapsed; }
  toggleGroup(group: NavGroup): void { if (!this.collapsed) group.collapsed = !group.collapsed; }

  logout(): void {
    this.authService.logout();
    window.location.href = '/';
  }

  parsePaths(d: string): string[] {
    return d.split(/(?= M)/g).map(s => s.trim()).filter(Boolean);
  }
}

