# Clinical Monitoring Front-End Integration Reference

## 1) Scope and integration contract
This document is the definitive front-end contract for the clinical monitoring backend modules:
- Tracker
- Medical Record
- Treatment Management
- Alerts
- Timeline
- WebSocket notifications
- HealthScore and PatientStatus

Important integration rule:
- Front-end does not compute clinical score, risk level, status, or timeline events.
- Backend computes and persists all clinical intelligence fields.

Current access-control state in code:
- No controller-level role guards are currently enforced (no role annotations/security filter constraints on these routes).
- Use intended role mapping below for UI behavior (patient/doctor), but do not assume backend blocks unauthorized calls yet.

---

## 2) End-to-end orchestration flow (tracker submission)
When front calls POST /api/tracker, backend executes this chain:
1. Save TrackerEntry
2. Evaluate AlertEngine clinical rules
3. Compute RiskEngine score and level
4. Compute HealthScoreService score (0-100) and update PatientProfile.patientStatus
5. Create Timeline event(s)
6. Push WebSocket notification if status changed

Expected front behavior:
- Refresh patient status widget after successful tracker submission.
- Refresh alerts list for assigned doctor views.
- Refresh timeline feed.
- Subscribe to WebSocket topics to react in near real-time.

---

## 3) Entity reference (fields, constraints, read/write, auto-managed)

### 3.1 PatientProfile
Enum values:
- PatientStatus: STABLE, WARNING, CRITICAL
- BloodType: A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE, O_POSITIVE, O_NEGATIVE

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK, non-null, non-updatable | No | Read-only |
| user | User ref | OneToOne required, unique | No (via these APIs) | Read-only relation |
| assignedDoctor | DoctorProfile ref | ManyToOne optional | Yes (Patient update endpoint) | Usually doctor-assignment workflow |
| medicalRecordNumber | String | max 80 | Yes | Optional |
| emergencyContactName | String | max 120 | Yes | Optional |
| emergencyContactPhone | String | max 30 | Yes | Optional |
| heightCm | Integer | nullable | Yes | Used for BMI auto-calc |
| weightKg | Double | nullable | Yes | Used for BMI auto-calc |
| profileCompletion | Integer | default 0 | No direct endpoint in scope | System/business-controlled |
| bloodType | BloodType enum | varchar(20) | Yes (medical record create) | Clinical profile field |
| healthScore | Integer | default 100 | No | Auto-calculated by backend |
| patientStatus | PatientStatus enum | varchar(20), default STABLE | No | Auto-derived from healthScore |
| medicalConsent | boolean | non-null, default false | Yes | Setting true auto-sets consentTimestamp now |
| consentTimestamp | Instant | nullable | No (treat as read-only) | Auto-managed when consent toggled |
| lastKnownLatitude | Double | nullable | Yes | Coordinate validation in PatientService location/update |
| lastKnownLongitude | Double | nullable | Yes | Coordinate validation in PatientService location/update |
| bmi (transient) | Double | computed | No | Auto-calculated from height/weight |

Computed fields:
- bmi = weightKg / (heightM^2), rounded to 2 decimals
- patientStatus from healthScore thresholds (see section 7)

Read-only for front:
- healthScore, patientStatus, bmi, consentTimestamp (practically), id

---

### 3.2 MedicalRecord
Enum values:
- CancerStage: STAGE_0, STAGE_I, STAGE_II, STAGE_III, STAGE_IV
- TumorType: HR_POSITIVE, HER2_POSITIVE, TRIPLE_NEGATIVE, HR_POSITIVE_HER2_POSITIVE, UNKNOWN

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK, non-null, non-updatable | No | Read-only |
| patient | PatientProfile ref | OneToOne required, unique | Indirect (patientId in create) | Relation managed by backend |
| diagnosis | String | max 300 | Yes | Optional update via diagnosis endpoint |
| cancerStage | enum | varchar(20) | Yes | Optional update |
| tumorType | enum | varchar(40) | Yes | Optional update |
| consentGiven | boolean | non-null, default false | Yes | Clinical consent in record context |
| notes | String | max 2000 | Yes | Optional |
| clinicalData | ClinicalData ref | OneToOne mapped | Yes (nested create/update endpoint) | Section object |
| createdAt | Instant | non-null, non-updatable | No | Auto set |
| updatedAt | Instant | non-null | No | Auto updated on persistence update |

