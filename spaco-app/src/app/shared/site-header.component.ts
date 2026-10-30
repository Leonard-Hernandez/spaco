import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantService } from '../core/api/tenant.service';
import { BrandComponent } from './brand.component';

/** Masthead of the public site. back = a single "Volver al inicio" link instead of the nav. */
@Component({
  selector: 'app-site-header',
  imports: [RouterLink, BrandComponent],
  template: `
    <div class="wrap">
      <header class="masthead">
        <app-brand />
        @if (back()) {
          <a routerLink="/" class="navlink">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"></path></svg>Volver al inicio
          </a>
        } @else {
          <nav class="nav" aria-label="Principal">
            <a routerLink="/" fragment="catalogo" class="navlink nav-text">Tratamientos</a>
            <a routerLink="/" fragment="historias" class="navlink nav-text">Historias</a>
            <a routerLink="/mi-historia" class="navlink nav-text">Mi historia</a>
            @if (tenant.whatsappLink(); as wa) {
              <a [href]="wa" target="_blank" rel="noopener" class="navlink nav-wa">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 20l1.3-3.9A8 8 0 1 1 8 19z"></path><path d="M9 9.5c0 3 2.5 5.5 5.5 5.5l1-1.5-2-1-1 .8a4 4 0 0 1-1.8-1.8l.8-1-1-2z"></path></svg>WhatsApp
              </a>
            } @else {
              <a routerLink="/" fragment="contacto" class="navlink nav-wa">Contacto</a>
            }
            <a routerLink="/agendar" class="btn btn-ink nav-cta">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="1"></rect><path d="M8 3v4M16 3v4M3 10h18"></path></svg>Agendar cita
            </a>
          </nav>
        }
      </header>
    </div>
  `
})
export class SiteHeaderComponent {
  readonly tenant = inject(TenantService);
  readonly back = input(false);
}
