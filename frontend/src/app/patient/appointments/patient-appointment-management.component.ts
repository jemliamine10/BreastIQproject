import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AppointmentStatus, PatientAppointment } from '../../models/appointment.model';

@Component({
  selector: 'app-patient-appointment-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patient-appointment-management.component.html',
  styleUrl: './patient-appointment-management.component.css'
})
export class PatientAppointmentManagementComponent {
  @Input() appointments: PatientAppointment[] = [];
  @Input() loading = false;

  @Output() openDetails = new EventEmitter<PatientAppointment>();
  @Output() cancel = new EventEmitter<PatientAppointment>();

  readonly appointmentStatus = AppointmentStatus;

  trackById(_: number, appointment: PatientAppointment): string {
    return appointment.id;
  }

  canCancel(status: AppointmentStatus): boolean {
    return status === AppointmentStatus.SCHEDULED;
  }

  getStatusLabel(status: AppointmentStatus): string {
    switch (status) {
      case AppointmentStatus.SCHEDULED:
        return 'Planifie';
      case AppointmentStatus.CANCELLED:
        return 'Annule';
      case AppointmentStatus.DONE:
        return 'Termine';
      case AppointmentStatus.RESCHEDULED:
        return 'Reprogramme';
      default:
        return status;
    }
  }

  getStatusClass(status: AppointmentStatus): string {
    switch (status) {
      case AppointmentStatus.SCHEDULED:
        return 'status-scheduled';
      case AppointmentStatus.CANCELLED:
        return 'status-cancelled';
      case AppointmentStatus.DONE:
        return 'status-done';
      case AppointmentStatus.RESCHEDULED:
        return 'status-rescheduled';
      default:
        return '';
    }
  }

  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
