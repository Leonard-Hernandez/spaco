import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantService } from '../core/api/tenant.service';

/** Logo mark (or the default honeycomb) + wordmark of the spa. */
@Component({
  selector: 'app-brand',
  imports: [RouterLink],
  template: `
    <a [routerLink]="link()" class="brand" [attr.aria-label]="(tenant.tenant()?.name ?? 'Inicio') + ', inicio'">
      @if (tenant.branding().logoMark; as mark) {
      <img class="brand-logo" [src]="mark" alt="" width="468" height="510">
      } @else {
      <svg viewBox="66 67 68 66" aria-hidden="true" focusable="false"><g fill="none" stroke="currentColor" stroke-width="2.6" stroke-linejoin="round"><polygon points="100,88 110.4,94 110.4,106 100,112 89.6,106 89.6,94" fill="currentColor"></polygon><polygon points="120.8,88 131.2,94 131.2,106 120.8,112 110.4,106 110.4,94"></polygon><polygon points="79.2,88 89.6,94 89.6,106 79.2,112 68.8,106 68.8,94"></polygon><polygon points="110.4,70 120.8,76 120.8,88 110.4,94 100,88 100,76"></polygon><polygon points="89.6,70 100,76 100,88 89.6,94 79.2,88 79.2,76"></polygon><polygon points="110.4,106 120.8,112 120.8,124 110.4,130 100,124 100,112"></polygon><polygon points="89.6,106 100,112 100,124 89.6,130 79.2,124 79.2,112"></polygon></g></svg>
      }
      <span class="brand-words">
        <span class="brand-miel">{{ tenant.branding().wordmark ?? tenant.tenant()?.name ?? '' }}</span>
        <span class="brand-sub">{{ tenant.branding().subline ?? '' }}</span>
      </span>
    </a>
  `,
  styles: `:host { display: contents; }`
})
export class BrandComponent {
  readonly tenant = inject(TenantService);
  readonly link = input<string>('/');
}
