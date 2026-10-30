import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Patient } from '../../core/api/models';
import { phone, shortDate } from '../../shared/format';

/** Patients of the spa: search (accents and typos ok) and register walk-ins. */
@Component({
  selector: 'app-patients',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="wrap page">
      <div class="row">
        <h1 class="page-h1">Pacientes</h1>
        <span class="spacer"></span>
        <button type="button" class="btn btn-ink" (click)="creating.set(!creating())">{{ creating() ? 'Cerrar' : 'Nueva paciente' }}</button>
      </div>
      @if (creating()) {
        <form class="sheet cut" (ngSubmit)="create()"><div class="sheet-in cut">
          <h2 class="sheet-h2">Nueva paciente</h2>
          <div class="grid-2">
            <div class="field span2"><label class="label-sm" for="n">Nombre completo</label><input id="n" name="n" class="ctl" required [(ngModel)]="form.fullName" /></div>
            <div class="field"><label class="label-sm" for="p">Celular</label><input id="p" name="p" class="ctl num" type="tel" required [(ngModel)]="form.phone" /></div>
            <div class="field"><label class="label-sm" for="e">Correo (para su historia en línea)</label><input id="e" name="e" class="ctl" type="email" [(ngModel)]="form.email" /></div>
            <div class="field"><label class="label-sm" for="b">Fecha de nacimiento</label><input id="b" name="b" class="ctl num" type="date" [(ngModel)]="form.birthDate" /></div>
          </div>
          @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
          <div class="row-end"><button type="submit" class="btn btn-ink">Guardar</button></div>
        </div></form>
      }
      <div class="search-box" style="max-width:520px">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7"></circle><path d="M20 20l-3.5-3.5"></path></svg>
        <input type="search" class="ctl" placeholder="Nombre, celular o correo" aria-label="Buscar paciente" [ngModel]="q" (ngModelChange)="search($event)" />
      </div>
      <div class="sheet cut"><div class="sheet-in cut">
        <div class="scroll-x"><table class="tbl">
          <thead><tr><th>Paciente</th><th>Celular</th><th>Correo</th><th>Historia en línea</th><th>Desde</th></tr></thead>
          <tbody>
            @for (p of patients(); track p.id) {
              <tr>
                <td><a class="link" [routerLink]="[p.id]">{{ p.fullName }}</a></td>
                <td class="num">{{ phone(p.phone) }}</td>
                <td>{{ p.email ?? '—' }}</td>
                <td>{{ p.portalLinked ? 'Activa' : (p.email ? 'Puede entrar' : 'Sin correo') }}</td>
                <td class="num">{{ shortDate(p.createdAt) }}</td>
              </tr>
            } @empty {
              <tr><td colspan="5">Sin resultados.</td></tr>
            }
          </tbody>
        </table></div>
      </div></div>
    </main>
  `
})
export default class PatientsComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  readonly patients = signal<Patient[]>([]);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);
  private queries = new Subject<string>();
  q = '';
  form = { fullName: '', phone: '', email: '', birthDate: '' };

  readonly phone = phone;
  readonly shortDate = shortDate;

  ngOnInit() {
    this.queries.pipe(debounceTime(250), switchMap(q => this.api.searchPatients(q)))
      .subscribe(p => this.patients.set(p));
    this.queries.next('');
  }

  search(q: string) {
    this.q = q;
    this.queries.next(q.trim());
  }

  create() {
    this.error.set(null);
    this.api.savePatient({
      fullName: this.form.fullName.trim(), phone: this.form.phone, email: this.form.email.trim() || null,
      birthDate: this.form.birthDate || null
    }).subscribe({
      next: (p) => this.router.navigate(['/panel/pacientes', p.id]),
      error: (e) => this.error.set(errorMessage(e))
    });
  }
}
