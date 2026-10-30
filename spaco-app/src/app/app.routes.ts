import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component') },
  { path: 'tratamientos/:slug', loadComponent: () => import('./features/treatment/treatment-page.component') },
  { path: 'agendar', loadComponent: () => import('./features/booking/booking.component') },
  { path: 'cita/:token', loadComponent: () => import('./features/appointment/booking-link.component') },
  { path: 'mi-historia', canActivate: [authGuard], loadComponent: () => import('./features/portal/portal.component') },
  { path: 'auth/callback', loadComponent: () => import('./features/auth/auth-callback.component') },
  { path: 'panel', canActivate: [authGuard], loadChildren: () => import('./features/panel/panel.routes') },
  { path: '**', redirectTo: '' }
];
