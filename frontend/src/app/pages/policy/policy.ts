import { KeyValuePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ClearanceApi } from '../../api/clearance-api';
import { describeError } from '../../api/errors';
import { Policy } from '../../api/models';
import { StatusChip } from '../../shared/status-chip';

@Component({
  selector: 'app-policy',
  imports: [KeyValuePipe, StatusChip],
  templateUrl: './policy.html',
})
export class PolicyPage {
  protected readonly policy = signal<Policy | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    inject(ClearanceApi).getPolicy().subscribe({
      next: (policy) => this.policy.set(policy),
      error: (e) => this.error.set(describeError(e)),
    });
  }
}
