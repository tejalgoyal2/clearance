import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ClearanceApi } from '../../api/clearance-api';
import { describeError } from '../../api/errors';

@Component({
  selector: 'app-new-scan',
  imports: [RouterLink],
  templateUrl: './new-scan.html',
})
export class NewScan {
  private readonly api = inject(ClearanceApi);
  private readonly router = inject(Router);

  protected readonly projectName = signal('');
  protected readonly file = signal<File | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly canSubmit = computed(
    () => this.projectName().trim().length > 0 && this.file() !== null && !this.busy(),
  );

  protected onNameInput(event: Event): void {
    this.projectName.set((event.target as HTMLInputElement).value);
  }

  protected onFileChange(event: Event): void {
    const files = (event.target as HTMLInputElement).files;
    this.file.set(files && files.length > 0 ? files[0] : null);
  }

  protected submit(event: Event): void {
    event.preventDefault(); // stay on this page; the upload happens over HttpClient
    const file = this.file();
    if (!this.canSubmit() || file === null) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.api.createScan(this.projectName().trim(), file).subscribe({
      next: (scan) => this.router.navigate(['/scans', scan.id]),
      error: (e) => {
        this.error.set(describeError(e));
        this.busy.set(false);
      },
    });
  }
}
