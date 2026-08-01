# Plan Front-End Complet pour l’équipe Backend

| Composant / Page         | Données manipulées                | Actions côté front-end                  | Interactions API backend                | Dynamique / Rafraîchissement |
|-------------------------|-----------------------------------|-----------------------------------------|-----------------------------------------|------------------------------|
| Connexion patient-médecin| Demandes de connexion (statut, patient, médecin) | - Patient : demande connexion à un médecin<br>- Médecin : liste des demandes en attente<br>- Médecin : approuver/refuser une demande | - POST /links/request<br>- GET /links/pending<br>- POST /links/approve<br>- POST /links/refuse | Rafraîchissement immédiat des listes après action |
| Rendez-vous             | Rendez-vous (date, heure, patient, médecin, statut) | - Formulaire création RDV<br>- Affichage des plages disponibles<br>- Validation disponibilité<br>- Lien RDV-patient-médecin | - GET /appointments/available<br>- POST /appointments/create<br>- GET /appointments/patient<br>- GET /appointments/doctor | Vérification disponibilité avant création, mise à jour dynamique |
| Gestion des liaisons    | Patients connectés, demandes en attente | - Affichage patients connectés<br>- Affichage demandes en attente<br>- Actions d’approbation/refus | - GET /links/connected<br>- GET /links/pending<br>- POST /links/approve<br>- POST /links/refuse | Mise à jour immédiate de l’interface |
| Notifications           | Messages d’erreur, confirmations   | - Affichage notification en cas d’erreur<br>- Confirmation d’action | - Réponses API (statut, message) | Affichage instantané, sans reload |

## Détail des interactions

- **Toutes les actions (création, validation, approbation/refus) sont faites via des appels API REST.**
- **Le front-end doit toujours vérifier la disponibilité d’une plage horaire avant de créer un rendez-vous.**
- **Après chaque action (création, validation, approbation/refus), le front doit rafraîchir dynamiquement les listes concernées (patients, demandes, rendez-vous) sans recharger la page.**
- **Les statuts (en attente, approuvé, refusé) doivent être clairement affichés et mis à jour en temps réel.**

## Exemple de flux pour création de rendez-vous

1. Patient ouvre le formulaire.
2. Front-end récupère les plages disponibles via GET /appointments/available.
3. Patient choisit une plage, front vérifie la disponibilité.
4. Si OK, POST /appointments/create avec patientId, doctorId, date, type, etc.
5. Après création, front rafraîchit la liste des rendez-vous.
6. Si erreur (plage non disponible), front affiche notification.

---

Ce plan peut être transmis directement à l’équipe backend pour garantir l’alignement des contrats API et la dynamique attendue côté front.
