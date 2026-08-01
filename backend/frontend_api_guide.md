# Guide Frontend : Appels REST et Routing Correct pour Backend Spring

## 1. Problème courant
- Ne jamais accéder aux endpoints REST comme ressources statiques (`<img src=...>`, `<script src=...>`).
- Toujours utiliser un client HTTP (Axios, fetch, HttpClient, Dio, etc.) pour interagir avec l’API.

---

## 2. Exemples d’appels corrects

### Angular
```typescript
// Récupérer les exceptions d’un médecin
this.http.get(`/api/doctors/${doctorId}/exceptions`).subscribe((exceptions) => {
  exceptions.forEach(e => {
    console.log(`${e.startDate} - ${e.reason || 'Blocked'}`);
  });
});
```

### React
```javascript
// Utilisation de fetch
fetch(`/api/doctors/${doctorId}/exceptions`)
  .then(res => res.json())
  .then(exceptions => {
    exceptions.forEach(e => {
      console.log(`${e.startDate} - ${e.reason || 'Blocked'}`);
    });
  });
```

### Flutter (Dio)
```dart
// Utilisation de Dio
final response = await dio.get('/api/doctors/$doctorId/exceptions');
for (var e in response.data) {
  print('${e['startDate']} - ${e['reason'] ?? 'Blocked'}');
}
```

---

## 3. Checklist pour éviter les erreurs
- [ ] Utiliser un client HTTP pour tous les endpoints REST.
- [ ] Vérifier que le proxy `/api` est configuré (dev server, nginx, etc.).
- [ ] Ne jamais utiliser `<img>` ou `<script>` pour accéder à `/api/...`.
- [ ] Les endpoints backend sont bien exposés via `@RestController` et `@GetMapping`, pas dans `/static` ou `/resources`.

---

## 4. Endpoints principaux

### Doctor
- `GET /api/doctors/{id}` : profil médecin
- `POST /api/doctors` : création
- `PUT /api/doctors/{id}` : update

### Availability
- `GET /api/doctors/{id}/availability` : grille hebdomadaire
- `POST /api/doctors/{id}/availability` : création
- `PUT /api/availability/{id}` : update
- `DELETE /api/availability/{id}` : suppression

### AvailabilityException
- `GET /api/doctors/{id}/exceptions` : liste exceptions
- `POST /api/doctors/{id}/exceptions` : création
- `PUT /api/exceptions/{id}` : update
- `DELETE /api/exceptions/{id}` : suppression

### Appointment
- `GET /api/appointments/{id}` : détails
- `POST /api/appointments` : création
- `PUT /api/appointments/{id}` : update
- `DELETE /api/appointments/{id}` : annulation

---

## 5. Résumé
- Toujours utiliser un client HTTP pour interagir avec l’API.
- Vérifier le proxy `/api`.
- Les endpoints REST ne sont pas des fichiers statiques.
- Afficher les données reçues, ne pas générer les exceptions côté UI.

---

**Pour toute question ou exemple spécifique, demandez !**

**Generated: March 18, 2026**
