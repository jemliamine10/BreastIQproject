# Implémentation Frontend Angular dynamique — Module Rendez-vous Patient

## Contexte actuel (audit du code existant)

Le frontend actuel est bien en mode statique :

- Composant cible : `src/app/patient/appointments/appointments.component.ts`
- Template cible : `src/app/patient/appointments/appointments.component.html`
- Service actuel vide : `src/app/services/appointment.service.ts`
- `HttpClient` est déjà disponible globalement via `provideHttpClient()` dans `src/app/app.config.ts`

Objectif : **rendre le module dynamique sans modifier le design, le CSS ni la structure visuelle du HTML**.

---

## 1) Models TypeScript à créer

Créer un nouveau fichier :

`src/app/models/appointment.model.ts`

### 1.1 Enums

```ts
export enum AppointmentType {
  CONSULTATION = 'consultation',
  EXAM = 'exam',
  TREATMENT = 'treatment',
  CONTROL = 'control',
  OTHER = 'other'
}

export enum AppointmentStatus {
  UPCOMING = 'upcoming',
  CONFIRMED = 'confirmed',
  CANCELED = 'canceled',
  COMPLETED = 'completed'
}

export enum TimelineStatus {
  COMPLETED = 'completed',
  ACTIVE = 'active',
  UPCOMING = 'upcoming'
}
```

### 1.2 Interfaces

```ts
export interface AppointmentDoctor {
  id: string;
  firstName: string;
  lastName: string;
  specialty: string;
  structure: string;
}

export interface PatientAppointment {
  id: string;
  patientId: string;
  doctorId: string;
  type: AppointmentType;
  title: string;
  description: string;
  date: string;           // ISO 8601
  endDate?: string;       // ISO 8601
  status: AppointmentStatus;
  location: string;
  notes: string[];
  doctor?: AppointmentDoctor;
  createdAt?: string;     // ISO 8601
  updatedAt?: string;     // ISO 8601
}

export interface CreatePatientAppointment {
  doctorId: string;
  type: AppointmentType;
  title: string;
  description?: string;
  date: string;           // ISO 8601
  endDate?: string;       // ISO 8601
  location: string;
  notes?: string[];
}

export interface UpdatePatientAppointment {
  type?: AppointmentType;
  title?: string;
  description?: string;
  date?: string;
  endDate?: string;
  status?: AppointmentStatus;
  location?: string;
  notes?: string[];
}

export interface AppointmentStats {
  nextAppointmentDate?: string;  // ISO 8601
  specialistsCount: number;
  examsCount: number;
  treatmentStep: number;
  treatmentTotalSteps: number;
}

export interface TimelineEvent {
  id: string;
  date: string;               // ISO 8601
  type: AppointmentType;
  label: string;
  description?: string;
  status: TimelineStatus;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size?: number;
}
```

### 1.3 Points de cohérence backend/frontend

- Conserver strictement les valeurs d’enums backend (`consultation`, `exam`, etc.).
- Date uniquement en format ISO côté transport (`date`, `endDate`, `createdAt`, `updatedAt`).
- Formater la date **uniquement dans le composant** (pipe date ou helper TS) pour affichage FR.

---

## 2) Service Angular à créer / mettre à jour

Créer (ou remplacer le service vide) :

`src/app/services/patient-appointment.service.ts`

> Ne pas surcharger `appointment.service.ts` existant si ce service doit rester générique. Préférer un service patient dédié.

### 2.1 Imports

```ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AppointmentStats,
  CreatePatientAppointment,
  PaginatedResponse,
  PatientAppointment,
  TimelineEvent,
  UpdatePatientAppointment
} from '../models/appointment.model';
```

### 2.2 Service complet

