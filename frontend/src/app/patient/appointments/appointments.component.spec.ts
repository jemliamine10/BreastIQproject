import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { AppointmentStatus, AppointmentType, TimelineStatus } from '../../models/appointment.model';
import { PatientAppointmentService } from '../../services/patient-appointment.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { LinkService } from '../../services/link.service';
import { DoctorCalendarService } from '../../services/doctor-calendar.service';

import { AppointmentsComponent } from './appointments.component';

describe('AppointmentsComponent', () => {
  let component: AppointmentsComponent;
  let fixture: ComponentFixture<AppointmentsComponent>;

  const appointmentServiceMock = {
    getAppointments: jasmine.createSpy('getAppointments').and.returnValue(of({
      content: [
        {
          id: 'a1',
          patientId: 'p1',
          doctorId: 'd1',
          type: AppointmentType.CONSULTATION,
          title: 'Consultation de suivi',
          description: 'Suivi oncologique',
          date: new Date().toISOString(),
          status: AppointmentStatus.SCHEDULED,
          location: 'Paris',
          notes: [],
          doctor: {
            id: 'd1',
            firstName: 'Alice',
            lastName: 'Martin',
            specialty: 'Oncologue',
            contact: '01020304',
            structure: 'Centre A'
          }
        }
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0
    })),
    getStats: jasmine.createSpy('getStats').and.returnValue(of({
      totalAppointments: 1,
      totalDoctors: 2,
      totalExams: 1,
      progressPercentage: 33
    })),
    getTimeline: jasmine.createSpy('getTimeline').and.returnValue(of([
      {
        id: 't1',
        date: new Date().toISOString(),
        type: AppointmentType.CONSULTATION,
        label: 'Diagnostic',
        status: TimelineStatus.COMPLETED
      }
    ])),
    getNextAppointment: jasmine.createSpy('getNextAppointment').and.returnValue(of({
      id: 'a1',
      patientId: 'p1',
      doctorId: 'd1',
      type: AppointmentType.CONSULTATION,
      title: 'Consultation de suivi',
      description: 'Suivi oncologique',
      date: new Date().toISOString(),
      status: AppointmentStatus.SCHEDULED,
      location: 'Paris',
      notes: [],
      doctor: {
        id: 'd1',
        firstName: 'Alice',
        lastName: 'Martin',
        specialty: 'Oncologue',
        contact: '01020304',
        structure: 'Centre A'
      }
    })),
    getAppointmentDetails: jasmine.createSpy('getAppointmentDetails').and.returnValue(of({ id: 'a1', status: AppointmentStatus.SCHEDULED })),
    updateAppointment: jasmine.createSpy('updateAppointment').and.returnValue(of({})),
    cancelAppointment: jasmine.createSpy('cancelAppointment').and.returnValue(of(void 0)),
    createAppointment: jasmine.createSpy('createAppointment').and.returnValue(of({}))
  };

  const userServiceMock = {
    getPatientByUserId: jasmine.createSpy('getPatientByUserId').and.returnValue(of({
      patientProfileId: 'p1'
    }))
  };

  const linkServiceMock = {
    getConnected: jasmine.createSpy('getConnected').and.returnValue(of([
      {
        id: '123e4567-e89b-12d3-a456-426614174001',
        patientProfileId: 'p1',
        doctorProfileId: '123e4567-e89b-12d3-a456-426614174002',
        status: 'ACTIVE',
        requestedBy: 'PATIENT'
      }
    ]))
  };

  const doctorCalendarServiceMock = {
    getCalendarSlots: jasmine.createSpy('getCalendarSlots').and.returnValue(of([]))
  };

  const authServiceMock = {
    currentUser: {
      id: '123e4567-e89b-12d3-a456-426614174000'
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppointmentsComponent],
      providers: [
        {
          provide: PatientAppointmentService,
          useValue: appointmentServiceMock
        },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(convertToParamMap({}))
          }
        },
        {
          provide: AuthService,
          useValue: authServiceMock
        },
        {
          provide: UserService,
          useValue: userServiceMock
        },
        {
          provide: LinkService,
          useValue: linkServiceMock
        },
        {
          provide: DoctorCalendarService,
          useValue: doctorCalendarServiceMock
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AppointmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load appointments on init', () => {
    expect(appointmentServiceMock.getAppointments).toHaveBeenCalled();
    expect(appointmentServiceMock.getStats).toHaveBeenCalled();
    expect(appointmentServiceMock.getTimeline).toHaveBeenCalled();
    expect(component.appointments.length).toBe(1);
  });
});
