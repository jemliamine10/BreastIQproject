# Consultation des Rendez-vous d’un Docteur : Guide Frontend/Backend

## 1️⃣ Endpoints pour consulter les rendez-vous d’un docteur

### A. Liste des rendez-vous
- **Endpoint** : `GET /api/doctors/{doctorId}/appointments`
- **Contrôleur** : DoctorController ou AppointmentController
- **Méthode** :
  ```java
  @GetMapping("/doctors/{doctorId}/appointments")
  public List<AppointmentResponseDto> getDoctorAppointments(@PathVariable UUID doctorId)
  ```
- **DTO utilisé** : `AppointmentResponseDto`
- **Champs principaux** :
  - `id` (UUID)
  - `patientId` (UUID)
  - `doctorId` (UUID)
  - `startAt` (Instant ou String ISO)
  - `endAt` (Instant ou String ISO)
  - `mode` (enum)
  - `status` (enum)
  - `linkId` (UUID)
  - `rescheduledFrom` (UUID, optionnel)

---

### B. Détail d’un rendez-vous
- **Endpoint** : `GET /api/appointments/{appointmentId}`
- **Contrôleur** : AppointmentController
- **Méthode** :
  ```java
  @GetMapping("/appointments/{appointmentId}")
  public AppointmentResponseDto getAppointment(@PathVariable UUID appointmentId)
  ```
- **DTO utilisé** : `AppointmentResponseDto`
- **Description** : Retourne tous les détails d’un rendez-vous (voir champs ci-dessus)

---

### C. Filtrage ou recherche avancée (optionnel)
- **Endpoint** : `GET /api/doctors/{doctorId}/appointments?status=CONFIRMED&date=2026-03-20`
- **Contrôleur** : DoctorController ou AppointmentController
- **Méthode** :
  ```java
  @GetMapping("/doctors/{doctorId}/appointments")
  public List<AppointmentResponseDto> getDoctorAppointmentsFiltered(@PathVariable UUID doctorId, @RequestParam Map<String, String> filters)
  ```
- **DTO utilisé** : `AppointmentResponseDto`

---

## 2️⃣ Flux complet côté docteur
1. Le frontend appelle `GET /api/doctors/{doctorId}/appointments` pour obtenir la liste.
2. Pour chaque rendez-vous, il peut appeler `GET /api/appointments/{appointmentId}` pour obtenir le détail.
3. Les DTOs utilisés sont toujours `AppointmentResponseDto`, qui contient tous les champs nécessaires pour l’affichage et la gestion.

---

## 3️⃣ Points à retenir pour le frontend
- Toujours utiliser l’UUID du docteur pour lister ses rendez-vous.
- Les champs du DTO doivent être respectés : types, nullabilité, enums.
- Pour afficher le détail, utiliser l’ID du rendez-vous.
- Le frontend peut filtrer ou paginer selon les paramètres supportés par le backend.

---

**Generated: March 18, 2026**
