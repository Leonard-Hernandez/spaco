import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Member, Professional, Role } from '../../../core/api/models';

const ROLES: { id: Role; label: string; hint: string }[] = [
  { id: 'OWNER', label: 'Dueña', hint: 'Todo, incluido catálogo, horario y equipo' },
  { id: 'ESTHETICIAN', label: 'Esteticista', hint: 'Agenda, historias clínicas, fotos, testimonios y asistente IA' },
  { id: 'RECEPTION', label: 'Recepción', hint: 'Solicitudes, agenda y datos de contacto; sin historias clínicas' }
];

/** Who performs treatments (shown when booking) and who can enter the panel (by Google email). */
@Component({
  selector: 'app-admin-team',
  imports: [FormsModule],
  template: `
    <main class="wrap page">
      <h1 class="page-h1">Equipo</h1>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }
      <div class="sheet cut"><div class="sheet-in cut">
        <h2 class="sheet-h2">Profesionales</h2>
        <p class="hint">Aparecen al agendar («¿Con quién?») y tienen su propio horario.</p>
        <div class="scroll-x"><table class="tbl">
          <thead><tr><th>Nombre</th><th>Cargo</th><th>Cuenta del panel</th><th>Activa</th><th></th></tr></thead>
          <tbody>
            @for (p of professionals(); track p.id) {
              <tr>
                <td><input class="ctl" [(ngModel)]="p.displayName" [name]="'pn' + p.id" aria-label="Nombre" /></td>
                <td><input class="ctl" [(ngModel)]="p.title" [name]="'pt' + p.id" aria-label="Cargo" /></td>
                <td><select class="ctl" [(ngModel)]="p.memberId" [name]="'pm' + p.id" aria-label="Cuenta"><option [ngValue]="null">—</option>@for (m of members(); track m.id) { <option [ngValue]="m.id">{{ m.name ?? m.email }}</option> }</select></td>
                <td><input type="checkbox" [(ngModel)]="p.active" [name]="'pa' + p.id" aria-label="Activa" /></td>
                <td><button type="button" class="btn btn-line btn-sm" (click)="saveProfessional(p)">Guardar</button></td>
              </tr>
            }
            <tr>
              <td><input class="ctl" placeholder="Nombre" [(ngModel)]="newPro.displayName" name="npn" aria-label="Nombre" /></td>
              <td><input class="ctl" placeholder="Esteticista" [(ngModel)]="newPro.title" name="npt" aria-label="Cargo" /></td>
              <td></td><td></td>
              <td><button type="button" class="btn btn-ink btn-sm" (click)="addProfessional()">Agregar</button></td>
            </tr>
          </tbody>
        </table></div>
      </div></div>

      <div class="sheet cut"><div class="sheet-in cut">
        <h2 class="sheet-h2">Acceso al panel</h2>
        <ul class="list">
          @for (m of members(); track m.id) {
            <li><div class="item-top">
              <span>{{ m.name ?? m.email }} <span class="meta">{{ m.email }}{{ m.linked ? '' : ' · invitación pendiente' }}</span></span>
              <span class="row">
                <select class="ctl" style="width:auto" [ngModel]="m.role" (ngModelChange)="changeRole(m, $event)" [name]="'mr' + m.id" aria-label="Rol">@for (r of roles; track r.id) { <option [value]="r.id">{{ r.label }}</option> }</select>
                <button type="button" class="btn-quiet" (click)="remove(m)">Quitar</button>
              </span>
            </div></li>
          }
        </ul>
        <form class="row" (ngSubmit)="invite()">
          <input class="ctl" style="max-width:320px" type="email" name="ie" placeholder="correo@gmail.com" [(ngModel)]="inviteEmail" aria-label="Correo de Google" required />
          <select class="ctl" style="width:auto" name="ir" [(ngModel)]="inviteRole" aria-label="Rol">@for (r of roles; track r.id) { <option [value]="r.id">{{ r.label }}</option> }</select>
          <button type="submit" class="btn btn-ink btn-sm">Invitar</button>
        </form>
        <dl class="kv">@for (r of roles; track r.id) { <dt>{{ r.label }}</dt><dd>{{ r.hint }}</dd> }</dl>
      </div></div>
    </main>
  `
})
export default class AdminTeamComponent implements OnInit {
  private api = inject(ApiService);
  readonly professionals = signal<Professional[]>([]);
  readonly members = signal<Member[]>([]);
  readonly error = signal<string | null>(null);
  readonly roles = ROLES;
  newPro = { displayName: '', title: 'Esteticista' };
  inviteEmail = '';
  inviteRole: Role = 'ESTHETICIAN';

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.staffProfessionals().subscribe(p => this.professionals.set(p));
    this.api.members().subscribe(m => this.members.set(m));
  }

  saveProfessional(p: Professional) {
    this.run(this.api.saveProfessional({ displayName: p.displayName, title: p.title, bio: p.bio, memberId: p.memberId, active: p.active }, p.id));
  }

  addProfessional() {
    if (!this.newPro.displayName.trim()) return;
    this.run(this.api.saveProfessional({ displayName: this.newPro.displayName.trim(), title: this.newPro.title, bio: null, memberId: null, active: true }),
      () => this.newPro = { displayName: '', title: 'Esteticista' });
  }

  invite() {
    this.run(this.api.inviteMember(this.inviteEmail.trim(), this.inviteRole), () => this.inviteEmail = '');
  }

  changeRole(m: Member, role: Role) {
    this.run(this.api.changeRole(m.id, role));
  }

  remove(m: Member) {
    if (confirm(`¿Quitar a ${m.email} del panel?`)) this.run(this.api.removeMember(m.id));
  }

  private run(request: import('rxjs').Observable<unknown>, done?: () => void) {
    this.error.set(null);
    request.subscribe({ next: () => { done?.(); this.load(); }, error: (e) => { this.error.set(errorMessage(e)); this.load(); } });
  }
}