```ts
@Injectable({ providedIn: 'root' })
export class PatientAppointmentService {
  private readonly baseUrl = '/api/patient/appointments';

  constructor(private readonly http: HttpClient) {}

  getAppointments(params?: {
    page?: number;
    size?: number;
    sort?: string;
    fromDate?: string;
    toDate?: string;
    type?: string;
    status?: string;
    search?: string;
  }): Observable<PaginatedResponse<PatientAppointment>> {
    let httpParams = new HttpParams();
    if (params?.page != null) httpParams = httpParams.set('page', params.page);
    if (params?.size != null) httpParams = httpParams.set('size', params.size);
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);
    if (params?.fromDate) httpParams = httpParams.set('fromDate', params.fromDate);
    if (params?.toDate) httpParams = httpParams.set('toDate', params.toDate);
    if (params?.type) httpParams = httpParams.set('type', params.type);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.search) httpParams = httpParams.set('search', params.search);

    return this.http.get<PaginatedResponse<PatientAppointment>>(this.baseUrl, { params: httpParams });
  }

  getAppointmentDetails(id: string): Observable<PatientAppointment> {
    return this.http.get<PatientAppointment>(`${this.baseUrl}/${id}`);
  }

  createAppointment(payload: CreatePatientAppointment): Observable<PatientAppointment> {
    return this.http.post<PatientAppointment>(this.baseUrl, payload);
  }

  updateAppointment(id: string, payload: UpdatePatientAppointment): Observable<PatientAppointment> {
    return this.http.put<PatientAppointment>(`${this.baseUrl}/${id}`, payload);
  }

  cancelAppointment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getNextAppointment(): Observable<PatientAppointment> {
    return this.http.get<PatientAppointment>(`${this.baseUrl}/next`);
  }

  getStats(): Observable<AppointmentStats> {
    return this.http.get<AppointmentStats>(`${this.baseUrl}/stats`);
  }

  getTimeline(): Observable<TimelineEvent[]> {
    return this.http.get<TimelineEvent[]>(`${this.baseUrl}/timeline`);
  }
}
```

### 2.3 Gestion d’erreur (service)

Approche recommandée : laisser le service “thin” (sans UI), et gérer les erreurs dans le composant/container.

Option alternative : utilitaire privé `handleHttpError` partagé dans le service si vous standardisez les erreurs API.

---

## 3) Modifications du composant Appointments

Fichier cible :

`src/app/patient/appointments/appointments.component.ts`

### 3.1 Variables du composant

```ts
appointments: PatientAppointment[] = [];
nextAppointment: PatientAppointment | null = null;
stats: AppointmentStats | null = null;
timeline: TimelineEvent[] = [];

loading = false;
error: string | null = null;

page = 0;
size = 20;
search = '';
```

### 3.2 Dépendances à injecter

```ts
constructor(private readonly appointmentService: PatientAppointmentService) {}
```

### 3.3 Chargement initial dans `ngOnInit`

Utiliser `forkJoin` pour charger en parallèle :

- liste des rendez-vous
- stats
- timeline
- prochain rendez-vous

```ts
ngOnInit(): void {
  this.loadAll();
}

private loadAll(): void {
  this.loading = true;
  this.error = null;

  forkJoin({
    appointmentsPage: this.appointmentService.getAppointments({
      page: this.page,
      size: this.size,
      search: this.search || undefined,
      sort: 'date,asc'
    }),
    stats: this.appointmentService.getStats(),
    timeline: this.appointmentService.getTimeline(),
    nextAppointment: this.appointmentService.getNextAppointment()
  })
    .pipe(
      finalize(() => (this.loading = false)),
      catchError((err) => {
        this.error = this.mapError(err);
        return throwError(() => err);
      })
    )
    .subscribe(({ appointmentsPage, stats, timeline, nextAppointment }) => {
      this.appointments = appointmentsPage.content;
      this.stats = stats;
      this.timeline = timeline;
      this.nextAppointment = nextAppointment;
    });
}
```

### 3.4 Méthodes d’actions UI

```ts
createAppointment(payload: CreatePatientAppointment): void {
  this.loading = true;
  this.error = null;

  this.appointmentService.createAppointment(payload)
    .pipe(finalize(() => (this.loading = false)))
    .subscribe({
      next: () => this.loadAll(),
      error: (err) => (this.error = this.mapError(err))
    });
}

updateAppointment(id: string, payload: UpdatePatientAppointment): void {
  this.loading = true;
  this.error = null;

  this.appointmentService.updateAppointment(id, payload)
    .pipe(finalize(() => (this.loading = false)))
    .subscribe({
      next: () => this.loadAll(),
      error: (err) => (this.error = this.mapError(err))
    });
}

cancelAppointment(id: string): void {
  this.loading = true;
  this.error = null;

  this.appointmentService.cancelAppointment(id)
    .pipe(finalize(() => (this.loading = false)))
    .subscribe({
      next: () => this.loadAll(),
      error: (err) => (this.error = this.mapError(err))
    });
}
```

### 3.5 Mapping d’erreur utilisateur

```ts
private mapError(err: any): string {
  if (err?.status === 0) return 'Réseau indisponible. Vérifiez votre connexion.';
  if (err?.status >= 500) return 'Erreur serveur. Veuillez réessayer.';
  if (err?.status === 400 && err?.error?.message) return err.error.message;
  return 'Une erreur inattendue est survenue.';
}
```

---

## 4) Remplacement des données statiques dans le HTML (sans changer la structure)

Fichier cible :

`src/app/patient/appointments/appointments.component.html`

### 4.1 Règle stricte

