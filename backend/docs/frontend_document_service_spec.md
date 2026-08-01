# Spécification Technique : Gestion des Documents Médicaux

Ce document récapitule l'intégralité du flux de gestion des documents médicaux pour le développement des interfaces **Patient** et **Médecin**.

---

## 1. Modèles de Données (DTOs)

Les DTOs sont conçus pour correspondre aux composants UI Angular existants et futurs.

### `DocumentResponseDto`
Utilisé pour l'affichage des listes et des détails de documents.
*   **Utilité** : Représentation standard d'un document côté Frontend.
*   **Structure** :
    ```json
    {
      "id": "uuid-string",
      "name": "Bilan sanguin Mars 2026",
      "category": "bilan",     // Slugs: compte-rendu, ordonnance, bilan, imagerie, autre
      "date": "2026-03-24",    // Format ISO yyyy-MM-dd
      "doctor": "Dr. Jean Dupont", // Nom complet du médecin (ou "—" si non renseigné)
      "size": "1.2 Mo",        // Taille formatée (Ko/Mo)
      "pages": 3,
      "status": "pending"      // validated, pending, archived
    }
    ```

### `DocumentUploadDto`
Utilisé pour envoyer les métadonnées lors d'un upload.
*   **Utilité** : Partie JSON de la requête multipart (sous la clé `metadata`).
*   **Structure** :
    ```json
    {
      "name": "Nom du document",
      "category": "compte-rendu",
      "pageCount": 2
    }
    ```

### `DocumentEventDto`
Utilisé pour la synchronisation temps réel via WebSocket.
*   **Utilité** : Notifier le frontend d'une modification système.
*   **Structure** :
    ```json
    {
      "type": "DOCUMENT_ADDED", // DOCUMENT_ADDED, DOCUMENT_SHARED, DOCUMENT_DELETED, DOCUMENT_UPDATED
      "document": { ... DocumentResponseDto ... }
    }
    ```

---

## 2. API REST (Endpoints)

Tous les endpoints sont préfixés par `/api/v1`.

### Interface PATIENT
| Méthode | URL | Paramètres | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/patient/{patientId}/documents` | `page`, `size` | Liste paginée des documents de la patiente. |
| `GET` | `/patient/{patientId}/documents/counts` | - | Compteurs par catégorie (ex: `{"bilan": 5, ...}`). |
| `POST` | `/patient/{patientId}/documents/upload` | `file` (File), `metadata` (JSON) | Upload multipart. Par défaut, non visible par le médecin (partage requis). |
| `POST` | `/patient/{patientId}/documents/{id}/share` | `doctorId` (Query) | Partage un document avec un médecin spécifique. |
| `GET` | `/documents/{id}/download` | - | Téléchargement du fichier binaire. |
| `DELETE` | `/documents/{id}` | `requesterId` (Query) | Suppression logique et suppression physique du fichier. |

### Interface MÉDECIN
| Méthode | URL | Paramètres | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/doctor/{doctorId}/patients/{patientId}/documents` | `page`, `size` | Documents du patient autorisés pour ce médecin. |
| `GET` | `/doctor/{doctorId}/patients/{patientId}/documents/counts` | - | Compteurs des documents visibles par le médecin. |
| `POST` | `/doctor/{doctorId}/patients/{patientId}/documents/upload` | `file` (File), `metadata` (JSON) | Upload pour le patient. Automatiquement partagé avec le médecin. |
| `PATCH` | `/doctor/{doctorId}/documents/{id}/status` | `{"status": "..."}` | Met à jour le statut (ex: `validated`). |

---

## 3. Temps Réel (WebSockets / STOMP)

L'application utilise SockJS + STOMP pour la mise à jour en direct de l'UI.

*   **Endpoint de connexion** : `/ws`
*   **Broker** : `/topic`

### Topics à souscrire :
1.  **Côté Patient** : `/topic/patient/{patientId}`
    *   Reçoit : `DocumentEventDto`
    *   Événements : `DOCUMENT_ADDED`, `DOCUMENT_SHARED`, `DOCUMENT_DELETED`, `DOCUMENT_UPDATED`.
    *   *Usage* : Rafraîchir la liste ou afficher un toast de succès.
2.  **Côté Médecin** : `/topic/doctor/{doctorId}`
    *   Reçoit : `DocumentEventDto`
    *   Événement : `DOCUMENT_SHARED`.
    *   *Usage* : Notifier le médecin qu'un nouveau document est disponible pour consultation.

---

## 4. Flux Documentaire Global

1.  **Upload** :
    *   Tout document est initialement en statut `pending`.
    *   Si le patient upload : seul lui voit le document jusqu'à ce qu'il clique sur "Partager".
    *   Si le médecin upload : le document est immédiatement visible par les deux parties.
2.  **Lecture** : Les listes sont paginées. Le frontend doit gérer la pagination via les paramètres `page` et `size`.
3.  **Partage** : Action explicite du patient. Déclenche un événement WebSocket vers le médecin concerné.
4.  **Action Médicale** : Le médecin peut valider ou archiver un document via le `PATCH` de statut. Le patient reçoit une mise à jour temps réel.
5.  **Suppression** : Un document supprimé disparait des listes et son fichier binaire est effacé du serveur.

---

## 5. Règles de Sécurité et Accès

*   **Vérification du Lien Actif** : Pour toutes les opérations impliquant un médecin et un patient, le système vérifie qu'un lien `ACTIVE` existe entre eux via le `DoctorPatientLinkGuardService`. Si le lien est `PENDING` ou rompu, l'accès est refusé (403 Forbidden).
*   **Propriété** : Le patient ne peut supprimer que ses propres documents.
*   **Visibilité** :
    *   `visibleToPatient` : Toujours vrai pour les documents médicaux.
    *   `visibleToDoctor` : Vrai si uploadé par un médecin ou partagé par le patient.
*   **Stockage** : Les fichiers sont renommés avec un UUID unique côté serveur pour éviter les collisions et sécuriser l'accès direct.
