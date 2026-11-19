import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { throwError } from 'rxjs';
import { ClearanceApi } from '../../api/clearance-api';
import { fakeApi, text } from '../../testing';
import { NewScan } from './new-scan';

describe('NewScan', () => {
  async function render(api = fakeApi()) {
    TestBed.configureTestingModule({
      imports: [NewScan],
      providers: [provideRouter([]), { provide: ClearanceApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(NewScan);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;

    const fill = async (name: string, file: File | null) => {
      const nameInput = page.querySelector('input[name="projectName"]') as HTMLInputElement;
      nameInput.value = name;
      nameInput.dispatchEvent(new Event('input'));
      if (file) {
        const fileInput = page.querySelector('input[type="file"]') as HTMLInputElement;
        Object.defineProperty(fileInput, 'files', { value: [file] });
        fileInput.dispatchEvent(new Event('change'));
      }
      await fixture.whenStable();
    };
    const submit = page.querySelector('button[type="submit"]') as HTMLButtonElement;
    return { page, fill, submit, fixture };
  }

  const pom = new File(['<project/>'], 'pom.xml');

  it('keeps Run scan disabled until there is a name and a file', async () => {
    const { fill, submit } = await render();
    expect(submit.disabled).toBe(true);

    await fill('demo', null);
    expect(submit.disabled).toBe(true);

    await fill('demo', pom);
    expect(submit.disabled).toBe(false);
  });

  it('uploads and opens the new scan', async () => {
    const api = fakeApi();
    const { fill, submit, fixture } = await render(api);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    await fill('demo', pom);
    submit.click();
    await fixture.whenStable();

    expect(api.createScan).toHaveBeenCalledWith('demo', pom);
    expect(navigate).toHaveBeenCalledWith(['/scans', 7]);
  });

  it('shows the server error message', async () => {
    const api = fakeApi();
    api.createScan.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { message: "Unsupported file 'build.gradle'" },
    })));
    const { page, fill, submit, fixture } = await render(api);

    await fill('demo', pom);
    submit.click();
    await fixture.whenStable();

    expect(text(page)).toContain("Unsupported file 'build.gradle'");
  });
});
