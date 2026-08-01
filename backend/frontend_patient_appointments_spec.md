# Spécification Frontend Angular — Module Rendez-vous Patient

## 1. Models TypeScript (models/appointment.model.ts)

### 1.1. Enumérations

```typescript
export enum AppointmentType {
  CONSULTATION = 'CONSULTATION',
  EXAM = 'EXAM',
  TREATMENT = 'TREATMENT',
  FOLLOW_UP = 'FOLLOW_UP',
  OTHER = 'OTHER'
}

export enum AppointmentStatus {
  UPCOMING = 'UPCOMING',
  REQUESTED = 'REQUESTED',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED',
  NO_SHOW = 'NO_SHOW'
}

export enum TimelineStatus {
  COMPLETED = 'COMPLETED',
  ACTIVE = 'ACTIVE',
  UPCOMING = 'UPCOMING'
}
```

### 1.2. Interfaces

```typescript
export interface AppointmentDoctor {
  id: string;
  firstName: string;
  lastName: string;
  specialty: string;
  contact: string;
  structure: string;
}

export interface PatientAppointment {
  id: string;
  type: AppointmentType;
  title: string;
  description: string;
  date: string; // ISO 8601
  endDate: string; // ISO 8601
  status: AppointmentStatus;
  location: string;
  doctor: AppointmentDoctor;
  notes: string[];
}

export interface CreatePatientAppointment {
  patientId: string;
  doctorId: string;
  type: AppointmentType;
  title: string;
  description: string;
  date: string; // ISO 8601
  endDate?: string; // ISO 8601
  location?: string;
}

export interface UpdatePatientAppointment {
  patientId: string;
  title?: string;
  description?: string;
  date?: string; // ISO 8601
  endDate?: string; // ISO 8601
  location?: string;
  notes?: string[];
}

export interface AppointmentStats {
  totalAppointments: number;
  totalDoctors: number;
  totalExams: number;
  progressPercentage: number;
}

export interface TimelineEvent {
  date: string; // ISO 8601
  type: AppointmentType;
  label: string;
  description: string;
  status: TimelineStatus;
}
```

---

## 2. Services Angular (services/patient-appointment.service.ts)

### 2.1. Endpoints à implémenter

| Méthode | URL Backend                                 | Description                                 |
|---------|---------------------------------------------|---------------------------------------------|
| GET     | /api/patient/appointments                   | Liste paginée des rendez-vous patient       |
| GET     | /api/patient/appointments/{id}              | Détail d’un rendez-vous                     |
| POST    | /api/patient/appointments                   | Création d’un rendez-vous                   |
| PUT     | /api/patient/appointments/{id}              | Modification d’un rendez-vous               |
| DELETE  | /api/patient/appointments/{id}?patientId=   | Annulation logique d’un rendez-vous         |
| GET     | /api/patient/appointments/next?patientId=   | Prochain rendez-vous                        |
| GET     | /api/patient/appointments/stats?patientId=  | Statistiques patient                        |
| GET     | /api/patient/appointments/timeline?patientId= | Timeline du parcours patient              |

### 2.2. Service Angular (exemple)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PatientAppointment,
  CreatePatientAppointment,
  UpdatePatientAppointment,
  AppointmentStats,
  TimelineEvent,
  AppointmentType,
  AppointmentStatus
} from '../models/appointment.model';

@Injectable({ providedIn: 'root' })
export class PatientAppointmentService {
  private readonly baseUrl = '/api/patient/appointments';

  constructor(private http: HttpClient) {}

