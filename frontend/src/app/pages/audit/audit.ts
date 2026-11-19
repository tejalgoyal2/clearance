import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClearanceApi } from '../../api/clearance-api';
import { describeError } from '../../api/errors';
import { AuditAction, AuditEvent } from '../../api/models';

const ACTION_LABELS: Record<AuditAction, string> = {
  SCAN_CREATED: 'Scan created',
  DECISION_MADE: 'Decision made',
  REPORT_EXPORTED: 'Report exported',
};

@Component({
  selector: 'app-audit',
  imports: [RouterLink, DatePipe],
  templateUrl: './audit.html',
})
export class Audit {
  private readonly api = inject(ClearanceApi);

  protected readonly scanId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  protected readonly events = signal<AuditEvent[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly labels = ACTION_LABELS;

  constructor() {
    this.api.getAuditTrail(this.scanId).subscribe({
      next: (events) => this.events.set(events),
      error: (e) => this.error.set(describeError(e)),
    });
  }
}
