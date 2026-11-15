import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { SpaProfile } from '../../../core/api/models';
import { TenantService } from '../../../core/api/tenant.service';

/** Form values are strings; empty ones are sent as null (the API clears the field). */
type Form = { [K in keyof SpaProfile]: string };

const EMPTY: Form = { phone: '', email: '', address: '', city: '', tagline: '', intro: '', instagram: '', cancellationNote: '' };

/** The spa's public contact details and texts. Opening hours are not here: they come from the schedule. */
@Component({
  selector: 'app-admin-profile',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="wrap page" style="max-width:980px">
      <h1 class="page-h1">Perfil del spa</h1>
      <p style="margin:0">Lo que ven tus pacientes en el sitio: contacto, dirección y textos de presentación. Los campos vacíos no se muestran.</p>
      @if (loading()) { <p class="tag">Cargando…</p> }
      @if (loadError()) {
        <p class="error-msg" role="alert">{{ loadError() }}</p>
        <button type="button" class="btn btn-ink btn-sm" (click)="load()">Reintentar</button>
      } @else if (!loading()) {
        <form class="sheet cut" (ngSubmit)="save()" #f="ngForm"><div class="sheet-in cut">
          <h2 class="sheet-h2">Contacto</h2>
          <div class="field"><label class="label-sm" for="p-phone">WhatsApp</label>
            <input class="ctl num" id="p-phone" name="phone" type="tel" inputmode="tel" autocomplete="off" maxlength="30" placeholder="300 123 4567" [(ngModel)]="form.phone" />
            <span class="meta">Celular de 10 dígitos (o con indicativo). Se usa para el botón de WhatsApp.</span></div>
          <div class="field"><label class="label-sm" for="p-email">Correo</label>
            <input class="ctl" id="p-email" name="email" type="email" maxlength="120" [(ngModel)]="form.email" #email="ngModel" email /></div>
          <div class="field"><label class="label-sm" for="p-address">Dirección</label>
            <input class="ctl" id="p-address" name="address" maxlength="255" [(ngModel)]="form.address" /></div>
          <div class="field"><label class="label-sm" for="p-city">Ciudad</label>
            <input class="ctl" id="p-city" name="city" maxlength="80" [(ngModel)]="form.city" /></div>
          <div class="field"><label class="label-sm" for="p-ig">Instagram</label>
            <input class="ctl" id="p-ig" name="instagram" maxlength="60" placeholder="@tuspa" [(ngModel)]="form.instagram" /></div>

          <h2 class="sheet-h2">Presentación</h2>
          <div class="field"><label class="label-sm" for="p-tagline">Frase principal</label>
            <input class="ctl" id="p-tagline" name="tagline" maxlength="160" [(ngModel)]="form.tagline" /></div>
          <div class="field"><label class="label-sm" for="p-intro">Introducción</label>
            <textarea class="ctl" id="p-intro" name="intro" rows="4" maxlength="600" [(ngModel)]="form.intro"></textarea></div>
          <div class="field"><label class="label-sm" for="p-cancel">Nota de cancelación</label>
            <textarea class="ctl" id="p-cancel" name="cancellationNote" rows="2" maxlength="300" [(ngModel)]="form.cancellationNote"></textarea></div>

          <p class="hint">El horario de atención del sitio se arma solo con los bloques de <a class="link" routerLink="/panel/horario">Horario</a>. Cámbialo allí.</p>

          @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
          @if (notice()) { <p class="ok-msg" role="status">{{ notice() }}</p> }
          <div class="row-end"><button type="submit" class="btn btn-ink" [disabled]="saving() || f.invalid">{{ saving() ? 'Guardando…' : 'Guardar cambios' }}</button></div>
        </div></form>
      }
    </main>
  `
})
export default class AdminProfileComponent implements OnInit {
  private api = inject(ApiService);
  private tenant = inject(TenantService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  form: Form = { ...EMPTY };

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.spaProfile().subscribe({
      next: (p) => { this.fill(p); this.loading.set(false); },
      error: (e) => { this.loadError.set(errorMessage(e, 'No pudimos cargar el perfil.')); this.loading.set(false); }
    });
  }

  save() {
    this.saving.set(true);
    this.error.set(null);
    this.notice.set(null);
    const body = Object.fromEntries(Object.entries(this.form).map(([k, v]) => [k, v.trim() || null])) as unknown as SpaProfile;
    this.api.saveSpaProfile(body).subscribe({
      next: (p) => {
        this.fill(p);
        this.saving.set(false);
        this.notice.set('Cambios guardados. Ya se ven en el sitio.');
        this.tenant.load();
      },
      error: (e) => { this.saving.set(false); this.error.set(errorMessage(e, 'No pudimos guardar. Intenta de nuevo.')); }
    });
  }

  private fill(p: SpaProfile) {
    this.form = { ...EMPTY, ...Object.fromEntries(Object.entries(p).map(([k, v]) => [k, v ?? ''])) };
  }
}
