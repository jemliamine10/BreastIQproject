export interface Availability {
  id: string;
  doctorId: string;
  dayOfWeek: number; // 1 for Monday, 2 for Tuesday, etc.
  startHour: string; // Format HH:mm
  endHour: string; // Format HH:mm
  slotDuration: number; // in minutes
  isActive: boolean;
}

export interface CreateAvailabilityDto extends Omit<Availability, 'id' | 'doctorId'> {}

export interface UpdateAvailabilityDto extends Partial<Omit<Availability, 'id' | 'doctorId'>> {}
