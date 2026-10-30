import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Testimonial } from '../../core/api/models';
import { PHOTO_LABEL, shortDate, STATUS_LABEL } from '../../shared/format';
import { SecurePhotoComponent } from '../../shared/secure-photo.component';

/**
 * Before/after stories. They are created from a patient's session photos; publishing copies
 * the photos to the website and needs her valid photo consent.
 */
@Component({
  selector: 'app-testimonials',
  imports: [FormsModule, RouterLink, SecurePhotoComponent],
  template: `
    <main class="wrap page">
      <h1 class="page-h1">Testimonios</h1>
      <p style="margin:0;max-width:52em">Se crean desde la ficha de la paciente: marca sus fotos en «Sesiones». Solo se publican si ella autorizó sus fotos; si retira la autorización, salen del sitio de inmediato.</p>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
      @for (t of items(); track t.id) {
        <div class="sheet cut"><div class="sheet-in cut">
          <div class="item-top">
            <span class="item-title">{{ t.displayName }}{{ t.treatmentName ? ' · ' + t.treatmentName : '' }}</span>
            <span class="badge" [attr.data-s]="t.status">{{ status[t.status] }}</span>
          </div>
          <div class="photos">
            @for (p of t.photos; track p.id) {
              <figure class="photo"><app-secure-photo [photoId]="p.sessionPhotoId" [alt]="photoLabel[p.kind]" /><figcaption>{{ photoLabel[p.kind] }}</figcaption></figure>
            }
          </div>
          @if (editing() === t.id) {
            <div class="grid-2">
              <div class="field"><label class="label-sm" [for]="'n' + t.id">Cómo se muestra</label><input [id]="'n' + t.id" class="ctl" [(ngModel)]="draft.displayName" [name]="'n' + t.id" /></div>
              <div class="field span2"><label class="label-sm" [for]="'q' + t.id">Testimonio</label><textarea [id]="'q' + t.id" class="ctl" rows="2" maxlength="600" [(ngModel)]="draft.quote" [name]="'q' + t.id"></textarea></div>
            </div>
            <div class="row-end"><button type="button" class="btn-quiet" (click)="editing.set(null)">Cancelar</button><button type="button" class="btn btn-ink btn-sm" (click)="save(t)">Guardar</button></div>
          } @else {
            @if (t.quote) { <p class="story-quote" style="font-size:18px">“{{ t.quote }}”</p> }
          }
          <div class="row">
            <a class="link" [routerLink]="['/panel/pacientes', t.patientId]">Ver paciente</a>
            @if (t.publishedAt) { <span class="meta">Publicado el {{ shortDate(t.publishedAt) }}</span> }
            <span class="spacer"></span>
            <button type="button" class="btn-quiet" (click)="edit(t)">Editar</button>
            @if (t.status === 'PUBLISHED') {
              <button type="button" class="btn btn-line btn-sm" (click)="act(t, 'hide')">Ocultar</button>
            } @else {
              <button type="button" class="btn btn-ink btn-sm" (click)="act(t, 'publish')">Publicar</button>
            }
            <button type="button" class="btn btn-danger btn-sm" (click)="remove(t)">Borrar</button>
          </div>
        </div></div>
      } @empty { <p class="empty-note">Aún no hay testimonios.</p> }
    </main>
  `
})
export default class TestimonialsComponent implements OnInit {
  private api = inject(ApiService);
  readonly items = signal<Testimonial[]>([]);
  readonly editing = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  draft = { displayName: '', quote: '' };
  readonly status = STATUS_LABEL;
  readonly photoLabel = PHOTO_LABEL;
  readonly shortDate = shortDate;

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.staffTestimonials().subscribe({ next: (t) => this.items.set(t), error: (e) => this.error.set(errorMessage(e)) });
  }

  edit(t: Testimonial) {
    this.draft = { displayName: t.displayName, quote: t.quote ?? '' };
    this.editing.set(t.id);
  }

  save(t: Testimonial) {
    this.api.saveTestimonial({
      patientId: t.patientId, displayName: this.draft.displayName.trim(), quote: this.draft.quote.trim() || null,
      treatmentId: t.treatmentId, photoIds: t.photos.map(p => p.sessionPhotoId)
    }, t.id).subscribe({ next: () => { this.editing.set(null); this.load(); }, error: (e) => this.error.set(errorMessage(e)) });
  }

  act(t: Testimonial, action: 'publish' | 'hide') {
    this.error.set(null);
    this.api.testimonialAction(t.id, action).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(e?.error?.code === 'CONSENT_REQUIRED'
        ? 'La paciente no ha autorizado publicar sus fotos. Pídele que lo haga en «Mi historia» o regístralo en su ficha.'
        : errorMessage(e))
    });
  }

  remove(t: Testimonial) {
    if (!confirm('¿Borrar este testimonio? Las fotos siguen en la historia de la paciente.')) return;
    this.api.deleteTestimonial(t.id).subscribe({ next: () => this.load(), error: (e) => this.error.set(errorMessage(e)) });
  }
}
