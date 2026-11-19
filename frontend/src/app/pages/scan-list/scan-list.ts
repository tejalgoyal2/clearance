import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClearanceApi } from '../../api/clearance-api';
import { describeError } from '../../api/errors';
import { POLICY_STATUSES, ScanSummary } from '../../api/models';

@Component({
  selector: 'app-scan-list',
  imports: [RouterLink, DatePipe],
  templateUrl: './scan-list.html',
})
export class ScanList {
  private readonly api = inject(ClearanceApi);

  protected readonly statuses = POLICY_STATUSES;
  /** null while loading. */
  protected readonly scans = signal<ScanSummary[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.listScans().subscribe({
      next: (scans) => this.scans.set(scans),
      error: (e) => this.error.set(describeError(e)),
    });
  }
}
