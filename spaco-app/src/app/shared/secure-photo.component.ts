import { Component, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../core/api/api.service';

/**
 * Clinical photo. It needs the access token and X-Tenant, which an img tag cannot send, so it
 * is fetched as a blob and shown from an object URL (released when the component goes away).
 */
@Component({
  selector: 'app-secure-photo',
  template: `
    @if (url(); as u) { <img [src]="u" [alt]="alt()" loading="lazy" /> }
    @else if (failed()) { <span class="meta" style="display:block;padding:12px">No se pudo cargar</span> }
    @else { <span class="meta" style="display:block;aspect-ratio:1;padding:12px">Cargando…</span> }
  `,
  styles: `:host { display: block; }`
})
export class SecurePhotoComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  readonly photoId = input.required<number>();
  /** portal = the patient's own endpoint */
  readonly source = input<'panel' | 'portal'>('panel');
  readonly alt = input('');
  readonly url = signal<string | null>(null);
  readonly failed = signal(false);

  ngOnInit() {
    const request: Observable<Blob> = this.source() === 'portal'
      ? this.api.myPhoto(this.photoId())
      : this.api.photo(this.photoId());
    request.subscribe({
      next: (blob) => this.url.set(URL.createObjectURL(blob)),
      error: () => this.failed.set(true)
    });
  }

  ngOnDestroy() {
    const u = this.url();
    if (u) URL.revokeObjectURL(u);
  }
}
