import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AppointmentService } from './appointment.service';

describe('AppointmentService', () => {
  let service: AppointmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AppointmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('calls doctor appointments endpoint via REST client', () => {
    const doctorId = '11111111-1111-1111-1111-111111111111';

    service.getDoctorAppointments(doctorId).subscribe();

    const req = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/doctors/${doctorId}/appointments`
    );

    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('passes status and date filters to doctor appointments endpoint', () => {
    const doctorId = '11111111-1111-1111-1111-111111111111';

    service.getDoctorAppointments(doctorId, {
      status: 'SCHEDULED',
      date: '2026-03-20'
    }).subscribe();

    const req = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/doctors/${doctorId}/appointments`
    );

    expect(req.request.params.get('status')).toBe('SCHEDULED');
    expect(req.request.params.get('date')).toBe('2026-03-20');
    req.flush([]);
  });

  it('calls doctor exceptions endpoint via REST client', () => {
    const doctorId = '11111111-1111-1111-1111-111111111111';

    service.getDoctorExceptions(doctorId).subscribe();

    const req = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/doctors/${doctorId}/exceptions`
    );

    req.flush([]);
  });

  it('falls back to /api/appointments/doctor/{doctorId} when nested doctor appointments endpoint is missing', () => {
    const doctorId = '11111111-1111-1111-1111-111111111111';

    service.getDoctorAppointments(doctorId).subscribe();

    const firstReq = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/doctors/${doctorId}/appointments`
    );
    firstReq.flush({ message: 'Not Found' }, { status: 404, statusText: 'Not Found' });

    const fallbackReq = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/appointments/doctor/${doctorId}`
    );
    fallbackReq.flush([]);
  });

  it('falls back to /api/exceptions/doctor/{doctorId} when nested doctor exceptions endpoint is missing', () => {
    const doctorId = '11111111-1111-1111-1111-111111111111';

    service.getDoctorExceptions(doctorId).subscribe();

    const firstReq = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/doctors/${doctorId}/exceptions`
    );
    firstReq.flush({ message: 'Not Found' }, { status: 404, statusText: 'Not Found' });

    const fallbackReq = httpMock.expectOne((request) =>
      request.method === 'GET'
      && request.url === `/api/exceptions/doctor/${doctorId}`
    );
    fallbackReq.flush([]);
  });
});
