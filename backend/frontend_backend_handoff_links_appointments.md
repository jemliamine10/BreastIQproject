# Handoff Frontend — Mise à jour Backend (Links + Appointments)

## 1) État général
Le backend expose maintenant des routes front-compatibles pour:
- Connexions patient-médecin (links)
- Disponibilité/création/liste rendez-vous (appointments)

Les routes legacy sont conservées pour rétrocompatibilité.

---

## 2) API Links (nouvelles routes front)
Base: `/api/links`

### 2.1 Créer une demande de connexion
- `POST /create-request`
- Body:
```json
{
  "patientId": "UUID",
  "doctorId": "UUID",
  "requestedBy": "PATIENT",
  "requestNote": "texte optionnel"
}
```
- Compatibilité: les champs historiques `patientProfileId` et `doctorProfileId` sont toujours acceptés.

### 2.2 Lister les demandes en attente
- `GET /pending?actorType=patient|doctor&actorId=UUID`
- Retour: `LinkResponseDto[]` (status = REQUESTED)

### 2.3 Lister les connexions actives
- `GET /connected?actorType=patient|doctor&actorId=UUID`
- Retour: `LinkResponseDto[]` (status = ACTIVE)

### 2.4 Approuver une demande
- `POST /approve`
- Body:
```json
{
  "linkId": "UUID",
  "decisionByUserId": "UUID optionnel"
}
```

### 2.5 Refuser une demande
- `POST /refuse`
- Body:
```json
{
  "linkId": "UUID",
  "decisionByUserId": "UUID optionnel",
  "rejectionReason": "texte optionnel"
}
```

---

## 3) API Appointments (nouvelles routes front)
Base: `/api/appointments`

### 3.1 Vérifier disponibilité d’un créneau
- `GET /available?doctorId=UUID&date=YYYY-MM-DD&heure=HH:mm[:ss]&durationMinutes=30&typeRDV=CONSULTATION`
- Retour:
```json
{
  "available": true,
  "message": "Créneau disponible"
}
```
ou
```json
{
  "available": false,
  "message": "Créneau indisponible"
}
```

### 3.2 Créer un rendez-vous (contrat front)
- `POST /create`
- Body:
```json
{
  "patientId": "UUID",
  "doctorId": "UUID",
  "date": "2026-03-16",
  "heure": "14:00",
  "typeRDV": "CONSULTATION",
  "title": "Consultation",
  "description": "Suivi",
  "location": "Clinique A",
  "durationMinutes": 30
}
```
- Retour: `PatientAppointmentDto`

### 3.3 Liste rendez-vous patient
- `GET /patient?patientId=UUID&date=YYYY-MM-DD&typeRDV=CONSULTATION&status=UPCOMING&page=0&size=20`
- Retour: `Page<PatientAppointmentDto>`

### 3.4 Liste rendez-vous médecin (calendrier)
- `GET /doctor?doctorId=UUID&from=2026-03-16T00:00:00Z&to=2026-03-17T00:00:00Z`
- Retour: `AppointmentResponseDto[]`

---

## 4) Contrats de données ajoutés/impactés

### 4.1 DTOs backend ajoutés
- `LinkActionRequestDto`
- `AppointmentAvailabilityResponseDto`
- `AppointmentCreateFrontRequestDto`

### 4.2 DTO backend modifié
- `LinkRequestCreateDto`
  - Ajout alias frontend: `patientId`, `doctorId`
  - Champs historiques toujours présents: `patientProfileId`, `doctorProfileId`

---

## 5) Énumérations à utiliser côté frontend
- `requestedBy`: `PATIENT | DOCTOR`
- `typeRDV`: `CONSULTATION | EXAM | TREATMENT | FOLLOW_UP | OTHER`
- `status` link: `REQUESTED | ACTIVE | REJECTED | BLOCKED | ENDED`
- `status` appointment: `UPCOMING | REQUESTED | CONFIRMED | CANCELLED | COMPLETED | NO_SHOW`

---

## 6) DTOs TypeScript à créer côté frontend

Créer un fichier `src/app/models/links-appointments.dto.ts` avec:

