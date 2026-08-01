import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { LinkResponseDto } from '../../models/links-appointments.dto';
import { PatientFullResponseDto } from '../../models/patient-full-response.dto';

interface PatientDemandView {
  link: LinkResponseDto;
  patient?: PatientFullResponseDto;
}

@Component({
  selector: 'app-links',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './links.component.html',
  styleUrl: './links.component.css'
})
export class LinksComponent implements OnInit {
  activeFilter = 'all';
  searchQuery = '';
  selectedDemand: PatientDemandView | null = null;
  demands: PatientDemandView[] = [];
  loading = false;
  actionLoading = false;
  error: string | null = null;
  doctorProfileId: string | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly linkService: LinkService
  ) {}

  ngOnInit(): void {
    this.loadDemands();
  }

  loadDemands(): void {
    const currentUserId = this.authService.currentUser?.id;
    if (!currentUserId) {
      this.error = 'Utilisateur non connecté.';
      return;
    }

    this.loading = true;
    this.error = null;

    this.userService.getDoctorByUserId(currentUserId).subscribe({
      next: (doctor) => {
        this.doctorProfileId = doctor.doctorProfileId;

        forkJoin({
          pendingLinks: this.linkService.getPending('doctor', this.doctorProfileId),
          connectedLinks: this.linkService.getConnected('doctor', this.doctorProfileId),
          patients: this.userService.getAllPatients()
        }).pipe(
          finalize(() => {
            this.loading = false;
          })
        ).subscribe({
          next: ({ pendingLinks, connectedLinks, patients }) => {
            const patientsByProfile = new Map<string, PatientFullResponseDto>();
            for (const patient of patients) {
              patientsByProfile.set(patient.patientProfileId, patient);
            }

            this.demands = pendingLinks
              .map((link) => ({
                link,
                patient: patientsByProfile.get(link.patientProfileId)
              }))
              .sort((a, b) => {
                const left = new Date(a.link.lastUpdatedAt ?? a.link.requestedAt ?? 0).getTime();
                const right = new Date(b.link.lastUpdatedAt ?? b.link.requestedAt ?? 0).getTime();
                return right - left;
              });

            // Auto-select first pending demand if available
            this.selectedDemand = this.demands[0] ?? null;
          },
          error: (err) => {
            this.error = this.mapError(err);
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.error = this.mapError(err);
      }
    });
  }

  selectDemand(demand: PatientDemandView): void {
    this.selectedDemand = demand;
  }

  setFilter(filter: string): void {
    this.activeFilter = filter;
  }

  get filteredDemands(): PatientDemandView[] {
    return this.demands.filter((demand) => {
      const status = demand.link.status.toLowerCase();
      const matchFilter = this.activeFilter === 'all' || status === this.activeFilter;
      if (!matchFilter) {
        return false;
      }

      if (!this.searchQuery.trim()) {
        return true;
      }

      const query = this.searchQuery.trim().toLowerCase();
      const fullName = `${demand.patient?.firstName ?? ''} ${demand.patient?.lastName ?? ''}`.toLowerCase();
      const email = (demand.patient?.email ?? '').toLowerCase();
      const city = (demand.patient?.city ?? '').toLowerCase();
      return fullName.includes(query) || email.includes(query) || city.includes(query);
    });
  }

  get newCount(): number {
    return this.demands.filter((d) => d.link.status === 'REQUESTED').length;
  }

  approveSelected(): void {
    if (!this.selectedDemand) {
      return;
    }
    if (this.selectedDemand.link.status !== 'REQUESTED') {
      return;
    }

    this.actionLoading = true;
    this.linkService.approve({
      linkId: this.selectedDemand.link.id,
      decisionByUserId: this.authService.currentUser?.id
    }).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: () => this.loadDemands(),
      error: (err) => {
        this.error = this.mapError(err);
      }
    });
  }

  refuseSelected(): void {
    if (!this.selectedDemand) {
      return;
    }
    if (this.selectedDemand.link.status !== 'REQUESTED') {
      return;
    }

    this.actionLoading = true;
    this.linkService.refuse({
      linkId: this.selectedDemand.link.id,
      decisionByUserId: this.authService.currentUser?.id,
      rejectionReason: 'Refusée depuis l’interface médecin.'
    }).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: () => this.loadDemands(),
      error: (err) => {
        this.error = this.mapError(err);
      }
    });
  }

  getStatusLabel(status: string): string {
    if (status === 'REQUESTED') return 'En attente';
    if (status === 'ACTIVE') return 'Active';
    if (status === 'REJECTED') return 'Refusée';
    return status;
  }

  getPatientInitials(demand: PatientDemandView): string {
    const first = demand.patient?.firstName?.charAt(0) ?? 'P';
    const last = demand.patient?.lastName?.charAt(0) ?? '';
    return `${first}${last}`.toUpperCase();
  }

  private mapError(err: any): string {
    if (err?.error?.message) return err.error.message;
    if (err?.status === 0) return 'Réseau indisponible.';
    if (err?.status >= 500) return 'Erreur serveur.';
    return 'Une erreur inattendue est survenue.';
  }
}
