import { Component, computed, effect, inject, input, OnInit, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Booking, Category, ConsentTemplate, Professional, Treatment } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { bogotaInstant, clock, duration, formula, longDate, longDay, monthTitle, price, time, todayIso } from '../../shared/format';
import { inkStyle } from '../../shared/ink';
import { LabelComponent } from '../../shared/label.component';
import { SiteHeaderComponent } from '../../shared/site-header.component';

const STEPS = ['Tratamiento', 'Fecha y hora', 'Tus datos', 'Confirmación'];
const DAYS_AHEAD = 60;

/**
 * Four-step booking without an account. Deep links: ?tratamiento=slug&fecha=2026-10-21&franja=m|t.
 * The request stays PENDING until the spa confirms it; the confirmation shows her booking link.
 */
@Component({
  selector: 'app-booking',
  imports: [FormsModule, RouterLink, LabelComponent, SiteHeaderComponent],
  template: `
    <div class="app is-booking">
      <app-site-header [back]="true" />
      <div class="wrap">
        <main class="bk">
          <div class="bk-head">
            <h1 class="bk-h1">Agenda tu cita</h1>
            <span class="bk-stepno num">Paso {{ step() }} de 4</span>
          </div>
          <ol class="steps" aria-label="Pasos de la reserva">
            @for (label of steps; track $index) {
              @let n = $index + 1;
              @let done = n < step() || step() === 4;
              <li class="step" [class.is-current]="n === step()" [class.is-done]="done" [class.has-line]="n < 4">
                <button type="button" class="step-btn" [disabled]="!(n < step()) || step() === 4" [attr.aria-current]="n === step() ? 'step' : null" (click)="step.set(n)">
                  <span class="seal-tilt" aria-hidden="true"><span class="step-seal num">
                    @if (done) { <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"></path></svg> } @else { {{ n }} }
                  </span></span>
                  <span class="step-label">{{ label }}</span>
                </button>
                @if (n < 4) { <span class="step-line" [class.is-past]="n < step()" aria-hidden="true"></span> }
              </li>
            }
          </ol>

          <div class="bk-grid">
            <div class="card-shadow">
              <div class="cut ly bg-navy bc1"><div class="cut ly bg-wax bc2"><div class="cut ly bg-navy bc3"><div class="cut ly bg-wax bc4">
                <form class="bk-form" aria-label="Formulario de reserva" novalidate (ngSubmit)="next()">
                  <div class="panel">
                    @switch (step()) {
                      @case (1) {
                        <div class="panel-head"><h2 class="panel-h2">¿Qué tratamiento quieres?</h2><p>Selecciona uno. Puedes cambiarlo antes de confirmar.</p></div>
                        <div class="opts" role="radiogroup" aria-label="Tratamientos">
                          @for (t of treatments(); track t.id) {
                            @let on = selected()?.id === t.id;
                            <button type="button" role="radio" class="opt" [class.is-on]="on" [attr.aria-checked]="on" [attr.style]="inkStyle(inkFor(t))" (click)="pick(t)">
                              <span class="cut opt-face"><span class="cut opt-body">
                                <span class="band opt-band"><span class="num">N.º {{ formula(t.formulaNo) }}</span><span>{{ t.categoryName }}</span></span>
                                <span class="opt-main"><span class="opt-name">{{ t.name }}</span><span class="opt-meta num">{{ duration(t.durationMin) }} · {{ price(t.price) }}</span>
                                  @if (on) { <span class="opt-check" aria-hidden="true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"></path></svg></span> }
                                </span>
                              </span></span>
                            </button>
                          }
                        </div>
                      }
                      @case (2) {
                        <div class="panel-head"><h2 class="panel-h2">Escoge fecha y hora</h2><p>{{ selected()?.name }} · {{ duration(selected()?.durationMin ?? 0) }}</p></div>
                        <div class="when">
                          <div class="cal">
                            <div class="cal-nav">
                              <button type="button" class="icon-btn" aria-label="Mes anterior" [disabled]="monthOffset() === 0" (click)="monthOffset.set(monthOffset() - 1)">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"></path></svg>
                              </button>
                              <span class="cal-month" aria-live="polite">{{ monthTitle(month().first) }}</span>
                              <button type="button" class="icon-btn" aria-label="Mes siguiente" [disabled]="monthOffset() >= 2" (click)="monthOffset.set(monthOffset() + 1)">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"></path></svg>
                              </button>
                            </div>
                            <div class="cal-box">
                              <div class="cal-dow" aria-hidden="true"><span>Lu</span><span>Ma</span><span>Mi</span><span>Ju</span><span>Vi</span><span>Sá</span><span>Do</span></div>
                              <div class="cal-days">
                                @for (blank of month().blanks; track $index) { <span class="cal-blank" aria-hidden="true"></span> }
                                @for (d of month().days; track d.iso) {
                                  <button type="button" class="day num" [disabled]="d.off" [attr.aria-pressed]="day() === d.iso" [attr.aria-label]="longDate(d.iso) + (d.off ? ', no disponible' : '')" (click)="chooseDay(d.iso)">{{ d.n }}</button>
                                }
                                @for (blank of month().tail; track $index) { <span class="cal-blank" aria-hidden="true"></span> }
                              </div>
                            </div>
                          </div>
                          <div class="times">
                            @if (!day()) {
                              <div class="times-empty">
                                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"></circle><path d="M12 7v5l3 2"></path></svg>
                                <span>Primero elige un día en el calendario para ver los horarios.</span>
                              </div>
                            } @else if (loadingSlots()) {
                              <div class="times-empty"><span>Buscando horarios libres…</span></div>
                            } @else if (!slots().length) {
                              <div class="times-empty"><span>No quedan horarios libres el {{ longDate(day()!) }}. Prueba con otro día{{ professionalId() ? ' u otra profesional' : '' }}.</span></div>
                            } @else {
                              @for (g of groups(); track g.key) {
                                @if (g.slots.length) {
                                  <div class="slot-group" role="group" [attr.aria-label]="'Horarios de la ' + g.title.toLowerCase()">
                                    <span class="slot-h">{{ g.title }}</span>
                                    <div class="slots">
                                      @for (s of g.slots; track s) {
                                        <button type="button" class="slot" [class.is-on]="slot() === s" [attr.aria-pressed]="slot() === s" (click)="slot.set(s)">
                                          <span class="stub stub-rim" aria-hidden="true"></span><span class="stub stub-fill" aria-hidden="true"></span><span class="stub-perf" aria-hidden="true"></span>
                                          <span class="slot-label num">{{ clock(s) }}</span>
                                        </button>
                                      }
                                    </div>
                                  </div>
                                }
                              }
                              <span class="note">Solo ves los horarios libres.</span>
                            }
                            @if (professionals().length > 1) {
                              <div class="field">
                                <label for="pro" class="label-sm">¿Con quién? (opcional)</label>
                                <select id="pro" name="profesional" class="ctl" [ngModel]="professionalId()" (ngModelChange)="chooseProfessional($event)">
                                  <option [ngValue]="null">Sin preferencia</option>
                                  @for (p of professionals(); track p.id) { <option [ngValue]="p.id">{{ p.displayName }}</option> }
                                </select>
                              </div>
                            }
                          </div>
                        </div>
                      }
                      @case (3) {
                        <div class="panel-head"><h2 class="panel-h2">Tus datos</h2><p>Para confirmarte la cita. No necesitas crear una cuenta.</p></div>
                        <div class="data-grid">
                          <div class="field span2">
                            <label for="nombre" class="label-sm">Nombre completo</label>
                            <input id="nombre" name="nombre" class="ctl" autocomplete="name" required [(ngModel)]="name" [attr.aria-invalid]="tried() && !name.trim()" />
                          </div>
                          <div class="field">
                            <label for="cel" class="label-sm">Celular (WhatsApp)</label>
                            <input id="cel" name="celular" class="ctl num" type="tel" inputmode="tel" autocomplete="tel" placeholder="300 123 4567" required [(ngModel)]="phone" [attr.aria-invalid]="tried() && !phoneOk()" />
                          </div>
                          <div class="field">
                            <label for="correo" class="label-sm">Correo (opcional)</label>
                            <input id="correo" name="correo" class="ctl" type="email" autocomplete="email" [(ngModel)]="email" [attr.aria-invalid]="tried() && !emailOk()" />
                            <span class="hint">Con tu correo de Google podrás ver tu historia en línea.</span>
                          </div>
                          <div class="field span2">
                            <label for="nota" class="label-sm">¿Algo que debamos saber? (opcional)</label>
                            <textarea id="nota" name="nota" class="ctl" rows="3" maxlength="500" placeholder="Alergias, embarazo, piel sensible…" [(ngModel)]="note"></textarea>
                          </div>
                          <!-- Honeypot: hidden from people, bots fill it in -->
                          <input type="text" name="website" tabindex="-1" autocomplete="off" aria-hidden="true" class="sr-only" [(ngModel)]="website" />
                          <div class="span2" style="display:flex;flex-direction:column;gap:8px">
                            <label class="check"><input type="checkbox" name="politica" [(ngModel)]="accepts" [attr.aria-invalid]="tried() && !accepts" />
                              <span>Autorizo el tratamiento de mis datos personales y de salud.
                                <button type="button" class="btn-quiet" style="min-height:0;padding:0" (click)="showPolicy.set(!showPolicy())">{{ showPolicy() ? 'Ocultar' : 'Leer' }} la autorización</button>
                              </span>
                            </label>
                            @if (showPolicy()) {
                              <div class="policy">{{ policy()?.body ?? 'Cargando…' }}</div>
                            }
                          </div>
                        </div>
                      }
                      @case (4) {
                        @if (booking(); as bk) {
                          <div class="done">
                            <span class="done-tilt stamp-land" aria-hidden="true"><span class="done-stamp"><span class="done-big">RECIBIDA</span><span class="done-small num">{{ formula(selected()?.formulaNo) }} · {{ time(bk.startAt) }}</span></span></span>
                            <h2 class="done-h2">¡Listo, {{ firstName() }}! Recibimos tu solicitud</h2>
                            <p>{{ bk.treatmentName }} el {{ longDay(bk.startAt) }} a las {{ time(bk.startAt) }} con {{ bk.professionalName }}. El spa te confirma por WhatsApp; guardamos el horario mientras tanto.</p>
                            <p class="hint">Guarda este enlace para ver o cancelar tu cita:</p>
                            <a class="link num" [routerLink]="['/cita', bk.token]">{{ origin }}/cita/{{ bk.token }}</a>
                            <div class="done-actions">
                              <a class="btn btn-ink" [routerLink]="['/cita', bk.token]">Ver mi cita</a>
                              <a class="btn btn-line" routerLink="/">Volver al inicio</a>
                            </div>
                          </div>
                        }
                      }
                    }
                    @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
                  </div>
                  @if (step() < 4) {
                    <div class="bfoot">
                      <div>
                        @if (step() > 1) {
                          <button type="button" class="btn btn-line" (click)="back()">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"></path></svg>Atrás
                          </button>
                        }
                      </div>
                      <div class="bfoot-r">
                        <span class="bk-hint" aria-live="polite">{{ hint() }}</span>
                        <button type="submit" class="btn btn-ink" [attr.aria-busy]="sending()" [disabled]="sending()">
                          {{ step() === 3 ? 'Confirmar cita' : 'Continuar' }}
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"></path></svg>
                        </button>
                      </div>
                    </div>
                  }
                </form>
              </div></div></div></div>
            </div>

            <aside class="aside" aria-label="Resumen de tu cita">
              <div class="card-shadow">
                <app-label size="summary" [ink]="selected() ? inkFor(selected()!) : 'navy'" [left]="'Fórmula N.º ' + formula(selected()?.formulaNo)" [right]="selected()?.categoryName ?? 'Sin llenar'" style="flex:1">
                  <div class="sum-body">
                    <h2 class="sum-h2">Resumen de tu cita</h2>
                    <dl class="sum-dl">
                      <div class="leader"><dt class="tag">Tratamiento</dt><span class="dots" aria-hidden="true"></span><dd [class.is-empty]="!selected()">{{ selected()?.name ?? 'Aún no eliges' }}</dd></div>
                      <div class="leader"><dt class="tag">Duración</dt><span class="dots" aria-hidden="true"></span><dd class="num" [class.is-empty]="!selected()">{{ selected() ? duration(selected()!.durationMin) : 'Por elegir' }}</dd></div>
                      <div class="leader"><dt class="tag">Fecha</dt><span class="dots" aria-hidden="true"></span><dd class="num" [class.is-empty]="!day()">{{ day() ? longDate(day()!) : 'Por elegir' }}</dd></div>
                      <div class="leader"><dt class="tag">Hora</dt><span class="dots" aria-hidden="true"></span><dd class="num" [class.is-empty]="!slot()">{{ slot() ? clock(slot()!) : 'Por elegir' }}</dd></div>
                      <div class="leader"><dt class="tag">Profesional</dt><span class="dots" aria-hidden="true"></span><dd>{{ professionalName() }}</dd></div>
                    </dl>
                    <div class="leader sum-total"><span class="tag">Total</span><span class="dots" aria-hidden="true"></span><span class="num">{{ selected() ? price(selected()!.price) : '—' }}</span></div>
                    @if (tenant.branding().cancellationNote) { <p class="sum-policy">{{ tenant.branding().cancellationNote }}</p> }
                  </div>
                </app-label>
              </div>
              <div class="cut help">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 20l1.3-3.9A8 8 0 1 1 8 19z"></path></svg>
                <span>¿Tienes dudas? Escríbenos por WhatsApp al <span class="num">{{ tenant.branding().whatsapp }}</span> y te ayudamos a elegir.</span>
              </div>
            </aside>
          </div>
        </main>
      </div>
    </div>
  `
})
export default class BookingComponent implements OnInit {
  private api = inject(ApiService);
  readonly tenant = inject(TenantService);