Read-only:
- id, createdAt, updatedAt

---

### 3.3 ClinicalData
Enum values:
- ReceptorStatus: POSITIVE, NEGATIVE, UNKNOWN

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| medicalRecord | MedicalRecord ref | OneToOne required, unique | Indirect | Managed via patient medical record |
| estrogenReceptor | enum | varchar(20) | Yes | Optional |
| progesteroneReceptor | enum | varchar(20) | Yes | Optional |
| her2Status | enum | varchar(20) | Yes | Optional |
| ki67 | Double | comment: percentage 0-100 (not bean-validated) | Yes | Front should enforce 0-100 |
| tumorSize | Double | stored as mm (column tumor_size_mm) | Yes | Front should display units |
| lymphNodesInvolved | Integer | nullable | Yes | Optional |
| metastasis | boolean | non-null, default false | Yes | |
| grade | Integer | comment: 1/2/3 (not bean-validated) | Yes | Front should constrain to 1-3 |
| notes | String | max 2000 | Yes | Optional |
| createdAt | Instant | non-null | No | Auto set |
| updatedAt | Instant | non-null | No | Auto updated |

Read-only:
- id, createdAt, updatedAt

---

### 3.4 MedicalHistory
Enum values:
- HistoryType: PERSONAL, FAMILY, SURGICAL

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| patient | PatientProfile ref | ManyToOne required | Indirect (patient path param) | |
| historyType | enum | non-null, varchar(20) | Yes | Required logically and DB-level |
| description | String | non-null, max 2000 | Yes | Required logically and DB-level |
| eventDate | LocalDate | nullable | Yes | Optional |
| deleted | boolean | non-null, default false | No | Soft delete flag |
| deletedAt | Instant | nullable | No | Auto-set on soft delete |
| createdAt | Instant | non-null | No | Auto set |
| updatedAt | Instant | non-null | No | Auto updated |

Soft-delete behavior:
- Delete endpoint does not hard-delete. It sets deleted=true and deletedAt=now.
- Aggregated medical record only returns histories where deleted=false.

---

### 3.5 Treatment
Enum values:
- TreatmentType: CHEMO, RADIO, SURGERY, HORMONAL, IMMUNOTHERAPY
- Status: UPCOMING, ACTIVE, COMPLETED, STOPPED

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| patient | PatientProfile ref | ManyToOne required | Indirect via patientId | |
| treatmentType | enum | non-null, varchar(30) | Yes | Required in treatment-management create |
| protocol | String | max 120 | Yes | Optional |
| medicationName | String | max 200 | Yes | Optional |
| dosage | String | max 100 | Yes | Optional |
| startDate | LocalDate | nullable | Yes | Strongly recommended when cycles provided |
| endDate | LocalDate | nullable | Yes | Optional |
| cyclesTotal | Integer | nullable | Yes | If >0 and startDate set, sessions auto-generated |
| currentCycle | Integer | default 0 | No (should be read-only in clinical flow) | Auto-updated when sessions marked DONE |
| status | enum | non-null, default UPCOMING | No in treatment-management flow | Auto-refreshed by refreshStatus logic |
| notes | String | max 1200 | Yes | Optional |
| deleted | boolean | non-null, default false | No | Soft delete flag |
| sessions | list TreatmentSession | mapped relation | No direct write | Read via sessions endpoint |

Status auto-computation logic:
- STOPPED stays STOPPED
- today < startDate => UPCOMING
- today > endDate => COMPLETED
- currentCycle >= cyclesTotal => COMPLETED
- startDate reached => ACTIVE

Soft-delete behavior:
- Delete marks deleted=true and keeps row.
- Patient listing endpoints filter out deleted treatments.

---

### 3.6 TreatmentSession
Enum values:
- SessionStatus: PLANNED, DONE, MISSED, CANCELLED

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| treatment | Treatment ref | ManyToOne required | No | Managed by backend |
| sessionNumber | Integer | non-null | No (auto on generation) | Auto-generated from cycle index |
| scheduledDate | LocalDate | non-null | No (auto on generation) | Computed from startDate + intervalDays |
| actualDate | LocalDate | nullable | No manual write in this flow | Auto-set when marked DONE |
| status | enum | non-null, default PLANNED | No direct write | Changed via done/missed endpoints |
| notes | String | max 1200 | Yes via done/missed | Optional |
| sideEffects | String | max 2000 | Yes via done endpoint | Optional |
| createdAt | Instant | non-null | No | Auto set |
| updatedAt | Instant | non-null | No | Auto updated |

