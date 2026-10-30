import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Booking, ClinicalAccess, ClinicalRecord, Consent, ConsentTemplate, Patient, PublicTestimonial, SessionNote } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { AuthService } from '../../core/auth/auth.service';
import { CONSENT_LABEL, longDay, PHOTO_LABEL, shortDate, stamp, STATUS_LABEL, time } from '../../shared/format';
import { SecurePhotoComponent } from '../../shared/secure-photo.component';
import { SiteHeaderComponent } from '../../shared/site-header.component';

type Tab = 'resumen' | 'sesiones' | 'citas' | 'autorizaciones' | 'accesos';
type State = 'loading' | 'ready' | 'unlinked' | 'error';

const CHANNEL: Record<string, string> = { PANEL: 'Panel del spa', PORTAL: 'Tú, en línea', MCP: 'Asistente de la esteticista' };
const ACTION: Partial<Record<string, string>> = {
  READ_RECORD: 'Consultó tu anamnesis', UPDATE_RECORD: 'Actualizó tu anamnesis', READ_SESSIONS: 'Consultó tus sesiones',
  READ_SESSION: 'Consultó una sesión', CREATE_SESSION: 'Registró una sesión', UPDATE_SESSION: 'Editó una sesión',
  ADD_PHOTO: 'Agregó una foto', DELETE_PHOTO: 'Borró una foto', READ_SUMMARY: 'Consultó tu resumen',
  CHECK_CONTRAINDICATIONS: 'Revisó contraindicaciones', SEARCH_NOTES: 'Buscó en las notas'
};

