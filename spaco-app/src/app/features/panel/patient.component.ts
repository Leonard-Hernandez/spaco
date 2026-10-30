import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import {
  Appointment, ClinicalAccess, Consent, ConsentType, ContraindicationCheck, Patient, PatientSummary, PhotoKind, RecordInput,
  SessionNote, Treatment
} from '../../core/api/models';
import { CONSENT_LABEL, longDay, PHOTO_LABEL, phone, shortDate, stamp, STATUS_LABEL, time } from '../../shared/format';
import { SecurePhotoComponent } from '../../shared/secure-photo.component';
import { StaffContext } from './staff.service';

type Tab = 'anamnesis' | 'sesiones' | 'citas' | 'autorizaciones' | 'contraindicaciones' | 'accesos';

interface NoteForm {
  id?: number;
  appointmentId: number | null;
  treatmentId: number | null;
  performedAt: string;
  products: string;
  parameters: string;
  observations: string;
  recommendations: string;
}

const EMPTY_RECORD: RecordInput = {
  allergies: null, medications: null, conditions: null, pregnancy: null, skinType: null, phototype: null,
  surgeries: null, habits: null, observations: null
};

const CHANNEL: Record<string, string> = { PANEL: 'Panel', PORTAL: 'La paciente', MCP: 'Asistente IA' };

