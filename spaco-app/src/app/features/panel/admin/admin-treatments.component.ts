import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Category, Ink, Treatment, TreatmentInput } from '../../../core/api/models';
import { duration, formula, price } from '../../../shared/format';

const INKS: { id: Ink; label: string }[] = [
  { id: 'honey', label: 'Miel' }, { id: 'lavender', label: 'Lavanda' }, { id: 'sage', label: 'Salvia' },
  { id: 'navy', label: 'Azul noche' }, { id: 'clay', label: 'Arcilla' }
];

interface Draft extends Omit<TreatmentInput, 'benefits' | 'composition'> {
  id?: number;
  benefits: string;
  composition: string;
}

/** Catalogue editor: categories (label color) and treatments with their description and photo. */
@Component({
  selector: 'app-admin-treatments',
  imports: [FormsModule],
  template: `
    <main class="wrap page">
      <h1 class="page-h1">Tratamientos</h1>
      @if (error()) { <p class="error-msg" role="alert">{{ error() }}</p> }

      @if (draft(); as d) {
        <form class="sheet cut" (ngSubmit)="save()"><div class="sheet-in cut">
          <h2 class="sheet-h2">{{ d.id ? 'Editar ' + d.name : 'Nuevo tratamiento' }}</h2>
          <div class="grid-2">
            <div class="field"><label class="label-sm" for="tn">Nombre</label><input id="tn" name="tn" class="ctl" required [(ngModel)]="d.name" /></div>
            <div class="field"><label class="label-sm" for="tc">Categoría</label>
              <select id="tc" name="tc" class="ctl" [(ngModel)]="d.categoryId">@for (c of categories(); track c.id) { <option [ngValue]="c.id">{{ c.name }}</option> }</select>
            </div>
            <div class="field span2"><label class="label-sm" for="tt">Frase corta (en la etiqueta)</label><input id="tt" name="tt" class="ctl" maxlength="200" [(ngModel)]="d.tagline" /></div>
            <div class="field span2"><label class="label-sm" for="td">Descripción para la paciente</label><textarea id="td" name="td" class="ctl" rows="4" [(ngModel)]="d.description"></textarea></div>
            <div class="field"><label class="label-sm" for="tb">Beneficios (uno por línea)</label><textarea id="tb" name="tb" class="ctl" rows="4" [(ngModel)]="d.benefits"></textarea></div>
            <div class="field"><label class="label-sm" for="tf">Fórmula (Etiqueta: valor, uno por línea)</label><textarea id="tf" name="tf" class="ctl" rows="4" placeholder="Base: miel de abejas" [(ngModel)]="d.composition"></textarea></div>
            <div class="field span2"><label class="label-sm" for="tx">Contraindicaciones</label><textarea id="tx" name="tx" class="ctl" rows="2" placeholder="Embarazo, rosácea activa, alergia a la miel…" [(ngModel)]="d.contraindications"></textarea>
              <span class="hint">Se muestran a la paciente y la esteticista (y el asistente) las cruzan con su historia.</span></div>
          </div>
          <div class="grid-3">
            <div class="field"><label class="label-sm" for="du">Duración (min)</label><input id="du" name="du" class="ctl num" type="number" min="5" [(ngModel)]="d.durationMin" /></div>
            <div class="field"><label class="label-sm" for="bu">Preparar sala (min)</label><input id="bu" name="bu" class="ctl num" type="number" min="0" [(ngModel)]="d.bufferMin" /></div>
            <div class="field"><label class="label-sm" for="pr">Precio (vacío = Consultar)</label><input id="pr" name="pr" class="ctl num" type="number" min="0" [(ngModel)]="d.price" /></div>
            <div class="field"><label class="label-sm" for="fo">Fórmula N.º</label><input id="fo" name="fo" class="ctl num" type="number" min="1" [(ngModel)]="d.formulaNo" /></div>
            <div class="field"><label class="label-sm" for="po">Orden</label><input id="po" name="po" class="ctl num" type="number" [(ngModel)]="d.position" /></div>
            <label class="check"><input type="checkbox" name="ac" [(ngModel)]="d.active" /> Visible en el sitio</label>
          </div>
          <div class="row-end"><button type="button" class="btn-quiet" (click)="draft.set(null)">Cancelar</button><button type="submit" class="btn btn-ink">Guardar</button></div>
        </div></form>
      }

      <div class="sheet cut"><div class="sheet-in cut">
        <div class="row"><h2 class="sheet-h2">Catálogo</h2><span class="spacer"></span><button type="button" class="btn btn-ink btn-sm" (click)="create()">Nuevo tratamiento</button></div>
        <div class="scroll-x"><table class="tbl">
          <thead><tr><th>N.º</th><th>Tratamiento</th><th>Categoría</th><th>Duración</th><th>Precio</th><th>Foto</th><th></th></tr></thead>
          <tbody>
            @for (t of treatments(); track t.id) {
              <tr [style.opacity]="t.active ? 1 : 0.55">
                <td class="num">{{ formula(t.formulaNo) }}</td>
                <td>{{ t.name }}{{ t.active ? '' : ' (oculto)' }}</td>
                <td>{{ t.categoryName }}</td>
                <td class="num">{{ duration(t.durationMin) }}</td>
                <td class="num">{{ price(t.price) }}</td>
                <td>
                  <label class="btn-quiet" style="cursor:pointer">{{ t.imageUrl ? 'Cambiar' : 'Subir' }}<input type="file" class="sr-only" accept="image/jpeg,image/png,image/webp" (change)="image(t, $event)" /></label>
                  @if (t.imageUrl) { <button type="button" class="btn-quiet" (click)="removeImage(t)">Quitar</button> }
                </td>
                <td><button type="button" class="btn btn-line btn-sm" (click)="edit(t)">Editar</button></td>
              </tr>
            }
          </tbody>
        </table></div>
      </div></div>

      <div class="sheet cut"><div class="sheet-in cut">
        <h2 class="sheet-h2">Categorías</h2>
        <div class="scroll-x"><table class="tbl">
          <thead><tr><th>Nombre</th><th>Color de etiqueta</th><th>Orden</th><th>Visible</th><th></th></tr></thead>
          <tbody>
            @for (c of categories(); track c.id) {
              <tr>
                <td><input class="ctl" [(ngModel)]="c.name" [name]="'cn' + c.id" aria-label="Nombre" /></td>
                <td><select class="ctl" [(ngModel)]="c.ink" [name]="'ci' + c.id" aria-label="Color">@for (i of inks; track i.id) { <option [value]="i.id">{{ i.label }}</option> }</select></td>
                <td><input class="ctl num" type="number" style="width:90px" [(ngModel)]="c.position" [name]="'cp' + c.id" aria-label="Orden" /></td>
                <td><input type="checkbox" [(ngModel)]="c.active" [name]="'ca' + c.id" aria-label="Visible" /></td>
                <td class="row"><button type="button" class="btn btn-line btn-sm" (click)="saveCategory(c)">Guardar</button><button type="button" class="btn-quiet" (click)="deleteCategory(c)">Borrar</button></td>
              </tr>
            }
            <tr>
              <td><input class="ctl" placeholder="Nueva categoría" [(ngModel)]="newCategory" name="nc" aria-label="Nueva categoría" /></td>
              <td><select class="ctl" [(ngModel)]="newInk" name="ni" aria-label="Color">@for (i of inks; track i.id) { <option [value]="i.id">{{ i.label }}</option> }</select></td>
              <td></td><td></td>
              <td><button type="button" class="btn btn-ink btn-sm" (click)="addCategory()">Agregar</button></td>
            </tr>
          </tbody>
        </table></div>
      </div></div>
    </main>
  `
})
export default class AdminTreatmentsComponent implements OnInit {
  private api = inject(ApiService);
  readonly categories = signal<Category[]>([]);
  readonly treatments = signal<Treatment[]>([]);
  readonly draft = signal<Draft | null>(null);
  readonly error = signal<string | null>(null);
  readonly inks = INKS;
  newCategory = '';
  newInk: Ink = 'honey';