---

### 3.7 TrackerEntry
| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| patient | PatientProfile ref | ManyToOne required | Indirect via patientId in DTO | |
| painLevel | Integer | DTO: min 0, max 10 | Yes | Optional but clinically recommended |
| fatigueLevel | Integer | DTO: min 0, max 10 | Yes | Optional |
| moodLevel | Integer | DTO: min 0, max 10 | Yes | Optional (10=best mood) |
| temperature | Double | no bean validation | Yes | Celsius expected |
| weight | Double | no bean validation | Yes | kg expected |
| vomiting | boolean | non-null default false | Yes | |
| diarrhea | boolean | non-null default false | Yes | |
| appetiteLoss | boolean | non-null default false | Yes | |
| notes | String | max 2000 | Yes | Optional |
| recordedAt | Instant | non-null | No | Auto-set to now on submit |

Post-submit computed response enrichments:
- healthScore (Integer)
- riskLevel (String from RiskLevel enum)

---

### 3.8 Alert
Enum values:
- Severity: LOW, MEDIUM, HIGH, CRITICAL
- AlertType: INFECTION_RISK, SEVERE_PAIN, RAPID_WEIGHT_LOSS, COMBINED_SYMPTOMS, HIGH_TEMPERATURE, MISSED_SESSION, CUSTOM

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| patient | PatientProfile ref | ManyToOne required | No | Derived from tracker/patient context |
| severity | enum | non-null | No | Set by AlertEngine rule |
| alertType | enum | non-null | No | Set by AlertEngine rule |
| message | String | non-null, max 2000 | No | System-generated clinical message |
| triggerData | String | max 2000 | No | Structured context string |
| resolved | boolean | non-null default false | No direct write | Changes via resolve endpoint |
| resolvedAt | Instant | nullable | No | Auto-set on resolve |
| resolvedBy | UUID | nullable | No | Set from doctorUserId resolve param |
| resolutionNotes | String | max 1000 | Yes via resolve endpoint notes | Optional |
| createdAt | Instant | non-null, non-updatable | No | Auto-set |

---

### 3.9 MedicalEvent (Timeline)
Enum values:
- EventType: DIAGNOSIS, TREATMENT_START, TREATMENT_END, SESSION_COMPLETED, SESSION_MISSED, ALERT_GENERATED, TRACKER_ENTRY, APPOINTMENT, MEDICAL_NOTE, STATUS_CHANGE

| Field | Type | Constraints | Front write? | System-managed / Notes |
|---|---|---|---|---|
| id | UUID | PK | No | Read-only |
| patient | PatientProfile ref | ManyToOne required | No | Backend internal service |
| eventType | enum | non-null | No | Set by service that records event |
| title | String | non-null, max 300 | No | System-generated |
| description | String | max 2000 | No | System-generated |
| severity | String | max 20 | No | Optional, mostly alert/status context |
| referenceId | UUID | nullable | No | Source entity id |
| referenceType | String | max 50 | No | Source entity type |
| eventDate | Instant | non-null | No | Auto set at record time |
| createdAt | Instant | non-null | No | Auto set |

---

## 4) Service and intelligence layer behavior (front perspective)

### 4.1 TrackerService
What front sends:
- Daily tracker payload

What backend auto-does:
- Persists tracker entry
- Runs AlertEngine rules
- Computes risk score/level
- Computes healthScore and patientStatus
- Adds timeline entry TRACKER_ENTRY
- Adds STATUS_CHANGE event if status changed
- Sends WebSocket status message when status changes

Front implication:
- One POST triggers multi-object side effects; refresh tracker, timeline, alerts, and status UI.

### 4.2 AlertEngine
Clinical rule triggers:
- Infection risk: temp >= 38.3 AND active CHEMO AND chemo session done within 7 days => CRITICAL INFECTION_RISK
- Severe pain: pain >= 8 => CRITICAL SEVERE_PAIN
- Rapid weight loss: > 3.0 kg loss in 10 days => CRITICAL RAPID_WEIGHT_LOSS
- Combined symptoms: fatigue >= 7 AND mood <= 3 AND appetiteLoss=true => HIGH COMBINED_SYMPTOMS
- High temperature: temp >= 38.0 (if no infection-risk rule already fired) => HIGH HIGH_TEMPERATURE

