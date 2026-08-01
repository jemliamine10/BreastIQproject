import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PatientAppointmentService } from './patient-appointment.service';
import { AppointmentMode } from '../models/appointment.model';

describe('PatientAppointmentService', () => {
  let service: PatientAppointmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(PatientAppointmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call getAppointments with query params', () => {
    service.getAppointments({
      patientId: '123e4567-e89b-12d3-a456-426614174000',
      page: 1,
      size: 10
    }).subscribe();

    const request = httpMock.expectOne((req) =>
      req.url === '/api/patient/appointments'
      && req.params.get('patientId') === '123e4567-e89b-12d3-a456-426614174000'
      && req.params.get('page') === '1'
      && req.params.get('size') === '10'
    );

    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 1 });
  });

  it('should call createAppointment', () => {
    service.createAppointment({
      linkId: '123e4567-e89b-12d3-a456-426614174000',
      startAt: new Date().toISOString(),
      endAt: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
      mode: AppointmentMode.VIDEO
    }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/appointments');
    expect(request.request.method).toBe('POST');
    request.flush({ id: 'a1' });
  });

  it('should call cancelAppointment with DELETE endpoint', () => {
    service.cancelAppointment('a1').subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/appointments/a1');
    expect(request.request.method).toBe('DELETE');
    request.flush({});
  });
});
