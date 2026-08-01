# Frontend/Backend Handoff Specification

## 1. Conversation Overview
- **Primary Objectives**: Backend robustesse (disponibilité médecin, calendrier, validation stricte des créneaux), ajout du champ timezone, nouveaux endpoints, documentation frontend détaillée.
- **Session Context**: Implémentation backend → compilation/validation → génération du guide d’intégration frontend.
- **Intent Evolution**: Backend robuste, validation stricte, puis documentation frontend exhaustive.

## 2. Technical Foundation
- **Stack**: Spring Boot (Java), REST API, DTOs, Entities, Services, Controllers.
- **REST API**: CRUD, calendrier, validation créneaux, reprogrammation, annulation.
- **DTOs/Entities**: DoctorProfile (avec timezone), Availability, AvailabilityException, Appointment (avec rescheduledFrom).
- **Patterns**: Soft delete, enum status, validation IANA timezone, slot validation stricte, gestion globale des erreurs.

## 3. Codebase Status
### DoctorProfile.java
- **Champ ajouté**: `timezone` (String, NOT NULL, default UTC)
- **Utilisation**: calendrier, rendez-vous, registration

### Availability.java
- **Nouvelle entité**: grille hebdomadaire du médecin
- **Champs**: dayOfWeek, startHour, endHour, slotDuration, isActive (default true)

### AvailabilityException.java
- **Nouvelle entité**: blocage de créneaux
- **Champs**: startDate (obligatoire), startHour/endHour (optionnels), reason, isActive

### Appointment.java
- **Champ ajouté**: rescheduledFrom (self-reference)
- **Champs**: status, mode, rescheduledFrom

### DTOs
- Tous les DTOs (create/update/response) synchronisés avec les champs backend.

### Controllers
- Endpoints exposés : AvailabilityController, DoctorController, AppointmentController, PatientAppointmentController, RegistrationController

### Services
- Validation stricte, logique calendrier, propagation timezone, gestion reprogrammation.

### GlobalExceptionHandler.java
- Format d’erreur standardisé.

## 4. Problem Resolution
- **Problèmes**: réservation hors disponibilité, blocage exception, mismatch timezone, normalisation status, traçabilité reprogrammation.
- **Solutions**: validation stricte, propagation timezone, logique reprogrammation, gestion centralisée des erreurs.

## 5. Progress Tracking
- **Tâches terminées**: backend complet, compilation, validation, spec frontend.
- **Travail partiel**: aucun.
- **Validation**: compilation OK, endpoints et champs documentés.

## 6. Active Work State
- **Focus**: lecture des fichiers backend pour garantir la spec frontend.
- **Code actif**: nouveaux champs (timezone, rescheduledFrom), nouvelles entités, DTOs, controllers, services.

## 7. Endpoints & Contracts
### Doctor
- **GET /doctors/{id}**: retourne profil (incl. timezone)
- **POST /doctors**: création (timezone obligatoire)
- **PUT /doctors/{id}**: update (timezone modifiable)

### Availability
- **GET /doctors/{id}/availability**: grille hebdomadaire
- **POST /doctors/{id}/availability**: création
- **PUT /availability/{id}**: update
- **DELETE /availability/{id}**: soft delete

### AvailabilityException
- **GET /doctors/{id}/exceptions**: liste exceptions
- **POST /doctors/{id}/exceptions**: création
- **PUT /exceptions/{id}**: update
- **DELETE /exceptions/{id}**: soft delete

### Appointment
- **GET /appointments/{id}**: détails (incl. rescheduledFrom)
- **POST /appointments**: création (validation stricte slot, timezone)
- **PUT /appointments/{id}**: update (reprogrammation, traçabilité)
- **DELETE /appointments/{id}**: annulation

## 8. Validation & Error Handling
- **Validation**: tous les champs obligatoires, enums synchronisés, timezone IANA validée.
- **Error Format**: standardisé via GlobalExceptionHandler.

## 9. Frontend Checklist
- Champs, types, nullabilité, valeurs par défaut, enums, validation, endpoints, format d’erreur.
- Respect strict des contrats backend.

---

**Generated: March 18, 2026**
