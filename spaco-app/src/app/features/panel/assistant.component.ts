import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { NoteMatch } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { appConfig } from '../../core/config/app-config';
import { shortDate } from '../../shared/format';

/**
 * How to ask questions in natural language: connect Claude (or any MCP client) to the spa's
 * MCP server with the staff Google account. Also a plain full-text search over the notes.
 */
@Component({
  selector: 'app-assistant',
  imports: [FormsModule, RouterLink],
  template: `
    <main class="wrap page" style="max-width:980px">
      <h1 class="page-h1">Asistente IA</h1>
      <div class="sheet cut"><div class="sheet-in cut">
        <h2 class="sheet-h2">Pregúntale a tu asistente por tus pacientes</h2>
        <p style="margin:0">Conecta Claude a los registros del spa y pregunta en lenguaje natural: «¿Qué alergias tiene Laura Gómez?», «¿Qué productos le usé a Marta en las últimas tres sesiones?», «¿Quién viene mañana y tiene alguna alerta?», «¿Puedo hacerle peeling a Ana con su historia?».</p>
        <h3 class="sheet-h3">Dirección del servidor MCP</h3>
        <div class="mcp-url">
          <code>{{ mcpUrl }}</code>
          <button type="button" class="btn btn-line btn-sm" (click)="copy()">{{ copied() ? 'Copiada' : 'Copiar' }}</button>
        </div>
        <h3 class="sheet-h3">Cómo conectarlo en Claude</h3>
        <ol style="margin:0;padding-left:22px;line-height:1.7">
          <li>En claude.ai abre <strong>Configuración → Conectores → Agregar conector personalizado</strong>.</li>
          <li>Pega la dirección de arriba y ponle un nombre, por ejemplo «{{ tenant.tenant()?.name }}».</li>
          <li>Al conectar se abre el inicio de sesión del spa: entra con la misma cuenta de Google que usas en este panel.</li>
          <li>En un chat nuevo, activa el conector y pregunta.</li>
        </ol>
        <p class="hint">Solo la dueña y las esteticistas pueden usarlo. El asistente solo lee: no cambia nada. Cada consulta queda en el registro de accesos de la paciente, y ella puede verlo.</p>
      </div></div>

      <div class="sheet cut"><div class="sheet-in cut">
        <h2 class="sheet-h2">Buscar en las notas de sesión</h2>
        <form class="row" (ngSubmit)="search()">
          <input class="ctl" style="max-width:420px" name="q" placeholder="Ej. enrojecimiento, ácido hialurónico" [(ngModel)]="q" aria-label="Buscar en notas" />
          <button type="submit" class="btn btn-ink">Buscar</button>
        </form>
        @if (error()) { <p class="error-msg">{{ error() }}</p> }
        @if (results(); as rs) {
          <ul class="list">
            @for (r of rs; track r.sessionNoteId) {
              <li><div class="item-top"><a class="link" [routerLink]="['/panel/pacientes', r.patientId]">{{ r.patientName }}</a><span class="meta">{{ shortDate(r.performedAt) }} · {{ r.treatmentName }}</span></div>
                <span>{{ r.excerpt }}</span></li>
            } @empty { <li>Sin resultados.</li> }
          </ul>
        }
      </div></div>
    </main>
  `
})
export default class AssistantComponent {
  private api = inject(ApiService);
  readonly tenant = inject(TenantService);
  readonly mcpUrl = `${window.location.origin}${appConfig().apiUrl}/mcp/${appConfig().tenant}`;
  readonly copied = signal(false);
  readonly results = signal<NoteMatch[] | null>(null);
  readonly error = signal<string | null>(null);
  q = '';
  readonly shortDate = shortDate;

  copy() {
    navigator.clipboard?.writeText(this.mcpUrl).then(() => this.copied.set(true));
  }

  search() {
    if (!this.q.trim()) return;
    this.error.set(null);
    this.api.searchNotes(this.q.trim()).subscribe({ next: (r) => this.results.set(r), error: (e) => this.error.set(errorMessage(e)) });
  }
}
