import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Appointment, Professional } from '../../core/api/models';
import { bogotaInstant, longDate, phone, STATUS_LABEL, time, todayIso } from '../../shared/format';
import { actionsFor } from './appointment-actions';
import { StaffContext } from './staff.service';

/** Day by day agenda (7 days from the chosen date), optionally of one professional. */
@Component({
  selector: 'app-agenda',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="wrap page">
      <div class="row">
        <h1 class="page-h1">Agenda</h1>
        <span class="spacer"></span>
        <button type="button" class="icon-btn" aria-label="Semana anterior" (click)="shift(-7)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"></path></svg>
        </button>
        <input type="date" class="ctl num" style="width:auto" [ngModel]="from()" (ngModelChange)="from.set($event); load()" aria-label="Desde" />
        <button type="button" class="icon-btn" aria-label="Semana siguiente" (click)="shift(7)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"></path></svg>
        </button>
        <button type="button" class="btn btn-line btn-sm" (click)="from.set(today); load()">Hoy</button>
        <select class="ctl" style="width:auto" [ngModel]="professionalId()" (ngModelChange)="professionalId.set($event); load()" aria-label="Profesional">
          <option [ngValue]="null">Todas</option>
          @for (p of professionals(); track p.id) { <option [ngValue]="p.id">{{ p.displayName }}</option> }
        </select>
      </div>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
      @for (d of days(); track d.date) {
        <section>
          <div class="day-head">{{ longDate(d.date) }}</div>
          @if (d.items.length) {
            <div class="sheet cut"><div class="sheet-in cut" style="padding-block:10px">
              <ul class="list">
                @for (a of d.items; track a.id) {
                  @let can = actions(a);
                  <li>
                    <div class="item-top">
                      <span><span class="num tag">{{ time(a.startAt) }}</span> · <a class="link" [routerLink]="['/panel/pacientes', a.patientId]">{{ a.patientName }}</a></span>
                      <span class="badge" [attr.data-s]="a.status">{{ status[a.status] }}</span>
                    </div>
                    <span class="meta">{{ a.treatmentName }} · {{ a.professionalName }} · <span class="num">{{ phone(a.patientPhone) }}</span></span>
                    @if (a.patientNote) { <span class="meta">Nota: {{ a.patientNote }}</span> }
                    <div class="row">
                      @if (can.accept) { <button type="button" class="btn btn-sm btn-ink" (click)="decide(a, 'accept')">Confirmar</button> }
                      @if (can.complete && ctx.clinical()) {
                        <button type="button" class="btn btn-sm btn-line" (click)="attend(a)">Atender y anotar</button>
                      } @else if (can.complete) {
                        <button type="button" class="btn btn-sm btn-line" (click)="decide(a, 'complete')">Marcar atendida</button>
                      }
                      @if (can.noShow) { <button type="button" class="btn btn-sm btn-line" (click)="decide(a, 'no-show')">No asistió</button> }
                      @if (can.cancel) { <button type="button" class="btn-quiet" (click)="decide(a, 'cancel')">Cancelar</button> }
                      @if (can.reject) { <button type="button" class="btn-quiet" (click)="decide(a, 'reject')">No aceptar</button> }
                    </div>
                  </li>
                }
              </ul>
            </div></div>
          } @else { <p class="meta" style="margin:4px 0 0">Sin citas.</p> }
        </section>
      }
    </main>
  `
})
export default class AgendaComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  readonly ctx = inject(StaffContext);

  readonly today = todayIso();
  readonly from = signal(this.today);
  readonly professionalId = signal<number | null>(null);
  readonly professionals = signal<Professional[]>([]);
  readonly items = signal<Appointment[]>([]);
  readonly error = signal<string | null>(null);

  readonly days = computed(() => Array.from({ length: 7 }, (_, i) => {
    const date = addDays(this.from(), i);
    return { date, items: this.items().filter(a => localDate(a.startAt) === date) };
  }));

  readonly status = STATUS_LABEL;
  readonly time = time;
  readonly longDate = longDate;
  readonly phone = phone;
  readonly actions = (a: Appointment) => actionsFor(a);

  ngOnInit() {
    this.api.staffProfessionals().subscribe(p => this.professionals.set(p));
    this.load();
  }

  shift(days: number) {
    this.from.set(addDays(this.from(), days));
    this.load();
  }

  load() {
    const from = bogotaInstant(this.from(), '00:00');
    const to = bogotaInstant(addDays(this.from(), 7), '00:00');
    this.api.appointments(from, to, null, this.professionalId()).subscribe({
      next: (a) => this.items.set(a),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  decide(a: Appointment, action: 'accept' | 'reject' | 'cancel' | 'complete' | 'no-show') {
    this.error.set(null);
    this.api.decide(a.id, action).subscribe({ next: () => this.load(), error: (e) => this.error.set(errorMessage(e)) });
  }

  /** Opens the patient's record with a session note prefilled for this visit. */
  attend(a: Appointment) {
    this.router.navigate(['/panel/pacientes', a.patientId], { queryParams: { cita: a.id } });
  }
}

function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

function localDate(instant: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date(instant));
}
