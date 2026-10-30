import { Routes } from '@angular/router';
import PanelLayoutComponent from './panel-layout.component';

export default [
  {
    path: '',
    component: PanelLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'solicitudes' },
      { path: 'solicitudes', loadComponent: () => import('./requests.component') },
      { path: 'agenda', loadComponent: () => import('./agenda.component') },
      { path: 'pacientes', loadComponent: () => import('./patients.component') },
      { path: 'pacientes/:id', loadComponent: () => import('./patient.component') },
      { path: 'testimonios', loadComponent: () => import('./testimonials.component') },
      { path: 'asistente', loadComponent: () => import('./assistant.component') },
      { path: 'tratamientos', loadComponent: () => import('./admin/admin-treatments.component') },
      { path: 'horario', loadComponent: () => import('./admin/admin-schedule.component') },
      { path: 'equipo', loadComponent: () => import('./admin/admin-team.component') },
      { path: 'autorizaciones', loadComponent: () => import('./admin/admin-consents.component') }
    ]
  }
] satisfies Routes;
