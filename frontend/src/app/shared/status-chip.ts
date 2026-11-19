import { Component, computed, input } from '@angular/core';
import { PolicyStatus } from '../api/models';

/** A policy verdict as a small coloured label. Colour is only ever used for these four meanings. */
@Component({
  selector: 'app-status-chip',
  template: `<span [class]="'chip chip--' + status().toLowerCase()">{{ label() }}</span>`,
})
export class StatusChip {
  readonly status = input.required<PolicyStatus>();

  /** "DENIED" becomes "Denied". */
  protected readonly label = computed(() => {
    const status = this.status();
    return status.charAt(0) + status.slice(1).toLowerCase();
  });
}
