import { Component, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Booking } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { longDay, STATUS_LABEL, time } from '../../shared/format';
import { SiteHeaderComponent } from '../../shared/site-header.component';

/** The patient's booking link: see the status, cancel before it starts. */
@Component({
  selector: 'app-booking-link',
  imports: [RouterLink, SiteHeaderComponent],
  template: `
    <div class="app">
      <app-site-header [back]="true" />
      <main class="wrap page" style="max-width:820px">
        <h1 class="page-h1">Tu cita</h1>
        @if (booking(); as b) {
          <div class="sheet cut"><div class="sheet-in cut">
            <div class="item-top"><h2 class="sheet-h2">{{ b.treatmentName }}</h2><span class="badge" [attr.data-s]="b.status">{{ status[b.status] }}</span></div>
            <dl class="dl">
              <div class="leader"><dt class="tag">Fecha</dt><span class="dots" aria-hidden="true"></span><dd>{{ longDay(b.startAt) }}</dd></div>
              <div class="leader"><dt class="tag">Hora</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ time(b.startAt) }}</dd></div>
              <div class="leader"><dt class="tag">Profesional</dt><span class="dots" aria-hidden="true"></span><dd>{{ b.professionalName }}</dd></div>
              <div class="leader"><dt class="tag">A nombre de</dt><span class="dots" aria-hidden="true"></span><dd>{{ b.patientName }}</dd></div>
            </dl>
            @if (b.status === 'PENDING') { <p class="hint">El spa aún no confirma tu cita. Te escribimos por WhatsApp.</p> }
            @if (b.rejectReason && (b.status === 'REJECTED' || b.status === 'CANCELLED')) { <p class="hint">Motivo: {{ b.rejectReason }}</p> }
            @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
            <div class="row">
              @if (cancellable(b)) {
                @if (confirming()) {
                  <span>¿Seguro que quieres cancelarla?</span>
                  <button type="button" class="btn btn-danger" (click)="cancel()">Sí, cancelar</button>
                  <button type="button" class="btn btn-line" (click)="confirming.set(false)">No</button>
                } @else {
                  <button type="button" class="btn btn-line" (click)="confirming.set(true)">Cancelar cita</button>
                }
              }
              <span class="spacer"></span>
              @if (tenant.whatsappLink('Hola, tengo una cita de ' + b.treatmentName); as wa) {
                <a class="btn btn-ink" [href]="wa" target="_blank" rel="noopener">Escribir al spa</a>
              }
              <a class="btn btn-line" routerLink="/agendar">Agendar otra</a>
            </div>
          </div></div>
          <p class="hint">¿Quieres ver tu historia y tus sesiones? Entra a <a routerLink="/mi-historia" class="link">Mi historia</a> con la cuenta de Google de tu correo.</p>
        } @else if (notFound()) {
          <div class="cut empty"><p class="serif">No encontramos esa cita</p><p>Revisa el enlace o escríbenos.</p><a class="btn btn-ink" routerLink="/">Ir al inicio</a></div>
        } @else {
          <p class="tag">Cargando…</p>
        }
      </main>
    </div>
  `
})
export default class BookingLinkComponent implements OnInit {
  private api = inject(ApiService);
  readonly tenant = inject(TenantService);
  readonly token = input.required<string>();
  readonly booking = signal<Booking | null>(null);
  readonly notFound = signal(false);
  readonly confirming = signal(false);
  readonly error = signal<string | null>(null);
  readonly status = STATUS_LABEL;
  readonly longDay = longDay;
  readonly time = time;

  ngOnInit() {
    this.api.booking(this.token()).subscribe({ next: (b) => this.booking.set(b), error: () => this.notFound.set(true) });
  }

  cancellable(b: Booking): boolean {
    return (b.status === 'PENDING' || b.status === 'ACCEPTED') && new Date(b.startAt).getTime() > Date.now();
  }

  cancel() {
    this.api.cancelBooking(this.token()).subscribe({
      next: (b) => { this.booking.set(b); this.confirming.set(false); },
      error: (e) => this.error.set(errorMessage(e))
    });
  }
}
