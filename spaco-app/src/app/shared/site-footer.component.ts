import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantService } from '../core/api/tenant.service';

@Component({
  selector: 'app-site-footer',
  imports: [RouterLink],
  template: `
    @let b = tenant.branding();
    <footer id="contacto" class="foot on-dark">
      <div class="wrap">
        <div class="foot-grid">
          <div class="foot-brand">
            @if (b.logoMark) { <img class="foot-logo" [src]="b.logoMark" alt="" width="468" height="510"> }
            <span class="foot-miel">{{ b.wordmark ?? tenant.tenant()?.name }}</span>
            <span class="foot-sub">{{ b.subline }}</span>
            @if (b.tagline) { <p class="soft" style="margin-top:10px;max-width:360px;line-height:1.6">{{ b.tagline }}.</p> }
          </div>
          <div class="foot-col">
            <h2 class="foot-h">Visítanos</h2>
            <p>{{ b.address ?? tenant.tenant()?.address }}</p>
            @if (b.city) { <p>{{ b.city }}</p> }
            <p class="soft">{{ b.hours }}</p>
          </div>
          <div class="foot-col wide">
            <h2 class="foot-h">Escríbenos</h2>
            @if (b.email) { <p>{{ b.email }}</p> }
            @if (b.instagram) { <p>{{ b.instagram }}</p> }
            @if (tenant.whatsappLink(); as wa) {
              <a class="wa-chip" [href]="wa" target="_blank" rel="noopener">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 20l1.3-3.9A8 8 0 1 1 8 19z"></path></svg>WhatsApp <span class="num">{{ b.whatsapp }}</span>
              </a>
            } @else {
              <p class="wa-chip">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 20l1.3-3.9A8 8 0 1 1 8 19z"></path></svg>WhatsApp <span class="num">{{ b.whatsapp }}</span>
              </p>
            }
          </div>
        </div>
        <div class="foot-legal row">
          <span>© {{ year }} {{ tenant.tenant()?.name }}</span>
          <span class="spacer"></span>
          <a routerLink="/mi-historia" class="link">Mi historia clínica</a>
          <a routerLink="/panel" class="link">Equipo</a>
        </div>
      </div>
    </footer>
  `,
  styles: `:host { display: block; } .foot-legal a { color: var(--night-muted); }`
})
export class SiteFooterComponent {
  readonly tenant = inject(TenantService);
  readonly year = new Date().getFullYear();
}
