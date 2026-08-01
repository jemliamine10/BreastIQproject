# Contrat Frontend ↔ Backend — Rendez-vous Patient (version stable)

Date: 2026-03-16

Ce document résume **ce qui est lié au endpoint details** et les règles à respecter côté frontend pour que le module rendez-vous fonctionne sans erreur.

---

## 1) Endpoints officiels à utiliser

Base: `/api/patient/appointments`

- `GET /api/patient/appointments?patientId=...&date=...&type=...&status=...&doctorId=...&page=0&size=20`
- `GET /api/patient/appointments/{id}?patientId=...`  ← **details**
- `POST /api/patient/appointments`
- `PUT /api/patient/appointments/{id}`
- `DELETE /api/patient/appointments/{id}?patientId=...`
- `GET /api/patient/appointments/next?patientId=...`
- `GET /api/patient/appointments/stats?patientId=...`
- `GET /api/patient/appointments/timeline?patientId=...`

Routes alias encore disponibles:
- `POST /api/appointments/create`
- `GET /api/appointments/patient`

---

## 2) Ce qui est “lié” dans `details`

Endpoint: `GET /api/patient/appointments/{id}?patientId=UUID`

Règle d’accès backend:
- le rendez-vous est renvoyé **uniquement** s’il appartient au `patientId` fourni,
- sinon réponse de type "introuvable pour ce patient".

Cela signifie côté frontend:
- toujours envoyer le **bon** `patientId` (profil patient connecté),
- ne jamais appeler `details` avec un `patientId` d’un autre compte.

---

## 3) Contrat JSON `PatientAppointmentDto`

Réponse de `details` (et de la liste/create/update) :

```json
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
  "notes": ["Prévoir bilan", "Venir avec résultats"]
}
```

Important:
- `notes` est une liste de `string` (jamais un objet),
- `notes` peut être vide (`[]`),
- le backend charge désormais correctement `notes` avant sérialisation (pas de `LazyInitializationException` attendue).

---

## 4) Payloads frontend à respecter

### Create
`POST /api/patient/appointments`

```json
{
  "patientId": "uuid",
  "doctorId": "uuid",
  "type": "CONSULTATION",
  "title": "Consultation Oncologie",
  "description": "Suivi trimestriel",
  "date": "2026-02-18T14:00:00Z",
  "endDate": "2026-02-18T14:30:00Z",
  "location": "Paris",
  "notes": ["Prévoir ordonnance"]
}
```

### Update
`PUT /api/patient/appointments/{id}`

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

---

## 5) `next` (format réel actuel)

Endpoint: `GET /api/patient/appointments/next?patientId=...`

Le backend renvoie toujours:

```json
{
  "nextAppointment": { ...PatientAppointmentDto... }
}
```

ou

```json
{
  "nextAppointment": null
}
```

Donc côté frontend, gérer `nextAppointment` nullable.

---

## 6) Enums à envoyer en MAJUSCULES exactes

- `type`: `CONSULTATION | EXAM | TREATMENT | FOLLOW_UP | OTHER`
- `status`: `UPCOMING | REQUESTED | CONFIRMED | CANCELLED | COMPLETED | NO_SHOW`

---

## 7) Checklist frontend “marche 100%”

- Utiliser le `patientId` du profil connecté pour tous les endpoints patient.
- Pour `details`, toujours passer `id` + `patientId` ensemble.
- Modéliser `notes: string[]` dans tous les modèles TS liés aux rendez-vous patient.
- Tolérer `notes = []` sans erreur d’affichage.
- Pour `/next`, gérer `nextAppointment: null`.
- Afficher les messages backend pour erreurs `400/404/409`.
- Conserver les enums en MAJUSCULES exactes.

---

## 8) Mapping TypeScript minimal recommandé

```ts
export interface PatientAppointmentDto {
  id: string;
  type: 'CONSULTATION' | 'EXAM' | 'TREATMENT' | 'FOLLOW_UP' | 'OTHER';
  title?: string;
  description?: string;
  date: string;
  endDate: string;
  status: 'UPCOMING' | 'REQUESTED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';
  location?: string;
  doctor?: {
    id: string;
    firstName?: string;
    lastName?: string;
    specialty?: string;
    contact?: string;
    structure?: string;
  };
  notes: string[];
}

export interface NextAppointmentResponse {
  nextAppointment: PatientAppointmentDto | null;
}
```
