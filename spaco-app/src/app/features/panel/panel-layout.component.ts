import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { BrandComponent } from '../../shared/brand.component';
import { StaffContext } from './staff.service';

type Access = 'checking' | 'granted' | 'denied' | 'error';

const ROLE_LABEL = { OWNER: 'Dueña', ESTHETICIAN: 'Esteticista', RECEPTION: 'Recepción' } as const;
const POLL_MS = 30_000;

@Component({
  selector: 'app-panel-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BrandComponent],
  providers: [StaffContext],
  template: `
    <div class="app">
      <div class="panel-top">
        <div class="wrap">
          <header class="masthead">
            <app-brand />
            <div class="row">
              @if (ctx.staff(); as s) {
                <span class="who"><b>{{ s.name ?? s.email }}</b><span>{{ roleLabel[s.role] }}</span></span>
              }
              <button type="button" class="btn btn-line btn-sm" (click)="auth.logout()">Salir</button>
            </div>
          </header>
          @if (access() === 'granted') {
            <nav class="panel-nav" aria-label="Panel">
              <a routerLink="solicitudes" routerLinkActive="active">Solicitudes
                @if (ctx.pendingCount()) { <span class="count-dot num">{{ ctx.pendingCount() }}</span> }
              </a>
              <a routerLink="agenda" routerLinkActive="active">Agenda</a>
              <a routerLink="pacientes" routerLinkActive="active">Pacientes</a>
              @if (ctx.clinical()) {
                <a routerLink="testimonios" routerLinkActive="active">Testimonios</a>
                <a routerLink="asistente" routerLinkActive="active">Asistente IA</a>
              }
              @if (ctx.isOwner()) {
                <a routerLink="tratamientos" routerLinkActive="active">Tratamientos</a>
                <a routerLink="horario" routerLinkActive="active">Horario</a>
                <a routerLink="equipo" routerLinkActive="active">Equipo</a>
                <a routerLink="autorizaciones" routerLinkActive="active">Textos legales</a>
              }
            </nav>
          }
        </div>
      </div>

      @switch (access()) {
        @case ('granted') { <router-outlet /> }
        @case ('denied') {
          <main class="wrap page" style="max-width:720px">
            <h1 class="page-h1">Esta cuenta no es del equipo</h1>
            <p>Entraste como {{ auth.displayName() }}. El panel es solo para el equipo del spa; pide a la dueña que invite tu correo.</p>
            <p>¿Eres paciente? Tu historia está en <a routerLink="/mi-historia" class="link">Mi historia</a>.</p>
            <div class="row"><button type="button" class="btn btn-ink" (click)="auth.logout()">Entrar con otra cuenta</button><a routerLink="/" class="btn btn-line">Ir al sitio</a></div>
          </main>
        }
        @case ('error') {
          <main class="wrap page" style="max-width:720px">
            <h1 class="page-h1">No pudimos abrir el panel</h1>
            <p>El servidor no respondió. Revisa tu conexión.</p>
            <button type="button" class="btn btn-ink" (click)="check()">Reintentar</button>
          </main>
        }
        @default { <main class="wrap page"><p class="tag">Abriendo el panel…</p></main> }
      }
    </div>
  `
})
export default class PanelLayoutComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly ctx = inject(StaffContext);
  private api = inject(ApiService);
  private destroyRef = inject(DestroyRef);

  readonly access = signal<Access>('checking');
  readonly roleLabel = ROLE_LABEL;

  ngOnInit() {
    this.check();
    const timer = setInterval(() => this.access() === 'granted' && this.refreshPending(), POLL_MS);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  check() {
    this.access.set('checking');
    this.api.staff().subscribe({
      next: (staff) => {
        this.ctx.staff.set(staff);
        this.access.set('granted');
        this.refreshPending();
      },
      error: (e) => this.access.set(e instanceof HttpErrorResponse && e.status === 403 ? 'denied' : 'error')
    });
  }

  private refreshPending() {
    this.api.pending().subscribe({ next: (p) => this.ctx.pendingCount.set(p.length), error: () => {} });
  }
}