  // Deep link
  readonly tratamiento = input<string>();
  readonly fecha = input<string>();
  readonly franja = input<string>();

  readonly steps = STEPS;
  readonly step = signal(1);
  readonly treatments = signal<Treatment[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly professionals = signal<Professional[]>([]);
  readonly selected = signal<Treatment | null>(null);
  readonly monthOffset = signal(0);
  readonly day = signal<string | null>(null);
  readonly slots = signal<string[]>([]);
  readonly loadingSlots = signal(false);
  readonly slot = signal<string | null>(null);
  readonly professionalId = signal<number | null>(null);
  readonly policy = signal<ConsentTemplate | null>(null);
  readonly showPolicy = signal(false);
  readonly tried = signal(false);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly booking = signal<Booking | null>(null);
  name = '';
  phone = '';
  email = '';
  note = '';
  website = '';
  accepts = false;

  readonly origin = window.location.origin;
  readonly today = todayIso();

  readonly month = computed(() => {
    const [y, m] = this.today.split('-').map(Number);
    const first = new Date(Date.UTC(y, m - 1 + this.monthOffset(), 1));
    const firstIso = first.toISOString().slice(0, 10);
    const daysIn = new Date(Date.UTC(first.getUTCFullYear(), first.getUTCMonth() + 1, 0)).getUTCDate();
    const lead = (first.getUTCDay() + 6) % 7;
    const last = new Date(Date.UTC(y, m - 1, Number(this.today.slice(8)) + DAYS_AHEAD)).toISOString().slice(0, 10);
    const days = Array.from({ length: daysIn }, (_, i) => {
      const iso = new Date(Date.UTC(first.getUTCFullYear(), first.getUTCMonth(), i + 1)).toISOString().slice(0, 10);
      return { iso, n: i + 1, off: iso < this.today || iso > last };
    });
    return { first: firstIso, days, blanks: Array(lead), tail: Array((7 - (lead + daysIn) % 7) % 7) };
  });
  readonly groups = computed(() => {
    const morning = this.slots().filter(s => s < '12:00');
    const afternoon = this.slots().filter(s => s >= '12:00');
    const g = [{ key: 'm', title: 'Mañana', slots: morning }, { key: 't', title: 'Tarde', slots: afternoon }];
    return this.franja() === 't' ? g.reverse() : g;
  });
  readonly professionalName = computed(() =>
    this.professionals().find(p => p.id === this.professionalId())?.displayName ?? 'Sin preferencia');
  readonly hint = computed(() => {
    switch (this.step()) {
      case 1: return this.selected() ? '' : 'Elige un tratamiento';
      case 2: return !this.day() ? 'Elige un día' : !this.slot() ? 'Elige una hora' : '';
      default: return '';
    }
  });

  readonly formula = formula;
  readonly duration = duration;
  readonly price = price;
  readonly clock = clock;
  readonly time = time;
  readonly longDay = longDay;
  readonly longDate = longDate;
  readonly monthTitle = monthTitle;
  readonly inkStyle = inkStyle;

  constructor() {
    // Slots follow the treatment, day and professional
    effect(() => {
      const t = this.selected();
      const d = this.day();
      const p = this.professionalId();
      if (t && d) untracked(() => this.loadSlots(t.id, d, p));
    });
  }

  ngOnInit() {
    this.api.catalog().subscribe(c => {
      this.categories.set(c.categories);
      this.treatments.set(c.treatments);
      const wanted = c.treatments.find(t => t.slug === this.tratamiento());
      if (wanted) {
        this.selected.set(wanted);
        this.step.set(2);
      }
      const date = this.fecha();
      if (wanted && date && /^\d{4}-\d{2}-\d{2}$/.test(date) && date >= this.today) {
        this.day.set(date);
        const [y, m] = this.today.split('-').map(Number);
        const [dy, dm] = date.split('-').map(Number);
        this.monthOffset.set(Math.min(2, Math.max(0, (dy - y) * 12 + dm - m)));
      }
    });
    this.api.professionals().subscribe(p => this.professionals.set(p));
    this.api.consentText('DATA_PROCESSING').subscribe({ next: (p) => this.policy.set(p), error: () => {} });
  }

  inkFor(t: Treatment): string {
    return this.categories().find(c => c.id === t.categoryId)?.ink ?? 'honey';
  }

  pick(t: Treatment) {
    this.selected.set(t);
    this.slot.set(null);
  }

  chooseDay(iso: string) {
    this.day.set(iso);
    this.slot.set(null);
  }

  chooseProfessional(id: number | null) {
    this.professionalId.set(id);
    this.slot.set(null);
  }

  phoneOk = () => this.phone.replace(/\D/g, '').length >= 10;
  emailOk = () => !this.email.trim() || /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.email.trim());
  firstName = () => this.name.trim().split(/\s+/)[0] ?? '';

