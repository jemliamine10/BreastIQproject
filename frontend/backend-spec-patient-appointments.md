# Spécification Backend — Module Rendez-vous Patient

## 1. Endpoints API nécessaires

- `GET /api/patient/appointments`  
  Liste paginée/filtrée des rendez-vous du patient connecté.

- `GET /api/patient/appointments/:id`  
  Détail d’un rendez-vous (avec praticien, lieu, documents, etc.).

- `POST /api/patient/appointments`  
  Création d’un nouveau rendez-vous (choix du type, praticien, date, etc.).

- `PUT /api/patient/appointments/:id`  
  Modification d’un rendez-vous (replanification, ajout de notes, etc.).

- `DELETE /api/patient/appointments/:id`  
  Annulation d’un rendez-vous.

- `GET /api/patient/appointments/next`  
  Prochain rendez-vous à venir (pour affichage rapide).

- `GET /api/patient/appointments/stats`  
  Statistiques (nombre de RDV, praticiens, examens, progression).

- `GET /api/patient/appointments/timeline`  
  Timeline du parcours de soins (étapes, progression).

## 2. Modèles / Entités

### Appointment (RendezVous)
- `id: string`
- `patientId: string`
- `doctorId: string`
- `type: enum` (consultation, examen, traitement, contrôle, autre)
- `title: string`
- `description: string`
- `date: datetime`
- `endDate: datetime`
- `status: enum` (à venir, confirmé, annulé, terminé)
- `location: string`
- `notes: string[]`
- `createdAt: datetime`
- `updatedAt: datetime`

### Doctor (Medecin)
- `id: string`
- `firstName: string`
- `lastName: string`
- `specialty: string`
- `contact: string`
- `structure: string`

### Patient
- `id: string`
- `firstName: string`
- `lastName: string`
- `email: string`
- `phone: string`
- `profileCompletion: number`

### TimelineEvent
- `id: string`
- `patientId: string`
- `date: datetime`
- `type: enum` (diagnostic, bilan, traitement, contrôle, suivi)
- `label: string`
- `description: string`
- `status: enum` (completed, active, upcoming)

## 3. Services

- **AppointmentService**
  - CRUD des rendez-vous
  - Recherche/filtrage (par date, type, praticien)
  - Calcul du prochain RDV
  - Statistiques (nombre, progression, etc.)
  - Génération de la timeline

- **DoctorService**
  - Recherche des praticiens liés au patient
  - Détails d’un praticien

- **TimelineService**
  - Génération de la progression du parcours patient

## 4. Controllers

- **AppointmentController**
  - Gère tous les endpoints `/api/patient/appointments`
  - Sécurise l’accès aux rendez-vous du patient connecté

- **DoctorController**
  - Pour la recherche et l’affichage des praticiens

- **TimelineController**
  - Pour la timeline du parcours patient

## 5. Logique métier

- Un patient ne peut voir/modifier que ses propres rendez-vous.
- Un rendez-vous ne peut être créé que si le créneau est disponible.
- Les statuts évoluent automatiquement (ex: passé → terminé).
- Les notifications sont générées pour les RDV à confirmer ou à venir.
- La timeline est générée dynamiquement selon les RDV, examens, traitements, etc.

## 6. Structure des requêtes et réponses JSON

### Exemple — Liste des rendez-vous

**GET /api/patient/appointments**
```
[
  {
    "id": "rdv1",
    "type": "consultation",
    "title": "Consultation Oncologie",
    "date": "2026-02-18T14:00:00Z",
    "status": "confirmé",
    "doctor": {
      "id": "doc1",
      "firstName": "Sophie",
      "lastName": "Martin",
      "specialty": "Oncologue",
      "structure": "Centre L.B."
    },
    "location": "Paris",
    "notes": []
  },
  ...
]
```

### Exemple — Timeline

**GET /api/patient/appointments/timeline**
```
[
  {
    "date": "2026-02-18",
    "type": "consultation",
    "label": "Consultation Dr. Martin",
    "status": "active"
  },
  {
    "date": "2026-03-03",
    "type": "traitement",
    "label": "Chimiothérapie",
    "status": "upcoming"
  }
]
```

## 7. Relations entre entités

- Un `Patient` a plusieurs `Appointment`
- Un `Appointment` est lié à un `Doctor`
- Un `TimelineEvent` est généré à partir des `Appointment`, `Exam`, `Treatment`, etc.

---

**Résumé** :  
Commencez par le backend des rendez-vous patients, car il structure tout le parcours de soins et la navigation du frontend. Cette base permettra ensuite d’étendre facilement vers les modules examens, traitements, documents, etc., tout en gardant une cohérence totale avec l’UX du frontend. 

Si vous souhaitez la même analyse pour une autre section, dites-le-moi !
