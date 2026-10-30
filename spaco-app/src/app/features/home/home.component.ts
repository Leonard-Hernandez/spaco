import { Component, computed, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { Category, PublicTestimonial, Treatment } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { duration, fold, formula, price, todayIso } from '../../shared/format';
import { inkOf, inkStyle } from '../../shared/ink';
import { LabelComponent } from '../../shared/label.component';
import { SiteFooterComponent } from '../../shared/site-footer.component';
import { SiteHeaderComponent } from '../../shared/site-header.component';

const PHOTO_KIND: Record<string, string> = { BEFORE: 'Antes', AFTER: 'Después', PROGRESS: 'Evolución' };

/** Landing: hero label with quick booking, the label shelf, stories, steps and contact. */
@Component({
  selector: 'app-home',
  imports: [FormsModule, RouterLink, LabelComponent, SiteHeaderComponent, SiteFooterComponent],
  template: `
    @let b = tenant.branding();
    <div class="app">
      <app-site-header />

      <section id="inicio" class="hero" aria-labelledby="hero-title">
        <div class="wrap hero-grid">
          <div class="hero-main">
            @if (hero(); as h) {
              <app-label size="hero" ink="navy" [left]="'Fórmula N.º ' + formula(h.formulaNo) + ' · ' + h.name" [right]="h.categoryName">
                <div class="hero-body">
                  <h1 class="hero-h1" id="hero-title">{{ b.tagline ?? tenant.tenant()?.name }}</h1>
                  @if (b.intro) { <p class="hero-lede">{{ b.intro }}</p> }
                  <div class="hero-meta">
                    <dl class="hero-dl">
                      <div class="leader"><dt class="tag">Duración</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ duration(quickTreatment()?.durationMin ?? h.durationMin) }}</dd></div>
                      <div class="leader"><dt class="tag">Precio</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ price(quickTreatment()?.price ?? h.price) }}</dd></div>
                    </dl>
                    <span class="tilt" aria-hidden="true"><span class="lot">LOTE<span class="num">{{ year }}·{{ formula((quickTreatment() ?? h).formulaNo) }}</span></span></span>
                  </div>
                  <form class="quick" aria-label="Reserva rápida" (ngSubmit)="quickGo()">
                    <div class="quick-row">
                      <label for="q-trat" class="label-caps">Tratamiento</label>
                      <select id="q-trat" name="tratamiento" class="ctl" [ngModel]="quickSlug()" (ngModelChange)="quickSlug.set($event)">
                        @for (t of treatments(); track t.id) { <option [value]="t.slug">{{ t.name }}</option> }
                      </select>
                    </div>
                    <div class="quick-pair">
                      <div class="field">
                        <label for="q-fecha" class="label-caps">Fecha</label>
                        <input id="q-fecha" name="fecha" type="date" class="ctl num" [min]="today" [(ngModel)]="quickDate" />
                      </div>
                      <div class="field">
                        <label for="q-hora" class="label-caps">Horario preferido</label>
                        <select id="q-hora" name="franja" class="ctl" [(ngModel)]="quickPart">
                          <option value="">Cualquier hora</option>
                          <option value="m">Mañana</option>
                          <option value="t">Tarde</option>
                        </select>
                      </div>
                    </div>
                    <div class="quick-go">
                      <span>Te mostramos los horarios libres para {{ quickTreatment()?.name ?? h.name }}.</span>
                      <button type="submit" class="btn btn-ink btn-lg">Ver disponibilidad
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"></path></svg>
                      </button>
                    </div>
                  </form>
                </div>
              </app-label>
            } @else if (loadError()) {
              <p class="error-msg">No pudimos cargar el catálogo. Recarga la página.</p>
            } @else {
              <p class="tag">Cargando…</p>
            }
          </div>

          <div class="hero-side">
            <div class="stack">
              @for (t of stack(); track t.id) {
                <button type="button" class="stack-btn" [attr.aria-pressed]="quickSlug() === t.slug" [attr.aria-label]="'Poner ' + t.name + ' en la reserva rápida'"
                        [attr.style]="inkStyle(inkFor(t))" (click)="quickSlug.set(t.slug)">
                  <span class="cut stack-face"><span class="cut ly bg-navy" style="--n:11px;padding:1px"><span class="cut ly bg-wax col" style="--n:10px">
                    <span class="band stack-band"><span class="num">N.º {{ formula(t.formulaNo) }}</span><span>{{ t.categoryName }}</span></span>
                    <span class="stack-body"><span class="stack-name">{{ t.name }}</span><span class="leader stack-meta"><span class="num">{{ duration(t.durationMin) }}</span><span class="dots" aria-hidden="true"></span><span class="num">{{ price(t.price) }}</span></span></span>
                  </span></span></span>
                </button>
              }
              <svg class="stack-stamp" viewBox="0 0 200 200" aria-hidden="true" focusable="false">
                <defs><path id="stampRing" d="M100,100 m-71,0 a71,71 0 1,1 142,0 a71,71 0 1,1 -142,0"></path></defs>
                <circle cx="100" cy="100" r="95" fill="none" stroke="currentColor" stroke-width="5"></circle>
                <circle cx="100" cy="100" r="88" fill="none" stroke="currentColor" stroke-width="1.5"></circle>
                <circle cx="100" cy="100" r="54" fill="none" stroke="currentColor" stroke-width="2.5"></circle>
                <text class="stamp-text" fill="currentColor"><textPath href="#stampRing" textLength="440" lengthAdjust="spacing">{{ stampText() }}</textPath></text>
                <g fill="none" stroke="currentColor" stroke-width="2.5" stroke-linejoin="round">
                  <polygon points="100,88 110.4,94 110.4,106 100,112 89.6,106 89.6,94" fill="currentColor"></polygon>
                  <polygon points="120.8,88 131.2,94 131.2,106 120.8,112 110.4,106 110.4,94"></polygon>
                  <polygon points="79.2,88 89.6,94 89.6,106 79.2,112 68.8,106 68.8,94"></polygon>
                  <polygon points="110.4,70 120.8,76 120.8,88 110.4,94 100,88 100,76"></polygon>
                  <polygon points="89.6,70 100,76 100,88 89.6,94 79.2,88 79.2,76" fill="currentColor" fill-opacity="0.35"></polygon>
                  <polygon points="110.4,106 120.8,112 120.8,124 110.4,130 100,124 100,112" fill="currentColor" fill-opacity="0.35"></polygon>
                  <polygon points="89.6,106 100,112 100,124 89.6,130 79.2,124 79.2,112"></polygon>
                </g>
              </svg>
            </div>
            <div class="cut info-card">
              <dl class="cut ly bg-wax col info-in" style="margin:0">
                <div class="leader"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"></circle><path d="M12 7v5l3 2"></path></svg><dt class="tag">Horario</dt><span class="dots" aria-hidden="true"></span><dd>{{ b.hours }}</dd></div>
                <div class="leader"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z"></path><circle cx="12" cy="9.5" r="2.5"></circle></svg><dt class="tag">Dirección</dt><span class="dots" aria-hidden="true"></span><dd>{{ b.address ?? tenant.tenant()?.address }}</dd></div>
                <div class="leader"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 20l1.3-3.9A8 8 0 1 1 8 19z"></path></svg><dt class="tag">WhatsApp</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ b.whatsapp }}</dd></div>
              </dl>
            </div>
          </div>
        </div>
      </section>

      <section id="catalogo" class="catalogue on-dark" aria-labelledby="cat-title">
        <div class="wrap">
          <div class="cat-head">
            <div>
              <h2 class="h2-big" id="cat-title">Nuestros tratamientos</h2>
              <p class="cat-lede">Filtra por tipo o busca por nombre. Cada etiqueta lleva impresa la duración y el precio.</p>
            </div>
            <div class="search">
              <label for="buscar" class="tag">Buscar tratamiento</label>
              <div class="search-box">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true" style="color:var(--navy)"><circle cx="11" cy="11" r="7"></circle><path d="M20 20l-3.5-3.5"></path></svg>
                <input id="buscar" type="search" class="ctl" placeholder="Ej. masaje, facial, miel…" autocomplete="off" [ngModel]="query()" (ngModelChange)="query.set($event)" />
              </div>
            </div>
          </div>
          <div class="filter-row">
            <div class="chips" role="group" aria-label="Filtrar por tipo de tratamiento">
              <button type="button" class="chip" [attr.aria-pressed]="cat() === null" (click)="cat.set(null)"><span class="swatch" aria-hidden="true" style="--sw:var(--wax)"></span>Todos</button>
              @for (c of categories(); track c.id) {
                <button type="button" class="chip" [attr.aria-pressed]="cat() === c.slug" (click)="cat.set(c.slug)">
                  <span class="swatch" aria-hidden="true" [attr.style]="'--sw:' + inkOf(c.ink).ink"></span>{{ c.name }}
                </button>
              }
            </div>
            <span class="count num" aria-live="polite">{{ shown().length === 1 ? '1 tratamiento' : shown().length + ' tratamientos' }}</span>
          </div>
          <div class="selmsg" aria-live="polite">
            @if (chosen(); as t) {
              <div class="selmsg-on">
                <p><strong>{{ t.name }}</strong> quedó en tu reserva rápida. Escoge fecha arriba o agenda de una vez.</p>
                <div class="selmsg-actions">
                  <button type="button" class="btn btn-sm btn-quiet-dark" (click)="toTop()">Ir a la reserva rápida</button>
                  <a class="btn btn-sm btn-gold" routerLink="/agendar" [queryParams]="{ tratamiento: t.slug }">Agendar</a>
                </div>
              </div>
            } @else {
              <p class="hint-dark">Toca «Elegir» en una etiqueta: la apartamos del estante y la escribimos en tu reserva rápida.</p>
            }
          </div>
          @if (shown().length) {
            <div class="shelf">
              @for (t of shown(); track t.id) {
                @let on = quickSlug() === t.slug && chosen() !== null;
                <article class="shelf-item wax" [class.is-dim]="chosen() !== null && !on" [class.is-lit]="on" [attr.aria-label]="'Fórmula N.º ' + formula(t.formulaNo) + ': ' + t.name">
                  <app-label [ink]="inkFor(t)" [left]="'Fórmula N.º ' + formula(t.formulaNo)" [right]="t.categoryName" style="flex:1">
                    <div class="lab-body">
                      <h3 class="lab-name"><a [routerLink]="['/tratamientos', t.slug]" style="text-decoration:none">{{ t.name }}</a></h3>
                      <p class="lab-desc">{{ t.tagline }}</p>
                      <dl class="lab-dl">
                        <div class="leader"><dt class="tag">Duración</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ duration(t.durationMin) }}</dd></div>
                        <div class="leader"><dt class="tag">Precio</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ price(t.price) }}</dd></div>
                      </dl>
                      <div class="lab-foot">
                        <span class="tilt-9" aria-hidden="true">
                          @if (on) { <span class="lot lot-sm lot-on stamp-land">ELEGIDA<span class="num">{{ year }}·{{ formula(t.formulaNo) }}</span></span> }
                          @else { <span class="lot lot-sm">LOTE<span class="num">{{ year }}·{{ formula(t.formulaNo) }}</span></span> }
                        </span>
                        <div class="lab-actions">
                          <button type="button" class="btn btn-sm" [class.btn-ink]="on" [class.btn-line]="!on" [attr.aria-pressed]="on" (click)="choose(t)">{{ on ? 'Elegida' : 'Elegir' }}</button>
                          <a class="btn btn-sm btn-ink" routerLink="/agendar" [queryParams]="{ tratamiento: t.slug }" [attr.aria-label]="'Agendar ' + t.name">Agendar
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"></path></svg>
                          </a>
                        </div>
                      </div>
                      <div class="lab-compact">
                        <span class="leader num"><span>{{ duration(t.durationMin) }}</span><span class="dots" aria-hidden="true"></span><span>{{ price(t.price) }}</span></span>
                        <a class="btn btn-sm btn-ink" routerLink="/agendar" [queryParams]="{ tratamiento: t.slug }" [attr.aria-label]="'Agendar ' + t.name">Agendar</a>
                      </div>
                    </div>
                  </app-label>
                </article>
              }
            </div>
          } @else if (treatments().length) {
            <div class="cut empty wax">
              <p class="serif">No encontramos tratamientos con esa búsqueda</p>
              <p>Prueba con otra palabra o mira todo el catálogo.</p>
              <button type="button" class="btn btn-ink" (click)="query.set(''); cat.set(null)">Ver todos los tratamientos</button>
            </div>
          }
        </div>
      </section>

      @if (stories().length) {
        <section id="historias" class="stories" aria-labelledby="stories-title">
          <div class="wrap">
            <div class="stories-head">
              <div style="display:flex;flex-direction:column;gap:12px">
                <h2 class="h2-big" id="stories-title">Historias de nuestras pacientes</h2>
                <p>Fotos reales de antes y después, publicadas con la autorización de cada paciente. Ella puede retirarlas cuando quiera.</p>
              </div>
              <div class="carousel-nav">
                <button type="button" class="icon-btn" aria-label="Historia anterior" (click)="scrollStories(-1)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"></path></svg>
                </button>
                <button type="button" class="icon-btn" aria-label="Historia siguiente" (click)="scrollStories(1)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"></path></svg>
                </button>
              </div>
            </div>
            <div class="carousel" #carousel tabindex="0" role="region" aria-label="Historias de antes y después">
              @for (s of stories(); track s.id) {
                <article class="cut story">
                  <div class="cut story-in">
                    <div class="band story-band"><span>{{ s.treatmentName ?? 'Historia' }}</span><span class="num">{{ s.photos.length }} fotos</span></div>
                    @if (s.photos.length) {
                      <div class="story-photos" [class.one]="s.photos.length === 1">
                        @for (p of s.photos.slice(0, 2); track $index) {
                          <figure class="story-photo">
                            <img [src]="api.media(p.url)" [alt]="photoKind[p.kind] + ' — ' + (s.treatmentName ?? '')" loading="lazy" />
                            <figcaption>{{ photoKind[p.kind] }}</figcaption>
                          </figure>
                        }
                      </div>
                    }
                    <div class="story-body">
                      @if (s.quote) { <p class="story-quote">“{{ s.quote }}”</p> }
                      <span class="story-who">{{ s.displayName }}</span>
                    </div>
                  </div>
                </article>
              }
            </div>
            <p class="stories-legal">Los resultados varían según cada persona. ¿Eres paciente? Puedes autorizar o retirar tus fotos desde <a routerLink="/mi-historia" class="link">tu historia</a>.</p>
          </div>
        </section>
      }

      <section id="como" class="how" aria-labelledby="how-title">
        <div class="wrap">
          <div class="how-head">
            <h2 class="h2-big" id="how-title">Tu cita en 3 pasos</h2>
            <a routerLink="/agendar" class="btn btn-ink btn-lg">Empezar a agendar
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"></path></svg>
            </a>
          </div>
          <ol class="how-list">
            <li><span class="how-n num" aria-hidden="true">1</span><div><h3>Elige tu tratamiento</h3><p>Explora el catálogo y selecciona lo que tu cuerpo necesita hoy.</p></div></li>
            <li><span class="how-n num" aria-hidden="true">2</span><div><h3>Escoge fecha y hora</h3><p>Mira los horarios disponibles y elige el que mejor te quede.</p></div></li>
            <li><span class="how-n num" aria-hidden="true">3</span><div><h3>Confirma y relájate</h3><p>Te confirmamos por WhatsApp. Después de tu visita puedes ver tu historia y tus sesiones en línea.</p></div></li>
          </ol>
        </div>
      </section>

      <app-site-footer />

      <div class="mbar">
        <div class="wrap">
          <a routerLink="/agendar" class="btn btn-ink btn-lg">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="1"></rect><path d="M8 3v4M16 3v4M3 10h18"></path></svg>Agendar cita
          </a>
        </div>
      </div>
    </div>
  `
})
export default class HomeComponent implements OnInit {
  readonly api = inject(ApiService);
  readonly tenant = inject(TenantService);
  private router = inject(Router);

  readonly categories = signal<Category[]>([]);
  readonly treatments = signal<Treatment[]>([]);
  readonly stories = signal<PublicTestimonial[]>([]);
  readonly loadError = signal(false);
  readonly query = signal('');
  readonly cat = signal<string | null>(null);
  readonly quickSlug = signal<string | null>(null);
  readonly chosen = signal<Treatment | null>(null);
  quickDate = '';
  quickPart = '';

  readonly today = todayIso();
  readonly year = new Date().getFullYear();
  readonly photoKind = PHOTO_KIND;
  readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');

  readonly hero = computed(() => {
    const list = this.treatments();
    return list.find(t => t.slug === this.tenant.branding().heroTreatment) ?? list[0] ?? null;
  });
  readonly quickTreatment = computed(() => this.treatments().find(t => t.slug === this.quickSlug()) ?? null);
  // One label per category (other than the hero's), as in the design
  readonly stack = computed(() => {
    const seen = new Set([this.hero()?.categoryId]);
    return this.treatments().filter(t => !seen.has(t.categoryId) && seen.add(t.categoryId)).slice(0, 3);
  });
  readonly shown = computed(() => {
    const q = fold(this.query().trim());
    return this.treatments().filter(t => (!this.cat() || t.category === this.cat())
      && (!q || fold(`${t.name} ${t.tagline ?? ''} ${t.categoryName}`).includes(q)));
  });
  readonly stampText = computed(() => {
    const b = this.tenant.branding();
    const words = `${(b.wordmark ?? '').toUpperCase()} PURA · ${(b.city ?? '').toUpperCase()} · `;
    return words + words;
  });

  readonly formula = formula;
  readonly duration = duration;
  readonly price = price;
  readonly inkStyle = inkStyle;
  readonly inkOf = inkOf;

  ngOnInit() {
    this.api.catalog().subscribe({
      next: (c) => {
        this.categories.set(c.categories);
        this.treatments.set(c.treatments);
        this.quickSlug.set(this.hero()?.slug ?? null);
      },
      error: () => this.loadError.set(true)
    });
    this.api.testimonials().subscribe({ next: (s) => this.stories.set(s), error: () => this.stories.set([]) });
  }

  inkFor(t: Treatment): string {
    return this.categories().find(c => c.id === t.categoryId)?.ink ?? 'honey';
  }

  choose(t: Treatment) {
    if (this.chosen()?.id === t.id) {
      this.chosen.set(null);
      return;
    }
    this.chosen.set(t);
    this.quickSlug.set(t.slug);
  }

  toTop() {
    document.getElementById('q-fecha')?.focus();
    document.getElementById('inicio')?.scrollIntoView({ block: 'start' });
  }

  quickGo() {
    const params: Record<string, string> = {};
    if (this.quickSlug()) params['tratamiento'] = this.quickSlug()!;
    if (this.quickDate) params['fecha'] = this.quickDate;
    if (this.quickPart) params['franja'] = this.quickPart;
    this.router.navigate(['/agendar'], { queryParams: params });
  }

  scrollStories(direction: number) {
    const el = this.carousel()?.nativeElement;
    el?.scrollBy({ left: direction * el.clientWidth * 0.8, behavior: 'smooth' });
  }
}
