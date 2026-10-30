import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { AvailabilityException, AvailabilityRule, Professional } from '../../../core/api/models';
import { longDate, todayIso } from '../../../shared/format';

const WEEKDAYS: { id: AvailabilityRule['weekday']; label: string }[] = [
  { id: 'MONDAY', label: 'Lunes' }, { id: 'TUESDAY', label: 'Martes' }, { id: 'WEDNESDAY', label: 'Miércoles' },
  { id: 'THURSDAY', label: 'Jueves' }, { id: 'FRIDAY', label: 'Viernes' }, { id: 'SATURDAY', label: 'Sábado' },
  { id: 'SUNDAY', label: 'Domingo' }
];

/** Weekly hours per professional, plus closures (holidays, vacations) and extra hours. */
@Component({
  selector: 'app-admin-schedule',
  imports: [FormsModule],
  template: `
    <main class="wrap page">
      <h1 class="page-h1">Horario</h1>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
      <div class="sheet cut"><div class="sheet-in cut">
        <div class="row">
          <h2 class="sheet-h2">Horas de atención</h2><span class="spacer"></span>
          <select class="ctl" style="width:auto" [ngModel]="professionalId()" (ngModelChange)="professionalId.set($event)" aria-label="Profesional">
            @for (p of professionals(); track p.id) { <option [ngValue]="p.id">{{ p.displayName }}</option> }
          </select>
        </div>
        <div class="scroll-x"><table class="tbl">
          <thead><tr><th>Día</th><th>Bloques</th></tr></thead>
          <tbody>
            @for (w of weekdays; track w.id) {
              <tr><td>{{ w.label }}</td><td class="row">
                @for (r of rulesOf(w.id); track r.id) {
                  <span class="badge" style="color:var(--navy)">{{ r.startTime.slice(0,5) }}–{{ r.endTime.slice(0,5) }}
                    <button type="button" class="btn-quiet" style="min-height:0;padding:0 0 0 6px" (click)="deleteRule(r)" [attr.aria-label]="'Quitar ' + r.startTime">✕</button></span>
                } @empty { <span class="meta">Cerrado</span> }
              </td></tr>
            }
          </tbody>
        </table></div>
        <form class="row" (ngSubmit)="addRule()">
          <select class="ctl" style="width:auto" name="wd" [(ngModel)]="rule.weekday" aria-label="Día">@for (w of weekdays; track w.id) { <option [value]="w.id">{{ w.label }}</option> }</select>
          <input class="ctl num" style="width:auto" type="time" name="rs" [(ngModel)]="rule.start" aria-label="Desde" />
          <input class="ctl num" style="width:auto" type="time" name="re" [(ngModel)]="rule.end" aria-label="Hasta" />
          <button type="submit" class="btn btn-ink btn-sm">Agregar bloque</button>
        </form>
      </div></div>

      <div class="sheet cut"><div class="sheet-in cut">
        <h2 class="sheet-h2">Cierres y horas extra</h2>
        <ul class="list">
          @for (x of exceptions(); track x.id) {
            <li><div class="item-top"><span>{{ longDate(x.date) }} · {{ x.type === 'BLOCK' ? 'Cerrado' : 'Hora extra' }} {{ x.startTime ? x.startTime.slice(0,5) + '–' + x.endTime!.slice(0,5) : 'todo el día' }}</span>
              <span class="meta">{{ proName(x.professionalId) }} {{ x.note ? '· ' + x.note : '' }} <button type="button" class="btn-quiet" (click)="deleteException(x)">Quitar</button></span></div></li>
          } @empty { <li class="meta">Sin cierres en los próximos 90 días.</li> }
        </ul>
        <form class="row" (ngSubmit)="addException()">
          <input class="ctl num" style="width:auto" type="date" name="xd" [(ngModel)]="ex.date" aria-label="Fecha" required />
          <select class="ctl" style="width:auto" name="xt" [(ngModel)]="ex.type" aria-label="Tipo"><option value="BLOCK">Cerrar</option><option value="EXTRA">Abrir extra</option></select>
          <input class="ctl num" style="width:auto" type="time" name="xs" [(ngModel)]="ex.start" aria-label="Desde (vacío = todo el día)" />
          <input class="ctl num" style="width:auto" type="time" name="xe" [(ngModel)]="ex.end" aria-label="Hasta" />
          <select class="ctl" style="width:auto" name="xp" [(ngModel)]="ex.professionalId" aria-label="Quién"><option [ngValue]="null">Todo el spa</option>@for (p of professionals(); track p.id) { <option [ngValue]="p.id">{{ p.displayName }}</option> }</select>
          <input class="ctl" style="width:auto" name="xn" placeholder="Nota (festivo, vacaciones…)" [(ngModel)]="ex.note" />
          <button type="submit" class="btn btn-ink btn-sm">Agregar</button>
        </form>
      </div></div>
    </main>
  `
})
export default class AdminScheduleComponent implements OnInit {
  private api = inject(ApiService);
  readonly professionals = signal<Professional[]>([]);
  readonly professionalId = signal<number | null>(null);
  readonly rules = signal<AvailabilityRule[]>([]);
  readonly exceptions = signal<AvailabilityException[]>([]);
  readonly error = signal<string | null>(null);
  readonly weekdays = WEEKDAYS;
  readonly mine = computed(() => this.rules().filter(r => r.professionalId === this.professionalId()));
  rule = { weekday: 'MONDAY' as AvailabilityRule['weekday'], start: '09:00', end: '12:00' };
  ex = { date: '', type: 'BLOCK' as 'BLOCK' | 'EXTRA', start: '', end: '', professionalId: null as number | null, note: '' };
  readonly longDate = longDate;

  ngOnInit() {
    this.api.staffProfessionals().subscribe(p => {
      this.professionals.set(p);
      this.professionalId.set(p[0]?.id ?? null);
    });
    this.load();
  }

  load() {
    this.api.rules().subscribe(r => this.rules.set(r));
    const from = todayIso();
    const [y, m, d] = from.split('-').map(Number);
    this.api.exceptions(from, new Date(Date.UTC(y, m - 1, d + 90)).toISOString().slice(0, 10)).subscribe(x => this.exceptions.set(x));
  }

  rulesOf(day: AvailabilityRule['weekday']) {
    return this.mine().filter(r => r.weekday === day).sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  proName(id: number | null): string {
    return id == null ? 'Todo el spa' : this.professionals().find(p => p.id === id)?.displayName ?? '';
  }

  addRule() {
    if (!this.professionalId()) return;
    this.run(this.api.saveRule({ professionalId: this.professionalId()!, weekday: this.rule.weekday, startTime: this.rule.start, endTime: this.rule.end, active: true }));
  }

  deleteRule(r: AvailabilityRule) {
    this.run(this.api.deleteRule(r.id));
  }

  addException() {
    const x = this.ex;
    this.run(this.api.addException({ professionalId: x.professionalId, date: x.date, startTime: x.start || null, endTime: x.end || null, type: x.type, note: x.note || null }));
  }

  deleteException(x: AvailabilityException) {
    this.run(this.api.deleteException(x.id));
  }

  private run(request: import('rxjs').Observable<unknown>) {
    this.error.set(null);
    request.subscribe({ next: () => this.load(), error: (e) => this.error.set(errorMessage(e)) });
  }
}