  readonly formula = formula;
  readonly duration = duration;
  readonly price = price;

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.categories().subscribe(c => this.categories.set(c));
    this.api.adminTreatments().subscribe(t => this.treatments.set(t));
  }

  create() {
    this.draft.set({
      categoryId: this.categories()[0]?.id ?? 0, formulaNo: this.treatments().length + 1, name: '', tagline: null,
      description: null, benefits: '', composition: '', contraindications: null, durationMin: 60, bufferMin: 15,
      price: null, position: this.treatments().length + 1, active: true
    });
    window.scrollTo({ top: 0 });
  }

  edit(t: Treatment) {
    this.draft.set({
      id: t.id, categoryId: t.categoryId, formulaNo: t.formulaNo, name: t.name, tagline: t.tagline,
      description: t.description, benefits: t.benefits.join('\n'),
      composition: t.composition.map(c => `${c.label}: ${c.value}`).join('\n'),
      contraindications: t.contraindications, durationMin: t.durationMin, bufferMin: t.bufferMin, price: t.price,
      position: t.position, active: t.active
    });
    window.scrollTo({ top: 0 });
  }

  save() {
    const d = this.draft()!;
    const { id, benefits, composition, ...rest } = d;
    const input: TreatmentInput = {
      ...rest,
      price: (rest.price as unknown) === '' || rest.price == null ? null : Number(rest.price),
      benefits: benefits.split('\n').map(s => s.trim()).filter(Boolean),
      composition: composition.split('\n').map(line => line.split(':')).filter(p => p.length > 1 && p[0].trim())
        .map(([label, ...value]) => ({ label: label.trim(), value: value.join(':').trim() }))
    };
    this.run(this.api.saveTreatment(input, id), () => { this.draft.set(null); this.load(); });
  }

  image(t: Treatment, event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) this.run(this.api.treatmentImage(t.id, file), () => this.load());
  }

  removeImage(t: Treatment) {
    this.run(this.api.treatmentImage(t.id, null), () => this.load());
  }

  saveCategory(c: Category) {
    this.run(this.api.saveCategory({ name: c.name, ink: c.ink, position: c.position, active: c.active }, c.id), () => this.load());
  }

  deleteCategory(c: Category) {
    if (confirm(`¿Borrar la categoría ${c.name}?`)) this.run(this.api.deleteCategory(c.id), () => this.load());
  }

  addCategory() {
    if (!this.newCategory.trim()) return;
    this.run(this.api.saveCategory({ name: this.newCategory.trim(), ink: this.newInk, position: this.categories().length + 1, active: true }),
      () => { this.newCategory = ''; this.load(); });
  }

  private run<T>(request: import('rxjs').Observable<T>, done: (v: T) => void) {
    this.error.set(null);
    request.subscribe({ next: done, error: (e) => this.error.set(errorMessage(e)) });
  }
}