Backend side effects per generated alert:
- Persist Alert
- Record timeline ALERT_GENERATED
- Push doctor WebSocket alert if assigned doctor exists

### 4.3 RiskEngine
What it does:
- Computes riskScore 0-100 and riskLevel LOW/MEDIUM/HIGH for latest tracker context

Factors:
- Recent chemo session (<14 days): +25
- Temperature: sub-febrile +10, high fever +20
- Fatigue: moderate +8, high +15
- Pain: moderate +8, high +15
- Low mood <=3: +10
- GI symptoms (vomiting/diarrhea/appetite loss): +5 each

Front usage:
- riskLevel returned in tracker response. Display as indicator only.

### 4.4 HealthScoreService
What it does:
- Computes healthScore 0-100 from tracker values
- Updates PatientProfile.healthScore and PatientProfile.patientStatus

Front rule:
- Never compute or overwrite healthScore/patientStatus on front.
- Always trust backend values.

### 4.5 MedicalRecordService
What it does:
- Creates aggregated medical dossier
- Updates diagnosis section independently
- Updates clinical data section independently
- Adds/deletes medical history (soft delete)
- Returns consolidated response including:
  - medical record
  - patient bloodType/height/weight/bmi
  - non-deleted medical histories
  - non-deleted allergies

Timeline trigger:
- DIAGNOSIS event at full record creation

### 4.6 TreatmentManagementService
What it does:
- Creates treatment
- Auto-generates treatment sessions when cyclesTotal and startDate are provided
- Marks sessions DONE/MISSED
- Auto-updates treatment currentCycle and status
- Soft-deletes treatment

Timeline triggers:
- TREATMENT_START on create
- SESSION_COMPLETED on done
- SESSION_MISSED on missed
- TREATMENT_END on soft delete

### 4.7 TimelineService
What it does:
- Internal event recorder used by other services
- Exposes timeline retrieval endpoints (all or by event type)

### 4.8 NotificationService
What it does:
- Pushes doctor alerts to topic /topic/alerts/{doctorId}
- Pushes patient status updates to topic /topic/status/{patientId}

---

## 5) API endpoint reference (method, payload, response, validation, role)

Role note for all endpoints:
- Backend currently has no explicit role enforcement on these routes.
- Intended role column below is functional ownership for front UX.

### 5.1 Tracker APIs

#### POST /api/tracker
Intended role:
- Patient app

Request body (TrackerEntryCreateDto):
| Field | Type | Required | Validation |
|---|---|---|---|
| patientId | UUID | Yes | NotNull |
| painLevel | Integer | No | 0..10 |
| fatigueLevel | Integer | No | 0..10 |
| moodLevel | Integer | No | 0..10 |
| temperature | Double | No | no backend min/max |
| weight | Double | No | no backend min/max |
| vomiting | boolean | No | default false |
| diarrhea | boolean | No | default false |
| appetiteLoss | boolean | No | default false |
| notes | String | No | implicit DB max 2000 |

Response body (TrackerEntryResponseDto):
| Field | Type |
|---|---|
| id | UUID |
| patientId | UUID |
| painLevel | Integer |
| fatigueLevel | Integer |
| moodLevel | Integer |
| temperature | Double |
| weight | Double |
| vomiting | boolean |
| diarrhea | boolean |
| appetiteLoss | boolean |
| notes | String |
| recordedAt | Instant |
| healthScore | Integer |
| riskLevel | String (LOW/MEDIUM/HIGH) |

System-managed notes:
- recordedAt, healthScore, riskLevel are backend-generated.
- This call can generate alerts, timeline entries, and WebSocket notifications.

Example request:
{
  "patientId": "2ca0f5d8-b846-4ec2-b3d0-0474b7266a70",
  "painLevel": 8,
  "fatigueLevel": 7,
  "moodLevel": 3,
  "temperature": 38.4,
  "weight": 58.2,
  "vomiting": true,
  "diarrhea": false,
  "appetiteLoss": true,
  "notes": "Frissons depuis ce matin"
}

