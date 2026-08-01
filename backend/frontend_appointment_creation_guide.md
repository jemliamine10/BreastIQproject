# Correction Frontend : Création de Rendez-vous Compatible Backend

## 1️⃣ Règles à respecter côté frontend
- Lors de la création d’un rendez-vous, le frontend doit toujours envoyer un objet JSON complet, avec tous les champs obligatoires :
  - `linkId` : UUID valide du lien patient-médecin
  - `startAt` : date/heure de début au format ISO UTC (ex : "2026-03-20T09:00:00Z")
  - `endAt` : date/heure de fin au format ISO UTC
  - `mode` : valeur de l’enum backend (`ONLINE`, `OFFLINE`, etc.)

---

## 2️⃣ Exemple de payload à envoyer
```json
{
  "linkId": "d1ea9210-884f-4533-ac93-56dc372fd4a9",
  "startAt": "2026-03-20T09:00:00Z",
  "endAt": "2026-03-20T09:30:00Z",
  "mode": "ONLINE"
}
```

---

## 3️⃣ Points de vigilance
- Ne jamais envoyer de champs null ou omis pour les champs annotés `@NotNull` côté backend.
- Respecter strictement les types : UUID pour `linkId`, string ISO UTC pour `startAt`/`endAt`, valeur d’enum pour `mode`.
- Vérifier que le frontend récupère bien les valeurs nécessaires (ex : linkId depuis la relation patient-médecin, mode depuis la sélection UI).

---

## 4️⃣ Résumé pour l’équipe frontend
- Construisez toujours l’objet `AppointmentCreateRequestDto` avec tous les champs requis.
- Validez côté UI que tous les champs sont renseignés avant d’envoyer la requête.
- Si un champ est manquant ou null, le backend rejettera la requête avec une erreur de validation.

---

**Generated: March 18, 2026**
