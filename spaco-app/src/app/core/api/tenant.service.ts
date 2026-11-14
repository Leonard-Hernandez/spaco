import { computed, inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ApiService } from './api.service';
import { openingHoursLines } from '../../shared/opening-hours';
import { Branding, Tenant } from './models';

/** Theme tokens a spa may override from its branding (see src/themes). */
const THEME_TOKENS = ['amber', 'navy', 'navy-hover', 'wax', 'wax-hover', 'field', 'gold-soft', 'gold', 'brown',
  'honey', 'lavender', 'sage', 'clay'];

/**
 * Only same-origin paths ("/assets/..."): a third-party logo URL would make every visitor, staff
 * included, leak their IP and referrer to that host. "//" and "/\" are protocol-relative in browsers.
 */
function sameOriginPath(url: string | undefined): string | undefined {
  return url && /^\/(?![\/\\])/.test(url) ? url : undefined;
}

/** A profile value worth showing: not empty and not a seed placeholder such as "[DIRECCIÓN]". */
export function realValue(value: string | null | undefined): string | null {
  const v = value?.trim();
  return v && !/^\[.*\]$/.test(v) ? v : null;
}

/** The spa this site belongs to (name, contact, branding). */
@Injectable({ providedIn: 'root' })
export class TenantService {

  private api = inject(ApiService);
  private title = inject(Title);

  readonly tenant = signal<Tenant | null>(null);
  /** True once the tenant request settled (even on error), so the UI knows a missing logo is final. */
  readonly loaded = signal(false);

  /** Public contact details, each null while empty or still a placeholder. */
  readonly contact = computed(() => {
    const t = this.tenant();
    const b = t?.branding ?? {};
    return {
      address: realValue(b.address) ?? realValue(t?.address),
      city: realValue(b.city),
      email: realValue(b.email),
      instagram: realValue(b.instagram),
      phone: realValue(t?.contactPhone) ?? realValue(b.whatsapp),
      hours: openingHoursLines(t?.openingHours ?? [])
    };
  });

  load() {
    this.api.tenant().subscribe({
      next: (tenant) => {
        this.tenant.set({ ...tenant, branding: { ...tenant.branding, logoMark: sameOriginPath(tenant.branding.logoMark) } });
        this.title.setTitle(tenant.name);
        this.applyTheme(tenant.branding.theme);
        this.loaded.set(true);
      },
      error: () => {
        this.tenant.set(null);
        this.loaded.set(true);
      }
    });
  }

  branding(): Branding {
    return this.tenant()?.branding ?? {};
  }

  /** wa.me link to the spa's WhatsApp (null while it is a placeholder), optionally prefilled. */
  whatsappLink(text?: string): string | null {
    const digits = (this.contact().phone ?? '').replace(/\D/g, '');
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
