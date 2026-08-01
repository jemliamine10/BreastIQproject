// Liste tous les couples status + type autorisés pour les rendez-vous
// À adapter selon la logique métier (exemple ci-dessous)

import { AppointmentStatusRDV, AppointmentTypeRDV } from '../models/links-appointments.dto';

// Exemple de mapping autorisé (à compléter selon la contrainte appointments_status_check)
const allowedStatusTypePairs: Array<{ status: AppointmentStatusRDV, type: AppointmentTypeRDV }> = [
  { status: 'SCHEDULED', type: 'CONSULTATION' },
  { status: 'SCHEDULED', type: 'EXAM' },
  { status: 'SCHEDULED', type: 'TREATMENT' },
  { status: 'SCHEDULED', type: 'FOLLOW_UP' },
  { status: 'SCHEDULED', type: 'OTHER' },
  { status: 'CANCELLED', type: 'CONSULTATION' },
  { status: 'CANCELLED', type: 'EXAM' },
  { status: 'CANCELLED', type: 'TREATMENT' },
  { status: 'CANCELLED', type: 'FOLLOW_UP' },
  { status: 'CANCELLED', type: 'OTHER' },
  { status: 'DONE', type: 'CONSULTATION' },
  { status: 'DONE', type: 'EXAM' },
  { status: 'DONE', type: 'TREATMENT' },
  { status: 'DONE', type: 'FOLLOW_UP' },
  { status: 'DONE', type: 'OTHER' },
  { status: 'RESCHEDULED', type: 'CONSULTATION' },
  { status: 'RESCHEDULED', type: 'EXAM' },
  { status: 'RESCHEDULED', type: 'TREATMENT' },
  { status: 'RESCHEDULED', type: 'FOLLOW_UP' },
  { status: 'RESCHEDULED', type: 'OTHER' },
];

export function isStatusTypeAllowed(status: AppointmentStatusRDV, type: AppointmentTypeRDV): boolean {
  return allowedStatusTypePairs.some(pair => pair.status === status && pair.type === type);
}

// Utilisation exemple :
// if (!isStatusTypeAllowed(status, type)) {
//   console.warn('Couple status/type non autorisé:', status, type);
// }