#### GET /api/tracker/patient/{patientId}
Intended role:
- Patient app, doctor app

Request:
- Path param patientId UUID

Response:
- Array of TrackerEntryResponseDto sorted recordedAt desc

#### GET /api/tracker/patient/{patientId}/latest
Intended role:
- Patient app, doctor app

Response:
- Single TrackerEntryResponseDto (latest)

---

### 5.2 Medical Record APIs

#### POST /api/medical-records
Intended role:
- Doctor app (or onboarding admin flow)

Request body (MedicalRecordCreateDto):
| Field | Type | Required | Validation |
|---|---|---|---|
| patientId | UUID | Yes | NotNull |
| diagnosis | String | No | DB max 300 |
| cancerStage | enum CancerStage | No | enum parse |
| tumorType | enum TumorType | No | enum parse |
| consentGiven | boolean | No | default false |
| notes | String | No | DB max 2000 |
| bloodType | enum BloodType | No | enum parse |
| heightCm | Integer | No | none |
| weightKg | Double | No | none |
| clinicalData | ClinicalDataDto | No | nested object |

clinicalData fields:
- estrogenReceptor, progesteroneReceptor, her2Status: ReceptorStatus enum
- ki67: Double
- tumorSize: Double (mm)
- lymphNodesInvolved: Integer
- metastasis: boolean
- grade: Integer
- notes: String

Response (MedicalRecordResponseDto):
- id, patientId, diagnosis, cancerStage, tumorType, consentGiven, notes
- bloodType, heightCm, weightKg, bmi
- clinicalData object
- medicalHistories array (non-deleted)
- allergies array (non-deleted)
- createdAt, updatedAt

System-managed:
- bmi computed backend
- createdAt/updatedAt backend
- timeline DIAGNOSIS event created

#### GET /api/medical-records/patient/{patientId}
Intended role:
- Doctor app, patient app (read-only view)

Response:
- MedicalRecordResponseDto aggregated

#### PUT /api/medical-records/patient/{patientId}/diagnosis
Intended role:
- Doctor app

Request:
- Query params (all optional)
  - diagnosis: String
  - cancerStage: CancerStage enum
  - tumorType: TumorType enum

Response:
- Updated MedicalRecordResponseDto

#### PUT /api/medical-records/patient/{patientId}/clinical-data
Intended role:
- Doctor app

Request body:
- ClinicalDataDto

Response:
- ClinicalDataDto

Notes:
- If no clinical data exists, backend creates it.
- metastasis always overwritten from incoming boolean value.

#### POST /api/medical-records/patient/{patientId}/history
Intended role:
- Doctor app

Request body (MedicalHistoryDto):
| Field | Type | Required by API | Required by DB |
|---|---|---|---|
| historyType | enum HistoryType | no bean validation annotation | yes |
| description | String | no bean validation annotation | yes |
| eventDate | LocalDate | optional | optional |

Response:
- MedicalHistoryDto with id and patientId

#### DELETE /api/medical-records/history/{historyId}
Intended role:
- Doctor app

Behavior:
- Soft delete (deleted=true, deletedAt=now)

Response:
- Empty body

---

### 5.3 Treatment Management APIs

#### POST /api/treatment-management
Intended role:
- Doctor app

Request (query params):
| Param | Type | Required | Notes |
|---|---|---|---|
| patientId | UUID | Yes | |
| type | TreatmentType enum | Yes | CHEMO/RADIO/SURGERY/HORMONAL/IMMUNOTHERAPY |
| protocol | String | No | |
| medicationName | String | No | |
| dosage | String | No | |
| startDate | LocalDate | No | Needed for session generation |
| endDate | LocalDate | No | |
| cyclesTotal | Integer | No | If >0 + startDate => sessions auto-generated |
| intervalDays | int | No | default 21 |
| notes | String | No | |

Response:
- Empty body (void)

Side effects:
- Creates treatment
- Auto-generates planned sessions if cyclesTotal/startDate provided
- Timeline TREATMENT_START event

#### DELETE /api/treatment-management/{treatmentId}
Intended role:
- Doctor app

Behavior:
- Soft delete treatment (deleted=true)
- Timeline TREATMENT_END event

Response:
- Empty body

