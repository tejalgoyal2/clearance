import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClearanceApi } from '../../api/clearance-api';
import { describeError } from '../../api/errors';
import { ComponentView, POLICY_STATUSES, PolicyStatus, ScanSummary } from '../../api/models';
import { StatusChip } from '../../shared/status-chip';

/** The reviewer's choice while the dialog is open. */
type DecisionChoice = 'ACCEPTED' | 'REJECTED';

@Component({
  selector: 'app-scan-detail',
  imports: [RouterLink, DatePipe, StatusChip],
  templateUrl: './scan-detail.html',
})
export class ScanDetail {
  private readonly api = inject(ClearanceApi);

  protected readonly scanId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  protected readonly statuses = POLICY_STATUSES;
  protected readonly reportUrl = this.api.reportUrl(this.scanId);

  protected readonly scan = signal<ScanSummary | null>(null);
  protected readonly components = signal<ComponentView[] | null>(null);
  /** null means "All". */
  protected readonly filter = signal<PolicyStatus | null>(null);
  protected readonly error = signal<string | null>(null);

  // Decision dialog state. The dialog is open while `reviewing` is not null.
  protected readonly reviewing = signal<ComponentView | null>(null);
  protected readonly choice = signal<DecisionChoice | null>(null);
  protected readonly reviewer = signal('');
  protected readonly comment = signal('');
  protected readonly saving = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  constructor() {
    this.api.getScan(this.scanId).subscribe({
      next: (scan) => this.scan.set(scan),
      error: (e) => this.error.set(describeError(e)),
    });
    this.loadComponents();
  }

  protected setFilter(status: PolicyStatus | null): void {
    this.filter.set(status);
    this.loadComponents();
  }

  private loadComponents(): void {
    this.api.getComponents(this.scanId, this.filter()).subscribe({
      next: (components) => this.components.set(components),
      error: (e) => this.error.set(describeError(e)),
    });
  }

  protected openReview(component: ComponentView): void {
    this.reviewing.set(component);
    this.choice.set(null);
    this.comment.set('');
    this.dialogError.set(null);
    // The reviewer name is kept, so one person can review several rows quickly.
  }

  protected closeReview(): void {
    this.reviewing.set(null);
  }

  protected canSave(): boolean {
    return this.choice() !== null && this.reviewer().trim() !== '' && this.comment().trim() !== '' && !this.saving();
  }

  protected saveDecision(event: Event): void {
    event.preventDefault();
    const component = this.reviewing();
    const choice = this.choice();
    if (component === null || choice === null || !this.canSave()) {
      return;
    }
    this.saving.set(true);
    this.dialogError.set(null);
    this.api.decide(component.id, {
      decision: choice,
      reviewer: this.reviewer().trim(),
      comment: this.comment().trim(),
    }).subscribe({
      next: (updated) => {
        // Replace just this row; a new array so the signal notices the change.
        this.components.update((rows) => rows?.map((row) => (row.id === updated.id ? updated : row)) ?? null);
        this.saving.set(false);
        this.reviewing.set(null);
      },
      error: (e) => {
        this.dialogError.set(describeError(e));
        this.saving.set(false);
      },
    });
  }

  protected inputValue(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected label(status: PolicyStatus): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }
}
