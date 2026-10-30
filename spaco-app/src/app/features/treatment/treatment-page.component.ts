import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { Treatment } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { duration, formula, price } from '../../shared/format';
import { LabelComponent } from '../../shared/label.component';
import { SiteFooterComponent } from '../../shared/site-footer.component';
import { SiteHeaderComponent } from '../../shared/site-header.component';

/** One treatment: description, benefits, its "formula" table and contraindications. */
@Component({
  selector: 'app-treatment-page',
  imports: [RouterLink, LabelComponent, SiteHeaderComponent, SiteFooterComponent],
  template: `
    <div class="app">
      <app-site-header [back]="true" />
      <main class="wrap detail">
        @if (treatment(); as t) {
          <div class="detail-grid">
            <app-label size="hero" [ink]="ink()" [left]="'Fórmula N.º ' + formula(t.formulaNo)" [right]="t.categoryName">
              <div class="detail-body">
                <h1 class="hero-h1">{{ t.name }}</h1>
                @if (t.tagline) { <p class="hero-lede">{{ t.tagline }}</p> }
                @if (api.media(t.imageUrl); as img) { <img class="detail-photo" [src]="img" [alt]="t.name" /> }
                @if (t.description) { <p class="pre" style="margin:0;line-height:1.6">{{ t.description }}</p> }
                @if (t.benefits.length) {
                  <div>
                    <h2 class="sheet-h3">Beneficios</h2>
                    <ul class="benefits">@for (x of t.benefits; track $index) { <li>{{ x }}</li> }</ul>
                  </div>
                }
                <dl class="lab-dl">
                  <div class="leader"><dt class="tag">Duración</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ duration(t.durationMin) }}</dd></div>
                  <div class="leader"><dt class="tag">Precio</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ price(t.price) }}</dd></div>
                  @for (c of t.composition; track $index) {
                    <div class="leader"><dt class="tag">{{ c.label }}</dt><span class="dots" aria-hidden="true"></span><dd>{{ c.value }}</dd></div>
                  }
                </dl>
                @if (t.contraindications) {
                  <div class="note-box"><strong>Contraindicaciones:</strong> {{ t.contraindications }} Cuéntale a tu esteticista tus antecedentes de salud antes de la sesión.</div>
                }
              </div>
            </app-label>
            <aside class="aside">
              <div class="sheet cut"><div class="sheet-in cut">
                <h2 class="sheet-h2">¿Te lo regalas?</h2>
                <p style="margin:0">Elige fecha y hora en menos de un minuto. No necesitas crear una cuenta.</p>
                <a class="btn btn-ink btn-lg btn-block" routerLink="/agendar" [queryParams]="{ tratamiento: t.slug }">Agendar {{ t.name }}</a>
                @if (tenant.whatsappLink('Hola, quiero saber más sobre ' + t.name); as wa) {
                  <a class="btn btn-line btn-block" [href]="wa" target="_blank" rel="noopener">Preguntar por WhatsApp</a>
                }
              </div></div>
            </aside>
          </div>
        } @else if (notFound()) {
          <div class="cut empty"><p class="serif">Ese tratamiento ya no está en el catálogo</p><a class="btn btn-ink" routerLink="/" fragment="catalogo">Ver tratamientos</a></div>
        } @else {
          <p class="tag">Cargando…</p>
        }
      </main>
      <app-site-footer />
    </div>
  `
})
export default class TreatmentPageComponent implements OnInit {
  readonly api = inject(ApiService);
  readonly tenant = inject(TenantService);
  readonly slug = input.required<string>();
  readonly treatment = signal<Treatment | null>(null);
  readonly inkName = signal('honey');
  readonly notFound = signal(false);
  readonly ink = computed(() => this.inkName());

  readonly formula = formula;
  readonly duration = duration;
  readonly price = price;

  ngOnInit() {
    this.api.treatment(this.slug()).subscribe({
      next: (t) => {
        this.treatment.set(t);
        this.api.catalog().subscribe(c => this.inkName.set(c.categories.find(x => x.id === t.categoryId)?.ink ?? 'honey'));
      },
      error: () => this.notFound.set(true)
    });
  }
}
