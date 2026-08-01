# Erreur : NoResourceFoundException sur /api/treatment-management/patient/{patientId}/treatments

## Problème

L’erreur suivante apparaît côté frontend Angular lors de la récupération des traitements d’un patient :

```
NoResourceFoundException: No static resource api/treatment-management/patient/{patientId}/treatments
```

Cela signifie que l’URL appelée contient littéralement `{patientId}` au lieu d’un vrai identifiant patient, ou que l’URL ne correspond à aucun mapping backend.

## Cause

- L’URL `/api/treatment-management/patient/{patientId}/treatments` doit être appelée avec un vrai UUID patient (ex : `/api/treatment-management/patient/123e4567-e89b-12d3-a456-426614174000/treatments`).
- Si `{patientId}` reste dans l’URL, le backend ne trouve pas de mapping et tente de servir une ressource statique, d’où l’erreur.

## Solution

### 1. Côté Angular, injecter le vrai patientId dans l’URL :

```typescript
getPatientTreatments(patientId: string, doctorId: string, status?: string): Observable<Treatment[]> {
  let params = new HttpParams().set('doctorId', doctorId);
  if (status) params = params.set('status', status);
  return this.http.get<Treatment[]>(`/api/treatment-management/patient/${patientId}/treatments`, { params });
}
```

### 2. Utilisation :

```typescript
this.treatmentManagementService
  .getPatientTreatments(patientId, doctorId, 'ONGOING')
  .subscribe({
    next: data => this.treatments = data,
    error: err => console.error(err)
  });
```

### 3. Vérifications :
- Redémarrer le backend après modification.
- Vérifier que patientId et doctorId sont bien des UUID valides.
- Tester l’URL dans Postman avec de vrais IDs.

### 4. Backend concerné :
- Mapping : `/api/treatment-management/patient/{patientId}/treatments` (GET)
- Fichier : src/main/java/com/breastcancer/breastcancerbackend/controller/TreatmentManagementController.java

---

**Résumé :**
Toujours injecter le vrai patientId dans l’URL côté frontend. Ne jamais laisser `{patientId}` en texte brut. L’erreur NoResourceFoundException indique une URL mal formée ou non mappée côté backend.