#### GET /api/treatment-management/{treatmentId}/sessions
Intended role:
- Doctor app, patient app (read-only tracking)

Response:
- Array TreatmentSessionResponseDto sorted by sessionNumber asc

#### PUT /api/treatment-management/sessions/{sessionId}/done
Intended role:
- Doctor app

Request (query params):
| Param | Type | Required |
|---|---|---|
| notes | String | No |
| sideEffects | String | No |

Response:
- TreatmentSessionResponseDto

Side effects:
- status -> DONE
- actualDate -> today
- treatment currentCycle recomputed from DONE sessions count
- treatment status refreshed
- timeline SESSION_COMPLETED event

#### PUT /api/treatment-management/sessions/{sessionId}/missed
Intended role:
- Doctor app

Request (query params):
| Param | Type | Required |
|---|---|---|
| reason | String | No |

Response:
- TreatmentSessionResponseDto

Side effects:
- status -> MISSED
- notes may store reason
- timeline SESSION_MISSED event

#### POST /api/treatment-management/patient/{patientId}/refresh
Intended role:
- Doctor app or background admin action

Behavior:
- Recomputes statuses for non-deleted treatments

Response:
- Empty body

---

### 5.4 Alert APIs

#### GET /api/alerts/patient/{patientId}?unresolvedOnly=false
Intended role:
- Patient app (own alerts), doctor app

Request:
- unresolvedOnly optional boolean, default false

Response:
- Array AlertResponseDto sorted createdAt desc

#### GET /api/alerts/doctor/{doctorId}
Intended role:
- Doctor app

Response:
- Array AlertResponseDto for unresolved alerts of patients assigned to doctor

#### PUT /api/alerts/{alertId}/resolve?doctorUserId={uuid}&notes=...
Intended role:
- Doctor app

Request:
| Param | Type | Required |
|---|---|---|
| doctorUserId | UUID | Yes |
| notes | String | No |

Response:
- AlertResponseDto (resolved=true, resolvedAt set)

Side effects:
- Timeline STATUS_CHANGE event with alert resolution context

---

### 5.5 Timeline APIs

#### GET /api/timeline/patient/{patientId}
Intended role:
- Patient app, doctor app

Response:
- Array MedicalEventResponseDto sorted eventDate desc

#### GET /api/timeline/patient/{patientId}/type/{eventType}
Intended role:
- Patient app, doctor app

Request:
- eventType path enum value from MedicalEvent.EventType

Response:
- Filtered array MedicalEventResponseDto sorted eventDate desc

Valid eventType values:
- DIAGNOSIS
- TREATMENT_START
- TREATMENT_END
- SESSION_COMPLETED
- SESSION_MISSED
- ALERT_GENERATED
- TRACKER_ENTRY
- APPOINTMENT
- MEDICAL_NOTE
- STATUS_CHANGE

---

### 5.6 Patient profile support APIs (needed to read HealthScore/PatientStatus)

#### GET /api/patients/{patientProfileId}
Intended role:
- Patient app, doctor app

Response:
- PatientProfileResponseDto

PatientProfileResponseDto fields:
| Field | Type |
|---|---|
| id | UUID |
| userId | UUID |
| assignedDoctorProfileId | UUID nullable |
| medicalRecordNumber | String |
| emergencyContactName | String |
| emergencyContactPhone | String |
| heightCm | Integer |
| weightKg | Double |
| bmi | Double (computed) |
| bloodType | String enum name |
| healthScore | Integer |
| patientStatus | PatientStatus enum |
| medicalConsent | boolean |
| consentTimestamp | Instant nullable |
| lastKnownLatitude | Double |
| lastKnownLongitude | Double |
| allergies | AllergyResponseDto[] (non-deleted only) |
| treatments | TreatmentResponseDto[] (non-deleted only) |

#### GET /api/patients/by-user/{userId}
Intended role:
- Patient app

Response:
- PatientProfileResponseDto

#### PUT /api/patients/{patientProfileId}
Intended role:
- Patient app (self profile), doctor/admin tools