  back() {
    this.error.set(null);
    this.step.set(Math.max(1, this.step() - 1));
  }

  next() {
    this.error.set(null);
    if (this.step() === 1 && this.selected()) this.step.set(2);
    else if (this.step() === 2 && this.day() && this.slot()) this.step.set(3);
    else if (this.step() === 3) this.submit();
  }

  private submit() {
    this.tried.set(true);
    if (!this.name.trim() || !this.phoneOk() || !this.emailOk()) {
      this.error.set('Revisa tu nombre, celular y correo.');
      return;
    }
    if (!this.accepts) {
      this.error.set('Para agendar necesitamos tu autorización de tratamiento de datos.');
      return;
    }
    this.sending.set(true);
    this.api.book({
      treatmentId: this.selected()!.id,
      professionalId: this.professionalId(),
      startAt: bogotaInstant(this.day()!, this.slot()!),
      fullName: this.name.trim(),
      phone: this.phone,
      email: this.email.trim() || null,
      note: this.note.trim() || null,
      acceptsDataPolicy: this.accepts,
      website: this.website
    }).subscribe({
      next: (b) => {
        this.sending.set(false);
        this.booking.set(b);
        this.step.set(4);
      },
      error: (e) => {
        this.sending.set(false);
        this.error.set(errorMessage(e));
        if ((e?.error?.code === 'SLOT_TAKEN' || e?.error?.code === 'TOO_SOON') && this.selected() && this.day()) {
          this.slot.set(null);
          this.step.set(2);
          this.loadSlots(this.selected()!.id, this.day()!, this.professionalId());
        }
      }
    });
  }

  private loadSlots(treatmentId: number, date: string, professionalId: number | null) {
    this.loadingSlots.set(true);
    this.api.availability(treatmentId, date, professionalId).subscribe({
      next: (r) => {
        this.slots.set(r.slots.map(s => s.slice(0, 5)));
        this.loadingSlots.set(false);
      },
      error: () => {
        this.slots.set([]);
        this.loadingSlots.set(false);
      }
    });
  }
}
