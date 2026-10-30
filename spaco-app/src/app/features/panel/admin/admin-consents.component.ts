import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { ConsentTemplate, ConsentType } from '../../../core/api/models';
import { CONSENT_LABEL } from '../../../shared/format';

/** The legal texts patients accept. Editing publishes a new version; past consents keep theirs. */
@Component({
  selector: 'app-admin-consents',
  imports: [FormsModule],
  template: `
    <main class="wrap page" style="max-width:980px">
      <h1 class="page-h1">Textos legales</h1>
      <p style="margin:0">Revisa estos textos con tu asesor legal (Ley 1581 de 2012 y normas de historia clínica). Cada cambio crea una versión nueva; cada autorización guarda la versión que la paciente aceptó.</p>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
      @if (notice()) { <p class="ok-msg">{{ notice() }}</p> }
      @for (type of types; track type) {
        @let current = currentOf(type);
        <form class="sheet cut" (ngSubmit)="publish(type)"><div class="sheet-in cut">
          <div class="item-top"><h2 class="sheet-h2">{{ label[type] }}</h2><span class="meta">{{ current ? 'Versión ' + current.version : 'Sin publicar' }}</span></div>
          <div class="field"><label class="label-sm" [for]="'t' + type">Título</label><input class="ctl" [id]="'t' + type" [name]="'t' + type" [(ngModel)]="drafts[type].title" /></div>
          <div class="field"><label class="label-sm" [for]="'b' + type">Texto</label><textarea class="ctl" rows="7" [id]="'b' + type" [name]="'b' + type" [(ngModel)]="drafts[type].body"></textarea></div>
          <div class="row-end"><button type="submit" class="btn btn-ink btn-sm">Publicar versión {{ (current?.version ?? 0) + 1 }}</button></div>
        </div></form>
      }
    </main>
  `
})
export default class AdminConsentsComponent implements OnInit {
  private api = inject(ApiService);
  readonly templates = signal<ConsentTemplate[]>([]);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly types: ConsentType[] = ['DATA_PROCESSING', 'TREATMENT', 'PHOTO_TESTIMONIAL'];
  readonly label = CONSENT_LABEL;
  drafts: Record<ConsentType, { title: string; body: string }> = {
    DATA_PROCESSING: { title: '', body: '' }, TREATMENT: { title: '', body: '' }, PHOTO_TESTIMONIAL: { title: '', body: '' }
  };

  ngOnInit() {
    this.load();
  }

  currentOf(type: ConsentType): ConsentTemplate | undefined {
    return this.templates().filter(t => t.type === type && t.active).sort((a, b) => b.version - a.version)[0];
  }

  load() {
    this.api.consentTemplates().subscribe(t => {
      this.templates.set(t);
      for (const type of this.types) {
        const c = this.currentOf(type);
        this.drafts[type] = { title: c?.title ?? '', body: c?.body ?? '' };
      }
    });
  }

  publish(type: ConsentType) {
    const d = this.drafts[type];
    this.error.set(null);
    this.api.publishConsentTemplate(type, d.title, d.body).subscribe({
      next: (t) => { this.notice.set(`Publicada la versión ${t.version}.`); this.load(); },
      error: (e) => this.error.set(errorMessage(e))
    });
  }
}