Request body (PatientProfileUpdateRequestDto):
| Field | Type | Required | Validation |
|---|---|---|---|
| medicalRecordNumber | String | No | max 80 |
| emergencyContactName | String | No | max 120 |
| emergencyContactPhone | String | No | max 30 |
| heightCm | Integer | No | none |
| weightKg | Double | No | none |
| medicalConsent | Boolean | No | if provided, backend updates consentTimestamp |
| consentTimestamp | Instant | No | treated as informational; backend controls final value when consent is toggled |
| lastKnownLatitude | Double | No | validated as lat/lon pair |
| lastKnownLongitude | Double | No | validated as lat/lon pair |
| assignedDoctorProfileId | UUID | No | doctor must exist |

Response:
- PatientProfileResponseDto

#### PUT /api/patients/{patientProfileId}/location
Intended role:
- Patient app

Request body (LocationUpdateRequestDto):
| Field | Type | Required |
|---|---|---|
| latitude | Double | Yes |
| longitude | Double | Yes |

Response:
- PatientProfileResponseDto

#### PUT /api/patients/{patientProfileId}/consent?value=true|false
Intended role:
- Patient app

Behavior:
- Updates medicalConsent and auto-updates consentTimestamp.

Response:
- PatientProfileResponseDto

---

## 6) WebSocket integration reference

WebSocket/STOMP endpoint:
- /ws (SockJS enabled)

