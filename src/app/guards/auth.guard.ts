import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/enums';

/** Redirige vers /public/login si non connecté */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn) {
    return true;
  }
  return router.createUrlTree(['/public/login']);
};

/** Redirige vers /patient/dashboard si connecté en tant que PATIENT */
export const patientGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn && auth.currentUser?.role === UserRole.PATIENT) {
    return true;
  }
  return router.createUrlTree(['/public/login']);
};

/** Redirige vers /doctor/dashboard si connecté en tant que DOCTOR */
export const doctorGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn && auth.currentUser?.role === UserRole.DOCTOR) {
    return true;
  }
  return router.createUrlTree(['/public/login']);
};

/**
 * Guard pour les pages publiques (login, register…) :
 * si déjà connecté, redirige vers le dashboard correspondant au rôle.
 */
export const publicGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn) {
    return true;
  }

  const role = auth.currentUser?.role;
  if (role === UserRole.PATIENT) {
    return router.createUrlTree(['/patient/dashboard']);
  }
  if (role === UserRole.DOCTOR) {
    return router.createUrlTree(['/doctor/dashboard']);
  }
  return true;
};
