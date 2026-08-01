import { Routes } from '@angular/router';
import { LandingComponent } from './public/landing/landing.component';
import { LoginComponent } from './public/login/login.component';
import { RegisterDoctorComponent } from './public/register-doctor/register-doctor.component';
import { RegisterPatientComponent } from './public/register-patient/register-patient.component';
import { RegisterSelectionComponent } from './public/register-selection/register-selection.component';

/* ── Doctor Components ── */
import { DoctorLayoutComponent } from './doctor/doctor-layout/doctor-layout.component';
import { DashboardComponent as DoctorDashboardComponent } from './doctor/dashboard/dashboard.component';

import { PatientsComponent as DoctorPatientsComponent } from './doctor/patients/patients.component';
import { TreatmentComponent as DoctorTreatmentComponent } from './doctor/treatment/treatment.component';
import { ImagingComponent as DoctorImagingComponent } from './doctor/imaging/imaging.component';
import { TrackerComponent as DoctorTrackerComponent } from './doctor/tracker/tracker.component';
import { MessagesComponent as DoctorMessagesComponent } from './doctor/messages/messages.component';
import { DocumentsComponent as DoctorDocumentsComponent } from './doctor/documents/documents.component';
import { LinksComponent as DoctorLinksComponent } from './doctor/links/links.component';
import { ProfileComponent as DoctorProfileComponent } from './doctor/profile/profile.component';
import { CalendrierDocteurComponent } from './doctor/calendrierdocteur/calendrierdocteur.component';

/* ── Patient Components ── */
import { PatientLayoutComponent } from './patient/patient-layout/patient-layout.component';
import { DashboardComponent as PatientDashboardComponent } from './patient/dashboard/dashboard.component';
import { ProfileComponent } from './patient/profile/profile.component';
import { AllergiesComponent } from './patient/allergies/allergies.component';
import { TreatmentsComponent } from './patient/treatments/treatments.component';
import { MyDoctorsComponent } from './patient/my-doctors/my-doctors.component';
import { AppointmentsComponent } from './patient/appointments/appointments.component';
import { MedicalRecordComponent } from './patient/medical-record/medical-record.component';
import { DocumentsComponent } from './patient/documents/documents.component';
import { ImagingComponent } from './patient/imaging/imaging.component';
import { TrackerComponent } from './patient/tracker/tracker.component';
import { MessagesComponent } from './patient/messages/messages.component';
import { EducationComponent } from './patient/education/education.component';
import { NotificationsComponent } from './patient/notifications/notifications.component';
import { BillingComponent } from './patient/billing/billing.component';

import { publicGuard, patientGuard, doctorGuard } from './guards/auth.guard';

export const routes: Routes = [
    /* ── Public ── */
    { path: '', component: LandingComponent, canActivate: [publicGuard] },
    { path: 'public/login', component: LoginComponent, canActivate: [publicGuard] },
    { path: 'public/register', component: RegisterSelectionComponent, canActivate: [publicGuard] },
    { path: 'public/register-doctor', component: RegisterDoctorComponent, canActivate: [publicGuard] },
    { path: 'public/register-patient', component: RegisterPatientComponent, canActivate: [publicGuard] },

    /* ── Doctor (avec sidebar layout) ── */
    { path: 'doctor', component: DoctorLayoutComponent, canActivate: [doctorGuard], children: [
        { path: 'dashboard',    component: DoctorDashboardComponent },
        { path: 'patients',     component: DoctorPatientsComponent },
       
        { path: 'treatment',    component: DoctorTreatmentComponent },
        { path: 'imaging',      component: DoctorImagingComponent },
        { path: 'tracker',      component: DoctorTrackerComponent },
        { path: 'messages',     component: DoctorMessagesComponent },
        { path: 'documents',    component: DoctorDocumentsComponent },
        { path: 'links',        component: DoctorLinksComponent },
        { path: 'profile',      component: DoctorProfileComponent },
        { path: 'schedule',     component: CalendrierDocteurComponent },
        { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]},

    /* ── Patient (avec sidebar layout) ── */
    { path: 'patient', component: PatientLayoutComponent, canActivate: [patientGuard], children: [
        { path: 'dashboard',      component: PatientDashboardComponent },
        { path: 'profile',        component: ProfileComponent },
        { path: 'allergies',      component: AllergiesComponent },
        { path: 'treatments',     component: TreatmentsComponent },
        { path: 'my-doctors',     component: MyDoctorsComponent },
        { path: 'appointments',   component: AppointmentsComponent },
        { path: 'medical-record', component: MedicalRecordComponent },
        { path: 'documents',      component: DocumentsComponent },
        { path: 'imaging',        component: ImagingComponent },
        { path: 'tracker',        component: TrackerComponent },
        { path: 'messages',       component: MessagesComponent },
        { path: 'education',      component: EducationComponent },
        { path: 'notifications',  component: NotificationsComponent },
        { path: 'billing',        component: BillingComponent },
        { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]},

    /* ── Fallback ── */
    { path: '**', redirectTo: '' }
];
