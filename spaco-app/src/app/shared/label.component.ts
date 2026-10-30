import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { inkStyle } from './ink';

/**
 * Apothecary label: nested die-cut layers (ink, wax, navy, wax...) with a band on top. size
 * picks the layer recipe of the design (hero, shelf, summary); the body is projected.
 */
@Component({
  selector: 'app-label',
  template: `
    @switch (size()) {
      @case ('hero') {
        <div class="cut ly bg-ink hl1" [attr.style]="style()"><div class="cut ly bg-wax hl2"><div class="cut ly bg-navy hl3"><div class="cut ly bg-wax hl4"><div class="cut ly bg-navy hl5"><div class="cut ly bg-wax col hl6">
          <div class="band hero-band"><span class="num">{{ left() }}</span><span>{{ right() }}</span></div>
          <ng-container *ngTemplateOutlet="body" />
        </div></div></div></div></div></div>
      }
      @case ('summary') {
        <div class="cut ly bg-ink sum1" [attr.style]="style()"><div class="cut ly bg-wax sum2"><div class="cut ly bg-navy sum3"><div class="cut ly bg-wax col sum4">
          <div class="band sum-band"><span class="num">{{ left() }}</span><span>{{ right() }}</span></div>
          <ng-container *ngTemplateOutlet="body" />
        </div></div></div></div>
      }
      @default {
        <div class="cut ly bg-ink sf1" [attr.style]="style()"><div class="cut ly bg-wax sf2"><div class="cut ly bg-navy sf3"><div class="cut ly bg-wax sf4"><div class="cut ly bg-navy sf5"><div class="cut ly bg-wax col sf6">
          <div class="band lab-band"><span class="num">{{ left() }}</span><span>{{ right() }}</span></div>
          <ng-container *ngTemplateOutlet="body" />
        </div></div></div></div></div></div>
      }
    }
    <ng-template #body><ng-content /></ng-template>
  `,
  imports: [NgTemplateOutlet],
  styles: `:host { display: flex; min-width: 0; }`
})
export class LabelComponent {
  readonly ink = input<string | null>('honey');
  readonly left = input('');
  readonly right = input('');
  readonly size = input<'hero' | 'shelf' | 'summary'>('shelf');
  readonly style = computed(() => inkStyle(this.ink()));
}
