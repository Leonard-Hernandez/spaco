import { computed, Injectable, signal } from '@angular/core';
import { Staff } from '../../core/api/models';

/** Who is using the panel, and what they may open. */
@Injectable()
export class StaffContext {
  readonly staff = signal<Staff | null>(null);
  readonly role = computed(() => this.staff()?.role ?? null);
  readonly isOwner = computed(() => this.role() === 'OWNER');
  /** Clinical records, photos, testimonials and the assistant: owner and estheticians. */
  readonly clinical = computed(() => this.role() === 'OWNER' || this.role() === 'ESTHETICIAN');
  /** Requests waiting for an answer, for the tab badge. */
  readonly pendingCount = signal(0);
}