/** The patient's own clinical history, signed in with Google (linked by her verified email). */
@Component({
  selector: 'app-portal',
  imports: [RouterLink, SiteHeaderComponent, SecurePhotoComponent],
  template: `
    <div class="app">
      <app-site-header [back]="true" />
      <main class="wrap page">
        @switch (state()) {
          @case ('ready') {
            @let p = patient()!;
            <div class="row">
              <h1 class="page-h1">Hola, {{ p.fullName.split(' ')[0] }}</h1>
              <span class="spacer"></span>
              <button type="button" class="btn btn-line btn-sm" (click)="auth.logout()">Salir</button>
            </div>
            <p style="margin:0;max-width:44em">Esta es tu historia en {{ tenant.tenant()?.name }}: lo que la esteticista registró de tu salud y de cada sesión. Si algo no es correcto, díselo en tu próxima visita.</p>
            <div class="tabs" role="tablist">
              @for (t of tabs; track t.id) {
                <button type="button" role="tab" class="tab" [attr.aria-selected]="tab() === t.id" (click)="tab.set(t.id)">{{ t.label }}</button>
              }
            </div>

            @switch (tab()) {
              @case ('resumen') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Tu anamnesis</h2>
                  @if (record(); as r) {
                    @if (r.updatedAt) {
                      <dl class="kv">
                        <dt>Alergias</dt><dd>{{ r.allergies ?? '—' }}</dd>
                        <dt>Medicamentos</dt><dd>{{ r.medications ?? '—' }}</dd>
                        <dt>Condiciones</dt><dd>{{ r.conditions ?? '—' }}</dd>
                        <dt>Embarazo o lactancia</dt><dd>{{ r.pregnancy ?? '—' }}</dd>
                        <dt>Tipo de piel</dt><dd>{{ r.skinType ?? '—' }}{{ r.phototype ? ' · fototipo ' + r.phototype : '' }}</dd>
                        <dt>Cirugías</dt><dd>{{ r.surgeries ?? '—' }}</dd>
                        <dt>Hábitos</dt><dd>{{ r.habits ?? '—' }}</dd>
                        <dt>Observaciones</dt><dd>{{ r.observations ?? '—' }}</dd>
                      </dl>
                      <p class="meta">Actualizada por {{ r.updatedBy }} el {{ shortDate(r.updatedAt) }}.</p>
                    } @else {
                      <p class="empty-note">Aún no hay anamnesis. La esteticista la llena contigo en tu primera visita.</p>
                    }
                  }
                </div></div>
              }
              @case ('sesiones') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Tus sesiones</h2>
                  @if (sessions().length) {
                    <ul class="list">
                      @for (s of sessions(); track s.id) {
                        <li>
                          <div class="item-top"><span class="item-title">{{ s.treatmentName ?? 'Sesión' }}</span><span class="meta">{{ longDay(s.performedAt) }} · {{ s.professionalName }}</span></div>
                          @if (s.products) { <div><span class="tag" style="font-size:15px">Productos:</span> {{ s.products }}</div> }
                          @if (s.observations) { <div class="pre">{{ s.observations }}</div> }
                          @if (s.recommendations) { <div class="note-box"><strong>Cuidados en casa:</strong> <span class="pre">{{ s.recommendations }}</span></div> }
                          @if (s.photos.length) {
                            <div class="photos">
                              @for (ph of s.photos; track ph.id) {
                                <figure class="photo"><app-secure-photo [photoId]="ph.id" source="portal" [alt]="photoLabel[ph.kind]" /><figcaption>{{ photoLabel[ph.kind] }}{{ ph.caption ? ' · ' + ph.caption : '' }}</figcaption></figure>
                              }
                            </div>
                          }
                        </li>
                      }
                    </ul>
                  } @else { <p class="empty-note">Todavía no hay sesiones registradas.</p> }
                </div></div>
              }
              @case ('citas') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <div class="row"><h2 class="sheet-h2">Tus citas</h2><span class="spacer"></span><a class="btn btn-ink btn-sm" routerLink="/agendar">Agendar</a></div>
                  @if (appointments().length) {
                    <ul class="list">
                      @for (a of appointments(); track a.token) {
                        <li>
                          <div class="item-top"><a class="item-title" [routerLink]="['/cita', a.token]">{{ a.treatmentName }}</a><span class="badge" [attr.data-s]="a.status">{{ status[a.status] }}</span></div>
                          <span class="meta">{{ longDay(a.startAt) }}, {{ time(a.startAt) }} · {{ a.professionalName }}</span>
                        </li>
                      }
                    </ul>
                  } @else { <p class="empty-note">No tienes citas.</p> }
                </div></div>
              }
              @case ('autorizaciones') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Fotos y testimonio en el sitio</h2>
                  @if (photoConsent()) {
                    <p class="ok-msg">Autorizaste publicar tus fotos de antes y después (desde el {{ shortDate(photoConsent()!.acceptedAt) }}).</p>
                    @if (myStories().length) {
                      <p style="margin:0">Publicadas ahora: {{ myStories().length }} historia(s). Si retiras la autorización, se quitan del sitio de inmediato.</p>
                    }
                    <div class="row"><button type="button" class="btn btn-danger" (click)="revokePhotos()">Retirar autorización</button></div>
                  } @else {
                    <p style="margin:0">Si tu tratamiento te dejó feliz, puedes autorizar que el spa publique tus fotos de antes y después, sin tu nombre completo. Tú decides y puedes retirarlas cuando quieras.</p>
                    @if (photoText(); as t) { <div class="policy">{{ t.body }}</div> }
                    <div class="row"><button type="button" class="btn btn-ink" (click)="grantPhotos()">Autorizo publicar mis fotos</button></div>
                  }
                  @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
                  <h3 class="sheet-h3" style="margin-top:8px">Historial de autorizaciones</h3>
                  <ul class="list">
                    @for (c of consents(); track c.id) {
                      <li><div class="item-top"><span>{{ consentLabel[c.type] }} <span class="meta">(versión {{ c.templateVersion }})</span></span>
                        <span class="meta">{{ shortDate(c.acceptedAt) }}{{ c.revokedAt ? ' · retirada ' + shortDate(c.revokedAt) : '' }}</span></div></li>
                    }
                  </ul>
                </div></div>
              }
              @case ('accesos') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Quién consultó tu historia</h2>
                  <p class="hint">Cada vez que alguien abre o cambia tu historia queda registrado aquí.</p>
                  <div class="scroll-x"><table class="tbl">
                    <thead><tr><th>Cuándo</th><th>Quién</th><th>Desde</th><th>Qué</th></tr></thead>
                    <tbody>
                      @for (a of accessLog(); track a.id) {
                        <tr><td class="num">{{ stamp(a.at) }}</td><td>{{ a.actorName }}</td><td>{{ channel[a.channel] }}</td><td>{{ action[a.action] ?? a.action }}</td></tr>
                      }
                    </tbody>
                  </table></div>
                </div></div>
              }
            }
          }
          @case ('unlinked') {
            <h1 class="page-h1">No encontramos tu historia</h1>
            <div class="sheet cut"><div class="sheet-in cut">
              <p style="margin:0">Entraste como <strong>{{ auth.displayName() }}</strong>, pero ninguna paciente de {{ tenant.tenant()?.name }} tiene ese correo.</p>
              <p style="margin:0">Tu historia se abre con el mismo correo que diste al agendar. Si agendaste sin correo o con otro, pídele al spa que lo actualice y vuelve a entrar.</p>
              <div class="row">
                <button type="button" class="btn btn-ink" (click)="auth.logout()">Entrar con otra cuenta</button>
                @if (tenant.whatsappLink('Hola, quiero ver mi historia en línea. Mi correo es: '); as wa) { <a class="btn btn-line" [href]="wa" target="_blank" rel="noopener">Escribir al spa</a> }
              </div>
            </div></div>
          }
          @case ('error') {
            <h1 class="page-h1">No pudimos abrir tu historia</h1>
            <p class="error-msg">{{ error() }}</p>
            <button type="button" class="btn btn-ink" (click)="load()">Reintentar</button>
          }
          @default { <p class="tag">Abriendo tu historia…</p> }
        }
      </main>
    </div>
  `
})
export default class PortalComponent implements OnInit {
  private api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly tenant = inject(TenantService);