  getAppointments(
    patientId: string,
    filters?: { date?: string; type?: AppointmentType; status?: AppointmentStatus; doctorId?: string; page?: number; size?: number }
  ): Observable<{ content: PatientAppointment[]; totalElements: number; totalPages: number; number: number }> {
    let params = new HttpParams().set('patientId', patientId);
    if (filters?.date) params = params.set('date', filters.date);
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.doctorId) params = params.set('doctorId', filters.doctorId);
    params = params.set('page', String(filters?.page ?? 0));
    params = params.set('size', String(filters?.size ?? 20));
    return this.http.get<{ content: PatientAppointment[]; totalElements: number; totalPages: number; number: number }>(this.baseUrl, { params });
  }

  getAppointmentDetails(id: string, patientId: string): Observable<PatientAppointment> {
    return this.http.get<PatientAppointment>(`${this.baseUrl}/${id}`, { params: new HttpParams().set('patientId', patientId) });
  }

  createAppointment(data: CreatePatientAppointment): Observable<PatientAppointment> {
    return this.http.post<PatientAppointment>(this.baseUrl, data);
  }

  updateAppointment(id: string, data: UpdatePatientAppointment): Observable<PatientAppointment> {
    return this.http.put<PatientAppointment>(`${this.baseUrl}/${id}`, data);
  }

  cancelAppointment(id: string, patientId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { params: new HttpParams().set('patientId', patientId) });
  }

  getNextAppointment(patientId: string): Observable<PatientAppointment> {
    return this.http.get<PatientAppointment>(`${this.baseUrl}/next`, { params: new HttpParams().set('patientId', patientId) });
  }

  getStats(patientId: string): Observable<AppointmentStats> {
    return this.http.get<AppointmentStats>(`${this.baseUrl}/stats`, { params: new HttpParams().set('patientId', patientId) });
  }

  getTimeline(patientId: string): Observable<TimelineEvent[]> {
    return this.http.get<TimelineEvent[]>(`${this.baseUrl}/timeline`, { params: new HttpParams().set('patientId', patientId) });
  }
}
```

---

## 3. Structure JSON attendue

### 3.1. Liste paginée des rendez-vous

**GET /api/patient/appointments?patientId=...&page=0&size=20**

Réponse :

```json
{
  "content": [
    {
      "id": "uuid",
      "type": "CONSULTATION",
      "title": "Consultation Oncologie",
      "description": "Suivi trimestriel",
      "date": "2026-02-18T14:00:00Z",
      "endDate": "2026-02-18T14:30:00Z",
      "status": "CONFIRMED",
      "location": "Paris",
      "doctor": {
        "id": "uuid",
        "firstName": "Sophie",
        "lastName": "Martin",
        "specialty": "Oncologue",
        "contact": "+33123456789",
        "structure": "Centre L.B."
      },
      "notes": []
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0
}
```

### 3.2. Détail d’un rendez-vous

**GET /api/patient/appointments/{id}?patientId=...**

Réponse : identique à un objet `PatientAppointment`.

### 3.3. Création d’un rendez-vous

**POST /api/patient/appointments**

Corps :

```json
{
  "patientId": "uuid",
  "doctorId": "uuid",
  "type": "CONSULTATION",
  "title": "Consultation Oncologie",
  "description": "Suivi trimestriel",
  "date": "2026-02-18T14:00:00Z",
  "endDate": "2026-02-18T14:30:00Z",
  "location": "Paris"
}
```

Réponse : objet `PatientAppointment`.

### 3.4. Modification d’un rendez-vous

**PUT /api/patient/appointments/{id}**

Corps :

```json
{
  "patientId": "uuid",
  "title": "Consultation modifiée",
  "description": "Nouvelles instructions",
  "date": "2026-02-18T15:00:00Z",
  "endDate": "2026-02-18T15:30:00Z",
  "location": "Paris",
  "notes": ["Prévoir bilan sanguin"]
}
```

Réponse : objet `PatientAppointment`.

### 3.5. Annulation d’un rendez-vous

**DELETE /api/patient/appointments/{id}?patientId=...**

Réponse : HTTP 204 No Content.

### 3.6. Prochain rendez-vous

**GET /api/patient/appointments/next?patientId=...**

Réponse : objet `PatientAppointment`.

### 3.7. Statistiques

**GET /api/patient/appointments/stats?patientId=...**

Réponse :

```json
{
  "totalAppointments": 12,
  "totalDoctors": 3,
  "totalExams": 5,
  "progressPercentage": 80
}
```

### 3.8. Timeline

**GET /api/patient/appointments/timeline?patientId=...**

Réponse :

```json
[
  {
    "date": "2026-02-18T14:00:00Z",
    "type": "CONSULTATION",
    "label": "Consultation Dr Martin",
    "description": "Suivi trimestriel",
    "status": "ACTIVE"
  },
  {
    "date": "2026-03-03T09:00:00Z",
    "type": "TREATMENT",
    "label": "Chimiothérapie",
    "description": "Séance 1",
    "status": "UPCOMING"
  }
]
```

---

## 4. Bonnes pratiques Angular

- Organisation :  
  - Placer les models dans `src/app/models/appointment.model.ts`.
  - Placer le service dans `src/app/services/patient-appointment.service.ts`.
  - Utiliser des modules Angular dédiés pour le domaine patient (ex : `PatientModule`).
- Typage strict :  
  - Toujours utiliser les enums et interfaces définis ci-dessus.
  - Préférer `readonly` pour les champs immuables.
- RxJS :  
  - Utiliser `Observable` pour toutes les méthodes du service.
  - Gérer les erreurs HTTP avec `catchError`.
- Séparation des responsabilités :  
  - Les composants Angular ne doivent pas manipuler directement le JSON, mais utiliser les models.
  - Les services ne doivent pas contenir de logique de présentation.
- Validation :  
  - Valider les formulaires côté Angular avant envoi.
  - Gérer les erreurs de validation backend (400) et afficher les messages utilisateur.
- Date/heure :  
  - Utiliser `Date` ou `string` ISO pour les champs date/heure.
  - Gérer le fuseau horaire côté UI si besoin.

---

## 5. À retenir

- Respecter strictement les noms de champs et enums pour éviter toute erreur d’intégration.
- Toujours passer le `patientId` dans les requêtes (query param ou body selon endpoint).
- Les réponses sont typées et paginées pour la liste.
- Les statuts et types sont en MAJUSCULES (alignement Java/TypeScript).
- Les erreurs backend sont au format :

```json
{
  "timestamp": "2026-03-16T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "patientId requis."
}
```

---

Ce document est prêt à être utilisé par une équipe Angular pour garantir une intégration sans friction avec le backend.