/** A patient: contact for everyone; clinical history for the owner and estheticians. */
@Component({
  selector: 'app-patient',
  imports: [FormsModule, RouterLink, SecurePhotoComponent],
  template: `
    <main class="wrap page">
      <a routerLink="/panel/pacientes" class="navlink">← Pacientes</a>
      @if (patient(); as p) {
        <div class="split">
          <div style="display:flex;flex-direction:column;gap:18px">
            <div class="sheet cut"><div class="sheet-in cut">
              <h1 class="sheet-h2">{{ p.fullName }}</h1>
              @if (editing()) {
                <form (ngSubmit)="saveContact()" style="display:flex;flex-direction:column;gap:12px">
                  <div class="field"><label class="label-sm" for="cn">Nombre</label><input id="cn" name="cn" class="ctl" [(ngModel)]="contact.fullName" /></div>
                  <div class="field"><label class="label-sm" for="cp">Celular</label><input id="cp" name="cp" class="ctl num" [(ngModel)]="contact.phone" /></div>
                  <div class="field"><label class="label-sm" for="ce">Correo</label><input id="ce" name="ce" class="ctl" type="email" [(ngModel)]="contact.email" /></div>
                  <div class="field"><label class="label-sm" for="cb">Nacimiento</label><input id="cb" name="cb" class="ctl num" type="date" [(ngModel)]="contact.birthDate" /></div>
                  <div class="row"><button type="submit" class="btn btn-ink btn-sm">Guardar</button><button type="button" class="btn-quiet" (click)="editing.set(false)">Cancelar</button></div>
                </form>
              } @else {
                <dl class="dl">
                  <div class="leader"><dt class="tag">Celular</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ phone(p.phone) }}</dd></div>
                  <div class="leader"><dt class="tag">Correo</dt><span class="dots" aria-hidden="true"></span><dd>{{ p.email ?? '—' }}</dd></div>
                  <div class="leader"><dt class="tag">Edad</dt><span class="dots" aria-hidden="true"></span><dd class="num">{{ summary()?.age ?? '—' }}</dd></div>
                  <div class="leader"><dt class="tag">En línea</dt><span class="dots" aria-hidden="true"></span><dd>{{ p.portalLinked ? 'Activa' : 'No ha entrado' }}</dd></div>
                </dl>
                <div class="row"><button type="button" class="btn btn-line btn-sm" (click)="startEdit(p)">Editar contacto</button>
                  @if (whatsapp(p); as wa) { <a class="btn btn-gold btn-sm" [href]="wa" target="_blank" rel="noopener">WhatsApp</a> }
                </div>
              }
            </div></div>
            @if (summary(); as s) {
              <div class="sheet cut"><div class="sheet-in cut">
                <h2 class="sheet-h3">Alertas</h2>
                @if (s.alerts.length) {
                  <div style="display:flex;flex-direction:column;gap:6px">@for (a of s.alerts; track $index) { <span class="alert-chip">{{ a }}</span> }</div>
                } @else { <p class="meta" style="margin:0">Sin alertas.</p> }
                @if (s.upcoming.length) {
                  <h2 class="sheet-h3">Próximas citas</h2>
                  @for (a of s.upcoming; track a.id) { <span class="meta">{{ longDay(a.startAt) }}, {{ time(a.startAt) }} · {{ a.treatmentName }}</span> }
                }
              </div></div>
            }
          </div>

          <div style="display:flex;flex-direction:column;gap:18px;min-width:0">
            @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
            @if (notice()) { <p class="ok-msg" role="status">{{ notice() }}</p> }
            <div class="tabs" role="tablist">
              @for (t of visibleTabs(); track t.id) {
                <button type="button" role="tab" class="tab" [attr.aria-selected]="tab() === t.id" (click)="open(t.id)">{{ t.label }}</button>
              }
            </div>

            @switch (tab()) {
              @case ('anamnesis') {
                <form class="sheet cut" (ngSubmit)="saveRecord()"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Anamnesis</h2>
                  <div class="grid-2">
                    <div class="field span2"><label class="label-sm" for="al">Alergias</label><textarea id="al" name="al" class="ctl" rows="2" [(ngModel)]="record.allergies"></textarea></div>
                    <div class="field"><label class="label-sm" for="me">Medicamentos</label><textarea id="me" name="me" class="ctl" rows="2" [(ngModel)]="record.medications"></textarea></div>
                    <div class="field"><label class="label-sm" for="co">Condiciones de salud</label><textarea id="co" name="co" class="ctl" rows="2" [(ngModel)]="record.conditions"></textarea></div>
                    <div class="field"><label class="label-sm" for="pr">Embarazo o lactancia</label><input id="pr" name="pr" class="ctl" placeholder="No" [(ngModel)]="record.pregnancy" /></div>
                    <div class="field"><label class="label-sm" for="sk">Tipo de piel</label><input id="sk" name="sk" class="ctl" placeholder="Mixta, sensible…" [(ngModel)]="record.skinType" /></div>
                    <div class="field"><label class="label-sm" for="ph">Fototipo (Fitzpatrick)</label>
                      <select id="ph" name="ph" class="ctl" [(ngModel)]="record.phototype"><option [ngValue]="null">—</option>@for (n of [1,2,3,4,5,6]; track n) { <option [ngValue]="n">{{ n }}</option> }</select>
                    </div>
                    <div class="field"><label class="label-sm" for="su">Cirugías y procedimientos</label><textarea id="su" name="su" class="ctl" rows="2" [(ngModel)]="record.surgeries"></textarea></div>
                    <div class="field span2"><label class="label-sm" for="ha">Hábitos (sol, agua, tabaco, rutina)</label><textarea id="ha" name="ha" class="ctl" rows="2" [(ngModel)]="record.habits"></textarea></div>
                    <div class="field span2"><label class="label-sm" for="ob">Observaciones</label><textarea id="ob" name="ob" class="ctl" rows="3" [(ngModel)]="record.observations"></textarea></div>
                  </div>
                  <div class="row">
                    @if (summary()?.record?.updatedAt; as u) { <span class="meta">Última actualización: {{ summary()!.record.updatedBy }}, {{ stamp(u) }}</span> }
                    <span class="spacer"></span>
                    <button type="submit" class="btn btn-ink">Guardar anamnesis</button>
                  </div>
                </div></form>
              }
              @case ('sesiones') {
                <form class="sheet cut" (ngSubmit)="saveNote()"><div class="sheet-in cut">
                  <h2 class="sheet-h2">{{ note.id ? 'Editar sesión' : 'Nueva sesión' }}</h2>
                  <div class="grid-2">
                    @if (!note.id) {
                      <div class="field"><label class="label-sm" for="ap">Cita</label>
                        <select id="ap" name="ap" class="ctl" [ngModel]="note.appointmentId" (ngModelChange)="pickAppointment($event)">
                          <option [ngValue]="null">Sin cita (visita sin agendar)</option>
                          @for (a of openAppointments(); track a.id) { <option [ngValue]="a.id">{{ longDay(a.startAt) }} · {{ a.treatmentName }}</option> }
                        </select>
                      </div>
                    }
                    <div class="field"><label class="label-sm" for="tr">Tratamiento</label>
                      <select id="tr" name="tr" class="ctl" [(ngModel)]="note.treatmentId">
                        <option [ngValue]="null">—</option>
                        @for (t of treatments(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }
                      </select>
                    </div>
                    <div class="field"><label class="label-sm" for="pa">Fecha</label><input id="pa" name="pa" class="ctl num" type="datetime-local" [(ngModel)]="note.performedAt" /></div>
                    <div class="field span2"><label class="label-sm" for="pd">Productos usados</label><textarea id="pd" name="pd" class="ctl" rows="2" [(ngModel)]="note.products"></textarea></div>
                    <div class="field span2"><label class="label-sm" for="pm">Parámetros (equipo, concentración, tiempo)</label><textarea id="pm" name="pm" class="ctl" rows="2" [(ngModel)]="note.parameters"></textarea></div>
                    <div class="field span2"><label class="label-sm" for="os">Observaciones</label><textarea id="os" name="os" class="ctl" rows="3" [(ngModel)]="note.observations"></textarea></div>
                    <div class="field span2"><label class="label-sm" for="rc">Recomendaciones para casa</label><textarea id="rc" name="rc" class="ctl" rows="2" [(ngModel)]="note.recommendations"></textarea></div>
                  </div>
                  <div class="row-end">
                    @if (note.id) { <button type="button" class="btn-quiet" (click)="resetNote()">Cancelar edición</button> }
                    <button type="submit" class="btn btn-ink">{{ note.id ? 'Guardar cambios' : 'Registrar sesión' }}</button>
                  </div>
                </div></form>

                @if (picked().length) {
                  <div class="sheet cut"><div class="sheet-in cut">
                    <h2 class="sheet-h3">Testimonio con {{ picked().length }} foto(s)</h2>
                    @if (!summary()?.validConsents?.includes('PHOTO_TESTIMONIAL')) {
                      <p class="hint">La paciente aún no autoriza publicar fotos. Puedes crear el borrador; solo se publica cuando ella autorice (en línea o firmando aquí).</p>
                    }
                    <div class="grid-2">
                      <div class="field"><label class="label-sm" for="tn">Cómo se muestra</label><input id="tn" class="ctl" [(ngModel)]="story.displayName" name="tn" placeholder="Laura, 34 años" /></div>
                      <div class="field"><label class="label-sm" for="tt">Tratamiento</label>
                        <select id="tt" class="ctl" name="tt" [(ngModel)]="story.treatmentId"><option [ngValue]="null">—</option>@for (t of treatments(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }</select>
                      </div>
                      <div class="field span2"><label class="label-sm" for="tq">Testimonio (sus palabras)</label><textarea id="tq" class="ctl" name="tq" rows="2" maxlength="600" [(ngModel)]="story.quote"></textarea></div>
                    </div>
                    <div class="row-end"><button type="button" class="btn-quiet" (click)="picked.set([])">Quitar selección</button><button type="button" class="btn btn-ink" (click)="createStory()">Crear borrador</button></div>
                  </div></div>
                }

                @for (s of sessions(); track s.id) {
                  <div class="sheet cut"><div class="sheet-in cut">
                    <div class="item-top"><span class="item-title">{{ s.treatmentName ?? 'Sesión' }}</span><span class="meta">{{ stamp(s.performedAt) }} · {{ s.professionalName ?? s.createdBy }}</span></div>
                    <dl class="kv">
                      @if (s.products) { <dt>Productos</dt><dd>{{ s.products }}</dd> }
                      @if (s.parameters) { <dt>Parámetros</dt><dd>{{ s.parameters }}</dd> }
                      @if (s.observations) { <dt>Observaciones</dt><dd>{{ s.observations }}</dd> }
                      @if (s.recommendations) { <dt>Recomendaciones</dt><dd>{{ s.recommendations }}</dd> }
                    </dl>
                    @if (s.photos.length) {
                      <div class="photos">
                        @for (ph of s.photos; track ph.id) {
                          <figure class="photo">
                            <app-secure-photo [photoId]="ph.id" [alt]="photoLabel[ph.kind]" />
                            <input type="checkbox" class="pick" [checked]="picked().includes(ph.id)" (change)="togglePick(ph.id)" [attr.aria-label]="'Usar en testimonio: ' + photoLabel[ph.kind]" />
                            <button type="button" class="btn btn-sm btn-line" style="min-height:32px;padding:0 8px;background:var(--wax)" (click)="deletePhoto(ph.id)" aria-label="Borrar foto">✕</button>
                            <figcaption>{{ photoLabel[ph.kind] }}{{ ph.caption ? ' · ' + ph.caption : '' }}</figcaption>
                          </figure>
                        }
                      </div>
                    }
                    <div class="row">
                      <label class="btn btn-line btn-sm" style="cursor:pointer">Subir foto
                        <input type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" (change)="upload(s, $event)" />
                      </label>
                      <select class="ctl" style="width:auto;height:44px" [(ngModel)]="photoKind" [name]="'kind' + s.id" aria-label="Tipo de foto">
                        <option value="BEFORE">Antes</option><option value="AFTER">Después</option><option value="PROGRESS">Evolución</option>
                      </select>
                      <span class="spacer"></span>
                      <button type="button" class="btn-quiet" (click)="editNote(s)">Editar</button>
                    </div>
                  </div></div>
                } @empty { <p class="empty-note">Sin sesiones registradas.</p> }
              }
              @case ('citas') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Citas</h2>
                  <ul class="list">
                    @for (a of appointments(); track a.id) {
                      <li><div class="item-top"><span>{{ longDay(a.startAt) }}, {{ time(a.startAt) }} · {{ a.treatmentName }}</span><span class="badge" [attr.data-s]="a.status">{{ status[a.status] }}</span></div>
                        <span class="meta">{{ a.professionalName }}{{ a.patientNote ? ' · Nota: ' + a.patientNote : '' }}</span></li>
                    } @empty { <li>Sin citas.</li> }
                  </ul>
                </div></div>
              }
              @case ('autorizaciones') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Autorizaciones</h2>
                  <ul class="list">
                    @for (c of consents(); track c.id) {
                      <li><div class="item-top"><span>{{ consentLabel[c.type] }}{{ c.treatmentId ? ' · ' + treatmentName(c.treatmentId) : '' }} <span class="meta">(v{{ c.templateVersion }}, {{ channelLabel(c) }})</span></span>
                        <span class="meta">{{ shortDate(c.acceptedAt) }}{{ c.revokedAt ? ' · retirada ' + shortDate(c.revokedAt) : '' }}</span></div>
                        @if (!c.revokedAt && c.type !== 'TREATMENT') { <div><button type="button" class="btn-quiet" (click)="revoke(c.type)">Registrar retiro</button></div> }
                      </li>
                    } @empty { <li>Sin autorizaciones.</li> }
                  </ul>
                  <h3 class="sheet-h3">Registrar firma en el spa</h3>
                  <div class="row">
                    <select class="ctl" style="width:auto" [(ngModel)]="grantType" name="gt" aria-label="Autorización">
                      <option value="TREATMENT">Consentimiento de tratamiento</option>
                      <option value="DATA_PROCESSING">Tratamiento de datos</option>
                      <option value="PHOTO_TESTIMONIAL">Publicar fotos y testimonio</option>
                    </select>
                    @if (grantType === 'TREATMENT') {
                      <select class="ctl" style="width:auto" [(ngModel)]="grantTreatment" name="gtr" aria-label="Tratamiento">
                        @for (t of treatments(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }
                      </select>
                    }
                    <button type="button" class="btn btn-ink" (click)="grant()">Registrar</button>
                  </div>
                  <p class="hint">Regístrala solo después de que la paciente lea y firme el texto vigente.</p>
                </div></div>
              }
              @case ('contraindicaciones') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Revisar contraindicaciones</h2>
                  <div class="row">
                    <select class="ctl" style="max-width:360px" [(ngModel)]="checkTreatment" name="ct" aria-label="Tratamiento">
                      @for (t of treatments(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }
                    </select>
                    <button type="button" class="btn btn-ink" (click)="check()">Revisar</button>
                  </div>
                  @if (checkResult(); as r) {
                    <dl class="kv">
                      <dt>Contraindicaciones</dt><dd>{{ r.contraindications ?? 'El tratamiento no tiene contraindicaciones registradas.' }}</dd>
                      <dt>Coincidencias</dt><dd>{{ r.matches.length ? r.matches.join(', ') : 'Ninguna palabra coincide con la historia.' }}</dd>
                      <dt>Alertas</dt><dd>{{ r.alerts.length ? r.alerts.join('\n') : 'Sin alertas.' }}</dd>
                      <dt>Anamnesis</dt><dd>{{ r.recordFilled ? 'Registrada' : 'Falta llenarla' }}</dd>
                      <dt>Consentimiento</dt><dd>{{ r.treatmentConsentSigned ? 'Firmado' : 'No firmado para este tratamiento' }}</dd>
                    </dl>
                    <p class="hint">Es un recordatorio por palabras; la decisión clínica es tuya.</p>
                  }
                </div></div>
              }
              @case ('accesos') {
                <div class="sheet cut"><div class="sheet-in cut">
                  <h2 class="sheet-h2">Quién consultó esta historia</h2>
                  <div class="scroll-x"><table class="tbl">
                    <thead><tr><th>Cuándo</th><th>Quién</th><th>Desde</th><th>Acción</th></tr></thead>
                    <tbody>@for (a of access(); track a.id) { <tr><td class="num">{{ stamp(a.at) }}</td><td>{{ a.actorName }}</td><td>{{ channel[a.channel] }}</td><td>{{ a.action }}</td></tr> }</tbody>
                  </table></div>
                </div></div>
              }
            }
          </div>
        </div>
      } @else if (error()) {
        <p class="error-msg">{{ error() }}</p>
      } @else { <p class="tag">Cargando…</p> }
    </main>
  `
})
export default class PatientComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  readonly ctx = inject(StaffContext);

  readonly id = input.required<string>();
  /** ?cita=: open a session note for that visit */
  readonly cita = input<string>();

  readonly patient = signal<Patient | null>(null);
  readonly summary = signal<PatientSummary | null>(null);
  readonly sessions = signal<SessionNote[]>([]);
  readonly appointments = signal<Appointment[]>([]);
  readonly consents = signal<Consent[]>([]);
  readonly access = signal<ClinicalAccess[]>([]);
  readonly treatments = signal<Treatment[]>([]);
  readonly checkResult = signal<ContraindicationCheck | null>(null);
  readonly picked = signal<number[]>([]);
  readonly tab = signal<Tab>('anamnesis');
  readonly editing = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);

  record: RecordInput = { ...EMPTY_RECORD };
  note: NoteForm = this.blankNote();
  contact = { fullName: '', phone: '', email: '', birthDate: '' };
  story = { displayName: '', quote: '', treatmentId: null as number | null };
  photoKind: PhotoKind = 'BEFORE';
  grantType: ConsentType = 'TREATMENT';
  grantTreatment: number | null = null;
  checkTreatment: number | null = null;

  readonly tabsAll: { id: Tab; label: string; clinical: boolean }[] = [
    { id: 'anamnesis', label: 'Anamnesis', clinical: true }, { id: 'sesiones', label: 'Sesiones', clinical: true },
    { id: 'citas', label: 'Citas', clinical: false }, { id: 'autorizaciones', label: 'Autorizaciones', clinical: true },
    { id: 'contraindicaciones', label: 'Contraindicaciones', clinical: true }, { id: 'accesos', label: 'Accesos', clinical: true }
  ];
  readonly visibleTabs = computed(() => this.tabsAll.filter(t => !t.clinical || this.ctx.clinical()));
  readonly openAppointments = computed(() => this.appointments().filter(a => a.status === 'ACCEPTED' || a.status === 'PENDING' || a.status === 'COMPLETED'));

  readonly status = STATUS_LABEL;
  readonly consentLabel = CONSENT_LABEL;
  readonly photoLabel = PHOTO_LABEL;
  readonly channel = CHANNEL;
  readonly longDay = longDay;
  readonly time = time;
  readonly stamp = stamp;
  readonly shortDate = shortDate;
  readonly phone = phone;

  private get pid() {
    return Number(this.id());
  }

  ngOnInit() {
    this.api.patient(this.pid).subscribe({ next: (p) => this.patient.set(p), error: (e) => this.error.set(errorMessage(e)) });
    this.api.patientAppointments(this.pid).subscribe(a => {
      this.appointments.set(a);
      const visit = a.find(x => String(x.id) === this.cita());
      if (visit && this.ctx.clinical()) {
        this.tab.set('sesiones');
        this.pickAppointment(visit.id);
      }
    });
    if (!this.ctx.clinical()) {
      this.tab.set('citas');
      return;
    }
    this.api.catalog().subscribe(({ treatments: t }) => {
      this.treatments.set(t);
      this.grantTreatment = t[0]?.id ?? null;
      this.checkTreatment = t[0]?.id ?? null;
    });
    this.loadSummary();
    this.loadSessions();
    this.loadConsents();
  }

  open(tab: Tab) {
    this.tab.set(tab);
    this.notice.set(null);
    if (tab === 'accesos') this.api.accessLog(this.pid).subscribe(a => this.access.set(a));
  }

  startEdit(p: Patient) {
    this.contact = { fullName: p.fullName, phone: p.phone, email: p.email ?? '', birthDate: p.birthDate ?? '' };
    this.editing.set(true);
  }

  saveContact() {
    this.run(this.api.savePatient({
      fullName: this.contact.fullName.trim(), phone: this.contact.phone, email: this.contact.email.trim() || null,
      birthDate: this.contact.birthDate || null
    }, this.pid), (p) => { this.patient.set(p); this.editing.set(false); this.loadSummary(); });
  }

  saveRecord() {
    this.run(this.api.saveRecord(this.pid, this.record), () => { this.notice.set('Anamnesis guardada.'); this.loadSummary(); });
  }

  pickAppointment(id: number | null) {
    this.note.appointmentId = id;
    const a = this.appointments().find(x => x.id === id);
    if (a) {
      this.note.treatmentId = a.treatmentId;
      this.note.performedAt = toLocalInput(a.startAt);
    }
  }

  saveNote() {
    const n = this.note;
    this.run(this.api.saveSession(this.pid, {
      appointmentId: n.appointmentId, treatmentId: n.treatmentId, professionalId: null,
      performedAt: n.performedAt ? `${n.performedAt}:00-05:00` : null,
      products: n.products || null, parameters: n.parameters || null, observations: n.observations || null,
      recommendations: n.recommendations || null
    }, n.id), () => {
      this.notice.set(n.id ? 'Sesión actualizada.' : 'Sesión registrada. Ahora puedes subir sus fotos.');
      this.resetNote();
      this.loadSessions();
      this.loadSummary();
      this.api.patientAppointments(this.pid).subscribe(a => this.appointments.set(a));
    });
  }

  editNote(s: SessionNote) {
    this.note = {
      id: s.id, appointmentId: s.appointmentId, treatmentId: s.treatmentId, performedAt: toLocalInput(s.performedAt),
      products: s.products ?? '', parameters: s.parameters ?? '', observations: s.observations ?? '',
      recommendations: s.recommendations ?? ''
    };
    window.scrollTo({ top: 0 });
  }

  resetNote() {
    this.note = this.blankNote();
  }

  upload(s: SessionNote, event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.run(this.api.addPhoto(s.id, file, this.photoKind, null), () => this.loadSessions());
  }

  deletePhoto(id: number) {
    if (!confirm('¿Borrar esta foto de la historia?')) return;
    this.run(this.api.deletePhoto(id), () => { this.picked.set(this.picked().filter(x => x !== id)); this.loadSessions(); });
  }

  togglePick(id: number) {
    this.picked.set(this.picked().includes(id) ? this.picked().filter(x => x !== id) : [...this.picked(), id]);
    if (!this.story.displayName) this.story.displayName = (this.patient()?.fullName.split(' ')[0] ?? '') + (this.summary()?.age ? `, ${this.summary()!.age} años` : '');
  }

  createStory() {
    if (!this.story.displayName.trim()) { this.error.set('Escribe cómo se muestra la paciente.'); return; }
    this.run(this.api.saveTestimonial({
      patientId: this.pid, displayName: this.story.displayName.trim(), quote: this.story.quote.trim() || null,
      treatmentId: this.story.treatmentId, photoIds: this.picked()
    }), () => this.router.navigate(['/panel/testimonios']));
  }

  grant() {
    this.run(this.api.grantConsent(this.pid, this.grantType, this.grantType === 'TREATMENT' ? this.grantTreatment : null), () => {
      this.notice.set('Autorización registrada.');
      this.loadConsents();
      this.loadSummary();
    });
  }

  revoke(type: ConsentType) {
    if (!confirm('¿Registrar que la paciente retiró esta autorización?')) return;
    this.run(this.api.revokeConsent(this.pid, type), () => { this.loadConsents(); this.loadSummary(); });
  }

  check() {
    if (this.checkTreatment) this.run(this.api.contraindications(this.pid, this.checkTreatment), (r) => this.checkResult.set(r));
  }

  treatmentName(id: number): string {
    return this.treatments().find(t => t.id === id)?.name ?? '';
  }

  channelLabel(c: Consent): string {
    return c.channel === 'WEB' ? 'al agendar' : c.channel === 'PORTAL' ? 'en línea' : `en el spa, ${c.recordedBy ?? ''}`;
  }

  whatsapp(p: Patient): string | null {
    const digits = p.phone.replace(/\D/g, '');
    return digits ? `https://wa.me/${digits}` : null;
  }

  private loadSummary() {
    this.api.summary(this.pid).subscribe(s => {
      this.summary.set(s);
      const r = s.record;
      this.record = {
        allergies: r.allergies, medications: r.medications, conditions: r.conditions, pregnancy: r.pregnancy,
        skinType: r.skinType, phototype: r.phototype, surgeries: r.surgeries, habits: r.habits, observations: r.observations
      };
    });
  }

  private loadSessions() {
    this.api.sessions(this.pid).subscribe(s => this.sessions.set(s));
  }

  private loadConsents() {
    this.api.consents(this.pid).subscribe(c => this.consents.set(c));
  }

  private blankNote(): NoteForm {
    return { appointmentId: null, treatmentId: null, performedAt: toLocalInput(new Date().toISOString()), products: '', parameters: '', observations: '', recommendations: '' };
  }

  private run<T>(request: import('rxjs').Observable<T>, done: (value: T) => void) {
    this.error.set(null);
    request.subscribe({ next: done, error: (e) => this.error.set(errorMessage(e)) });
  }
}

/** "2026-10-21T09:30" in Bogotá for a datetime-local input. */
function toLocalInput(instant: string): string {
  const d = new Date(new Date(instant).getTime() - 5 * 3600_000);
  return d.toISOString().slice(0, 16);
}