  readonly tabs: { id: Tab; label: string }[] = [
    { id: 'resumen', label: 'Resumen' }, { id: 'sesiones', label: 'Sesiones' }, { id: 'citas', label: 'Citas' },
    { id: 'autorizaciones', label: 'Autorizaciones' }, { id: 'accesos', label: 'Accesos' }
  ];
  readonly tab = signal<Tab>('resumen');
  readonly state = signal<State>('loading');
  readonly patient = signal<Patient | null>(null);
  readonly record = signal<ClinicalRecord | null>(null);
  readonly sessions = signal<SessionNote[]>([]);
  readonly appointments = signal<Booking[]>([]);
  readonly consents = signal<Consent[]>([]);
  readonly accessLog = signal<ClinicalAccess[]>([]);
  readonly myStories = signal<PublicTestimonial[]>([]);
  readonly photoText = signal<ConsentTemplate | null>(null);
  readonly error = signal<string | null>(null);

  readonly status = STATUS_LABEL;
  readonly consentLabel = CONSENT_LABEL;
  readonly photoLabel = PHOTO_LABEL;
  readonly channel = CHANNEL;
  readonly action = ACTION;
  readonly longDay = longDay;
  readonly shortDate = shortDate;
  readonly stamp = stamp;
  readonly time = time;

  ngOnInit() {
    this.load();
  }

  photoConsent(): Consent | null {
    return this.consents().find(c => c.type === 'PHOTO_TESTIMONIAL' && !c.revokedAt) ?? null;
  }

  load() {
    this.state.set('loading');
    this.api.me().subscribe({
      next: (p) => {
        this.patient.set(p);
        this.state.set('ready');
        this.api.myRecord().subscribe(r => this.record.set(r));
        this.api.mySessions().subscribe(s => this.sessions.set(s));
        this.api.myAppointments().subscribe(a => this.appointments.set(a));
        this.loadConsents();
        this.api.myAccessLog().subscribe(a => this.accessLog.set(a));
        this.api.consentText('PHOTO_TESTIMONIAL').subscribe({ next: (t) => this.photoText.set(t), error: () => {} });
      },
      error: (e) => {
        if (e instanceof HttpErrorResponse && e.error?.code === 'PATIENT_NOT_LINKED') {
          this.state.set('unlinked');
        } else {
          this.error.set(errorMessage(e));
          this.state.set('error');
        }
      }
    });
  }

  grantPhotos() {
    this.error.set(null);
    this.api.grantMyConsent('PHOTO_TESTIMONIAL').subscribe({ next: () => this.loadConsents(), error: (e) => this.error.set(errorMessage(e)) });
  }

  revokePhotos() {
    this.error.set(null);
    this.api.revokeMyConsent('PHOTO_TESTIMONIAL').subscribe({ next: () => this.loadConsents(), error: (e) => this.error.set(errorMessage(e)) });
  }

  private loadConsents() {
    this.api.myConsents().subscribe(c => this.consents.set(c));
    this.api.myTestimonials().subscribe(t => this.myStories.set(t.filter(x => x.photos.length)));
  }
}