Broker prefix:
- Server publishes under /topic/**

### 6.1 Topic: /topic/alerts/{doctorId}
Trigger:
- AlertEngine generates an alert and patient has assignedDoctor

Payload format:
| Field | Type | Example |
|---|---|---|
| type | String | CRITICAL_ALERT |
| alertId | UUID | ... |
| patientId | UUID | ... |
| severity | String | CRITICAL |
| alertType | String | INFECTION_RISK |
| message | String | clinical text |
| timestamp | String ISO instant | 2026-03-21T09:30:01Z |
| patientName | String | Jane Doe (if available) |

Example payload:
{
  "type": "CRITICAL_ALERT",
  "alertId": "8f95f8c5-146d-410f-9f39-9ec2117a16b2",
  "patientId": "2ca0f5d8-b846-4ec2-b3d0-0474b7266a70",
  "severity": "CRITICAL",
  "alertType": "INFECTION_RISK",
  "message": "RISQUE D'INFECTION ...",
  "timestamp": "2026-03-21T09:30:01Z",
  "patientName": "Jane Doe"
}

Front actions:
- Insert alert at top of doctor alert feed
- Highlight severity badge (HIGH/CRITICAL)
- Optionally show toast/sound for CRITICAL
- Refresh unresolved count

### 6.2 Topic: /topic/status/{patientId}
Trigger:
- Tracker submission causes patientStatus change

Payload format:
| Field | Type | Example |
|---|---|---|
| type | String | STATUS_UPDATE |
| patientId | UUID | ... |
| status | String | WARNING |
| healthScore | Integer | 42 |
| timestamp | String ISO instant | 2026-03-21T09:31:12Z |

Example payload:
{
  "type": "STATUS_UPDATE",
  "patientId": "2ca0f5d8-b846-4ec2-b3d0-0474b7266a70",
  "status": "WARNING",
  "healthScore": 42,
  "timestamp": "2026-03-21T09:31:12Z"
}

Front actions:
- Update patient status chip and health score gauge immediately
- Add or refresh latest timeline segment
- If status becomes CRITICAL, surface urgent visual state

---

## 7) HealthScore and PatientStatus mapping

Score range:
- Integer 0 to 100, backend-computed

Status thresholds:
- STABLE: score >= 60
- WARNING: score 35 to 59
- CRITICAL: score < 35

Front rule:
- Never recalculate score/status client-side.
- Display exactly what backend returns in TrackerResponse or PatientProfile.

Suggested UI mapping:
- STABLE: green badge
- WARNING: amber/orange badge
- CRITICAL: red badge with urgent marker

---

## 8) Timeline event catalog and reference mapping

All timeline event types (enum):
1. DIAGNOSIS
2. TREATMENT_START
3. TREATMENT_END
4. SESSION_COMPLETED
5. SESSION_MISSED
6. ALERT_GENERATED
7. TRACKER_ENTRY
8. APPOINTMENT
9. MEDICAL_NOTE
10. STATUS_CHANGE

Current known producers in this clinical module:
- DIAGNOSIS: medical record creation
- TREATMENT_START: treatment creation
- TREATMENT_END: treatment soft delete/stop
- SESSION_COMPLETED: mark session done
- SESSION_MISSED: mark session missed
- ALERT_GENERATED: alert engine rules
- TRACKER_ENTRY: tracker submission
- STATUS_CHANGE: patient status transition, alert resolution note
- APPOINTMENT, MEDICAL_NOTE: available enum values (may be produced by other modules)

Reference linkage by event:
- TRACKER_ENTRY -> referenceType TRACKER_ENTRY, referenceId trackerEntry.id
- ALERT_GENERATED -> referenceType ALERT, referenceId alert.id
- SESSION_COMPLETED/SESSION_MISSED -> referenceType TREATMENT_SESSION, referenceId session.id
- TREATMENT_START/TREATMENT_END -> referenceType TREATMENT, referenceId treatment.id
- DIAGNOSIS -> referenceType MEDICAL_RECORD, referenceId medicalRecord.id
- STATUS_CHANGE -> referenceType STATUS_CHANGE or ALERT context depending source

MedicalEventResponseDto for rendering:
| Field | Type |
|---|---|
| id | UUID |
| patientId | UUID |
| eventType | EventType enum |
| title | String |
| description | String |
| severity | String nullable |
| referenceId | UUID nullable |
| referenceType | String nullable |
| eventDate | Instant |
| createdAt | Instant |

Example timeline item payload:
{
  "id": "16791357-5180-445f-8542-8f041dcf2ed5",
  "patientId": "2ca0f5d8-b846-4ec2-b3d0-0474b7266a70",
  "eventType": "ALERT_GENERATED",
  "title": "INFECTION_RISK - CRITICAL",
  "description": "Température 38.6C sous chimiothérapie active...",
  "severity": "CRITICAL",
  "referenceId": "8f95f8c5-146d-410f-9f39-9ec2117a16b2",
  "referenceType": "ALERT",
  "eventDate": "2026-03-21T09:30:01Z",
  "createdAt": "2026-03-21T09:30:01Z"
}

Filter options:
- Full feed: GET /api/timeline/patient/{patientId}
- By type: GET /api/timeline/patient/{patientId}/type/{eventType}

---

## 9) Validation and error contract

Validation sources:
- Bean validation on DTO fields (example: pain/fatigue/mood 0-10, patientId not null)
- Enum parsing on path/query/body fields
- Database-level not-null/length constraints

Global error response shape:
| Field | Type |
|---|---|
| timestamp | String ISO instant |
| status | Integer HTTP code |
| error | String reason phrase |
| message | String human message |

Typical status codes:
- 400 bad request (missing/invalid params, body format, bean validation)
- 404 not found
- 409 conflict (example: duplicate medical record)
- 500 internal error (including DB constraint failures not mapped explicitly)

Front recommendation:
- Display message field directly in debug/admin mode.
- Provide generic localized fallback in patient-facing screens.

---

## 10) Never-edit and system-managed fields checklist

Front-end must never attempt to control these:
- PatientProfile.healthScore
- PatientProfile.patientStatus
- PatientProfile.bmi
- TrackerEntry.recordedAt
- Alert.severity
- Alert.alertType
- Alert.message
- Alert.triggerData
- Alert.createdAt
- Alert.resolvedAt
- Alert.resolvedBy
- MedicalEvent all creation fields (eventType/title/description/severity/reference/eventDate)
- Treatment.currentCycle in treatment-management flow
- Treatment.status in treatment-management flow
- TreatmentSession.actualDate/status in direct payloads (use dedicated done/missed endpoints)
- Soft-delete flags and timestamps: deleted, deletedAt

---

## 11) Practical front integration sequence

Recommended sequence for a patient dashboard:
1. Load patient profile (for healthScore/status)
2. Load latest tracker and recent history
3. Load timeline feed
4. Open WebSocket subscriptions:
   - /topic/status/{patientId}
   - if doctor UI: /topic/alerts/{doctorId}

When patient submits tracker:
1. POST /api/tracker
2. On success, immediately update:
   - tracker widgets from response
   - health score/status from response
3. Refresh alert list (doctor screens)
4. Refresh timeline

When doctor resolves alert:
1. PUT /api/alerts/{alertId}/resolve
2. Remove/reskin alert as resolved in UI
3. Refresh timeline or append resolved event

This sequence matches backend side effects and avoids stale UI states.
