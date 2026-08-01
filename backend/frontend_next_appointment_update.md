# Mise à jour frontend — Endpoint /api/patient/appointments/next

## 1. Changement côté backend
- L’endpoint `/api/patient/appointments/next` ne lance plus d’exception si aucun rendez-vous n’est trouvé.
- Il retourne toujours HTTP 200.
- Deux formats de réponse possibles :

### Cas 1 : prochain rendez-vous existe
```json
{
  "nextAppointment": { /* objet PatientAppointment */ }
}
```

### Cas 2 : aucun rendez-vous
```json
{
  "message": "Aucun prochain rendez-vous"
}
```

---

## 2. Modèle TypeScript à ajouter

```typescript
export interface NextAppointmentResponse {
  nextAppointment: PatientAppointment | null;
}
```

---

## 3. Service Angular à adapter

- Adapter la méthode pour accepter les deux formats :

```typescript
getNextAppointment(patientId: string): Observable<NextAppointmentResponse | { message: string }> {
  return this.http.get<NextAppointmentResponse | { message: string }>(`${this.baseUrl}/next`, { params: new HttpParams().set('patientId', patientId) });
}
```

---

## 4. Composant Angular à adapter

- Logique d’affichage :
  - Si `response.nextAppointment` est défini → afficher le rendez-vous.
  - Sinon, afficher `response.message`.

---

## 5. Résumé
- Plus d’erreur 500 sur `/next`.
- Nouveau modèle `NextAppointmentResponse` à ajouter.
- Adapter le service et le composant pour gérer les deux cas de réponse.
- Le frontend peut afficher “Aucun rendez-vous prévu” sans erreur.
