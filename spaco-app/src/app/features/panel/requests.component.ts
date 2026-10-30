import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Appointment } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { longDay, phone, stamp, time } from '../../shared/format';
import { StaffContext } from './staff.service';

/** Booking requests from the website, oldest first. Accepting keeps the slot; rejecting frees it. */
@Component({
  selector: 'app-requests',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="wrap page">
      <h1 class="page-h1">Solicitudes</h1>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
      @if (loaded() && !items().length) {
        <p class="empty-note">No hay solicitudes por responder.</p>
      }
      @for (a of items(); track a.id) {
        <div class="sheet cut"><div class="sheet-in cut">
          <div class="item-top">
            <span class="item-title">{{ a.treatmentName }}</span>
            <span class="meta">Pedida {{ stamp(a.createdAt) }}</span>
          </div>
          <dl class="dl">
            <div class="leader"><dt class="tag">Cuándo</dt><span class="dots" aria-hidden="true"></span><dd>{{ longDay(a.startAt) }}, {{ time(a.startAt) }}</dd></div>
            <div class="leader"><dt class="tag">Con</dt><span class="dots" aria-hidden="true"></span><dd>{{ a.professionalName }}</dd></div>
            <div class="leader"><dt class="tag">Paciente</dt><span class="dots" aria-hidden="true"></span><dd><a class="link" [routerLink]="['/panel/pacientes', a.patientId]">{{ a.patientName }}</a> · <span class="num">{{ phone(a.patientPhone) }}</span></dd></div>
          </dl>
          @if (a.patientNote) { <div class="note-box"><strong>Nota de la paciente:</strong> {{ a.patientNote }}</div> }
          <div class="row">
            <button type="button" class="btn btn-ink" (click)="decide(a, 'accept')">Confirmar</button>
            @if (rejecting() === a.id) {
              <input class="ctl" style="max-width:320px" placeholder="Motivo (opcional)" [(ngModel)]="reason" name="reason" />
              <button type="button" class="btn btn-danger" (click)="decide(a, 'reject')">No aceptar</button>
              <button type="button" class="btn-quiet" (click)="rejecting.set(null)">Volver</button>
            } @else {
              <button type="button" class="btn btn-line" (click)="rejecting.set(a.id); reason = ''">No aceptar</button>
            }
            <span class="spacer"></span>
            @if (whatsapp(a); as wa) { <a class="btn btn-gold btn-sm" [href]="wa" target="_blank" rel="noopener">WhatsApp</a> }
          </div>
        </div></div>
      }
    </main>
  `
})
export default class RequestsComponent implements OnInit {
  private api = inject(ApiService);
  private tenant = inject(TenantService);
  private ctx = inject(StaffContext);

  readonly items = signal<Appointment[]>([]);
  readonly loaded = signal(false);
  readonly rejecting = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  reason = '';

  readonly longDay = longDay;
  readonly time = time;
  readonly stamp = stamp;
  readonly phone = phone;

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.pending().subscribe({
      next: (p) => { this.items.set(p); this.ctx.pendingCount.set(p.length); this.loaded.set(true); },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  decide(a: Appointment, action: 'accept' | 'reject') {
    this.error.set(null);
    this.api.decide(a.id, action, action === 'reject' ? this.reason.trim() || null : null).subscribe({
      next: () => { this.rejecting.set(null); this.load(); },
      error: (e) => { this.error.set(errorMessage(e)); this.load(); }
    });
  }

  /** wa.me message to the patient confirming her appointment. */
  whatsapp(a: Appointment): string | null {
    const digits = a.patientPhone.replace(/\D/g, '');
    const name = this.tenant.tenant()?.name ?? '';
    const text = `Hola ${a.patientName.split(' ')[0]}, te escribimos de ${name} sobre tu cita de ${a.treatmentName} el ${longDay(a.startAt)} a las ${time(a.startAt)}.`;
    return digits ? `https://wa.me/${digits}?text=${encodeURIComponent(text)}` : null;
  }
}