- Ne pas toucher aux classes CSS (`stat-v4`, `item-v4`, `node-v4`, etc.).
- Ne pas supprimer de blocs visuels.
- Remplacer uniquement le contenu hardcodé par des bindings Angular.

### 4.2 Zones à rendre dynamiques

1. **Stats header**
   - `18 Fév. · 14:00` → `nextAppointment?.date | date:'dd MMM · HH:mm':'':'fr'`
   - `4 Praticiens` → `stats?.specialistsCount`
   - `2 Prévus` → `stats?.examsCount`
   - `Étape 2 / 6` → `stats?.treatmentStep / stats?.treatmentTotalSteps`

2. **Feed “Prochainement”**
   - Les 3 cartes hardcodées deviennent un `@for`/`*ngFor` sur `appointments` triés par date.

3. **Timeline track**
   - Les nœuds statiques deviennent un `@for`/`*ngFor` sur `timeline`, en conservant les classes d’état :
     - `completed`
     - `active`
     - état vide (upcoming)

4. **Calendar panel**
   - Si vous ne changez pas la structure de grille, injecter uniquement les événements dans les cellules déjà présentes.
   - Version robuste: garder grille fixe pour respecter le design, mais alimenter les libellés d’événements depuis `appointments` filtrés par jour.

### 4.3 États d’UI à ajouter (sans casser le layout)

- `loading`: afficher message discret dans les zones de contenu (ex: `Chargement...`).
- `error`: bandeau texte simple sous le header.
- `empty`: quand `appointments.length === 0`, afficher “Aucun rendez-vous à afficher”.

---

## 5) Gestion des réponses API paginées

Structure backend cible :

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0
}
```

Mapping recommandé côté composant :

- `this.appointments = response.content`
- `this.page = response.number`
- optionnel : stocker `totalElements` et `totalPages` pour pagination future

---

## 6) Gestion des erreurs (RxJS)

### 6.1 Pattern standard

- `catchError` pour transformer l’erreur technique en message métier.
- `finalize` pour reset du loader.
- éviter les `subscribe` imbriqués.

### 6.2 Cas à couvrir

- `status === 0` : réseau
- `status >= 500` : backend indisponible
- `status === 400/422` : validation backend
- `status === 401/403` : session/permissions (rediriger via logique auth globale)

---

## 7) Chargement au démarrage

Séquence recommandée :

1. `ngOnInit()` appelle `loadAll()`
2. `loadAll()` déclenche les 4 endpoints en parallèle
3. Bind les propriétés d’écran
4. Active les actions CRUD

Optimisation : si `getNextAppointment` est déjà inclus dans `getAppointments`, supprimer l’appel dédié pour réduire les requêtes.

---

## 8) Performance et maintenabilité

- Préférer `@for (...; track item.id)` ou `*ngFor="...; trackBy: trackById"`.
- Ne jamais recalculer lourdement dans le template.
- Centraliser tous les appels HTTP dans `PatientAppointmentService`.
- Si la page grossit, passer en approche `vm$` (ViewModel Observable + `async` pipe).

---

## 9) Compatibilité stricte Backend / Frontend

Checklist obligatoire :

- Champs JSON identiques à l’API (nommage inclus).
- Enums synchronisés (mêmes valeurs string).
- Dates ISO transport, format FR uniquement à l’affichage.
- Codes HTTP attendus pour create/update/cancel.
- Cas `null` tolérés côté UI (`nextAppointment`, `stats`).

---

## 10) Plan d’implémentation concret (ordre recommandé)

1. Créer `appointment.model.ts`.
2. Créer `patient-appointment.service.ts` avec les 8 endpoints.
3. Mettre à jour `appointments.component.ts` (state + appels service + erreurs).
4. Remplacer les valeurs hardcodées dans `appointments.component.html` par des bindings.
5. Vérifier qu’aucune classe CSS existante n’a changé.
6. Tester :
   - chargement initial
   - création
   - modification
   - annulation
   - erreur réseau

---

## 11) Résultat attendu

Le module Rendez-vous Patient doit :

- récupérer les données backend en dynamique
- afficher les données sans altérer l’UI actuelle
- créer/modifier/annuler un rendez-vous
- afficher stats + timeline réelles
- gérer loading, erreurs et état vide proprement

---

## 12) Notes d’alignement avec votre base actuelle

- Le composant actuel est vide côté TypeScript: l’intégration peut être faite sans dette technique de migration.
- Le template actuel est très riche visuellement mais hardcodé : prioriser des bindings ciblés avant toute refactorisation structurelle.
- Vous pouvez conserver `appointment.service.ts` tel quel et introduire `patient-appointment.service.ts` pour isoler le domaine patient proprement.