```ts
export type RequestedBy = 'PATIENT' | 'DOCTOR';
export type LinkStatus = 'REQUESTED' | 'ACTIVE' | 'REJECTED' | 'BLOCKED' | 'ENDED';
export type AppointmentTypeRDV = 'CONSULTATION' | 'EXAM' | 'TREATMENT' | 'FOLLOW_UP' | 'OTHER';
export type AppointmentStatus = 'UPCOMING' | 'REQUESTED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export interface LinkRequestCreateDto {
  patientId?: string;
  doctorId?: string;
  patientProfileId?: string;
  doctorProfileId?: string;
  requestedBy: RequestedBy;
  requestNote?: string;
}

export interface LinkActionRequestDto {
  linkId: string;
  decisionByUserId?: string;
  rejectionReason?: string;
}

export interface LinkResponseDto {
  id: string;
  patientProfileId: string;
  doctorProfileId: string;
  status: LinkStatus;
  requestedBy: RequestedBy;
  requestNote?: string;
  decisionByUserId?: string;
  rejectionReason?: string;
  requestedAt?: string;
  activatedAt?: string;
  endedAt?: string;
  lastUpdatedAt?: string;
}

export interface AppointmentAvailabilityResponseDto {
  available: boolean;
  message: string;
}

export interface AppointmentCreateFrontRequestDto {
  patientId: string;
  doctorId: string;
  date: string;
  heure: string;
  typeRDV: AppointmentTypeRDV;
  title?: string;
  description?: string;
  location?: string;
  durationMinutes?: number;
}

export interface AppointmentResponseDto {
  id: string;
  linkId?: string;
  patientProfileId?: string;
  doctorProfileId?: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  reason?: string;
  patientNotes?: string;
  doctorNotes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AppointmentDoctorDto {
  id: string;
  firstName?: string;
  lastName?: string;
  specialty?: string;
  contact?: string;
  structure?: string;
}

export interface PatientAppointmentDto {
  id: string;
  type: AppointmentTypeRDV;
  title?: string;
  description?: string;
  date: string;
  endDate: string;
  status: AppointmentStatus;
  location?: string;
  doctor?: AppointmentDoctorDto;
  notes: string[];
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

---

## 7) Logique frontend à respecter pour alignement backend
- Toujours envoyer des **Profile IDs** (`patientId`, `doctorId`) pour les endpoints front-compat.
- Pour `create-request`, privilégier `patientId/doctorId`; garder `patientProfileId/doctorProfileId` seulement en fallback legacy.
- Pour disponibilité et création RDV, envoyer `date` (`YYYY-MM-DD`) + `heure` (`HH:mm` ou `HH:mm:ss`) séparés.
- Avant `POST /api/appointments/create`, appeler `GET /api/appointments/available`.
- Si `available = false`, bloquer la soumission et afficher `message` backend.
- Après action `links` (`create-request`, `approve`, `refuse`) ou `appointments` (`create`), relancer immédiatement les listes concernées (`pending`, `connected`, `patient`, `doctor`).
- Pour `GET /api/appointments/doctor`, toujours fournir `from` et `to` (ISO datetime UTC).
- Les enums doivent être envoyés en **MAJUSCULES exactes** (ex: `CONSULTATION`, `REQUESTED`).
- Gérer les erreurs API via `status/message` (400/404/409), et afficher le `message` serveur dans les notifications UI.

---

## 8) Notes d’intégration front
- IDs contractuels: utiliser des IDs de profil (`PatientProfile.id`, `DoctorProfile.id`).
- Pour la création/availability RDV: envoyer `date` + `heure` séparés.
- Après action (`create-request`, `approve`, `refuse`, `create`), rafraîchir les listes concernées côté UI.
- Les routes legacy sont conservées; migrer progressivement vers ces nouvelles routes front.

---

## 9) Fichiers backend modifiés
- `src/main/java/com/breastcancer/breastcancerbackend/controller/LinkController.java`
- `src/main/java/com/breastcancer/breastcancerbackend/service/LinkService.java`
- `src/main/java/com/breastcancer/breastcancerbackend/dto/LinkRequestCreateDto.java`
- `src/main/java/com/breastcancer/breastcancerbackend/dto/LinkActionRequestDto.java`
- `src/main/java/com/breastcancer/breastcancerbackend/controller/AppointmentController.java`
- `src/main/java/com/breastcancer/breastcancerbackend/service/AppointmentService.java`
- `src/main/java/com/breastcancer/breastcancerbackend/dto/AppointmentAvailabilityResponseDto.java`
- `src/main/java/com/breastcancer/breastcancerbackend/dto/AppointmentCreateFrontRequestDto.java`
