import { inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ApiService } from './api.service';
import { Branding, Tenant } from './models';

/** Theme tokens a spa may override from its branding (see src/themes). */
const THEME_TOKENS = ['amber', 'navy', 'navy-hover', 'wax', 'wax-hover', 'field', 'gold-soft', 'gold', 'brown',
  'honey', 'lavender', 'sage', 'clay'];

/** The spa this site belongs to (name, contact, branding). */
@Injectable({ providedIn: 'root' })
export class TenantService {

  private api = inject(ApiService);
  private title = inject(Title);

  readonly tenant = signal<Tenant | null>(null);

  load() {
    this.api.tenant().subscribe({
      next: (tenant) => {
        this.tenant.set(tenant);
        this.title.setTitle(tenant.name);
        this.applyTheme(tenant.branding.theme);
      },
      error: () => this.tenant.set(null)
    });
  }

  branding(): Branding {
    return this.tenant()?.branding ?? {};
  }

  /** wa.me link to the spa's WhatsApp (null while it is a placeholder), optionally prefilled. */
  whatsappLink(text?: string): string | null {
    const digits = (this.tenant()?.contactPhone ?? this.branding().whatsapp ?? '').replace(/\D/g, '');
    if (digits.length < 10) return null;
    const phone = digits.length === 10 && digits.startsWith('3') ? `57${digits}` : digits;
    return `https://wa.me/${phone}${text ? `?text=${encodeURIComponent(text)}` : ''}`;
  }

  // The theme file is the base; branding.theme lets a spa adjust colors without a build.
  private applyTheme(theme: Record<string, string> | undefined) {
    if (!theme) return;
    const root = document.documentElement.style;
    for (const [token, value] of Object.entries(theme)) {
      if (THEME_TOKENS.includes(token) && /^#[0-9a-f]{3,8}$/i.test(value)) {
        root.setProperty(`--${token}`, value);
      }
    }
  }
}
