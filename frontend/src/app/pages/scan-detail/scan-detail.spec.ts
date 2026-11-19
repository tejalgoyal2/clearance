import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ClearanceApi } from '../../api/clearance-api';
import { component, fakeApi, text } from '../../testing';
import { ScanDetail } from './scan-detail';

describe('ScanDetail', () => {
  const lodash = component({ id: 1 });
  const gplLib = component({
    id: 2, name: 'gpl-lib', rawLicense: 'GPL-3.0', spdxExpression: 'GPL-3.0-only', policyStatus: 'DENIED',
  });

  async function render(api = fakeApi()) {
    api.getComponents.mockReturnValue(of([lodash, gplLib]));
    TestBed.configureTestingModule({
      imports: [ScanDetail],
      providers: [
        provideRouter([]),
        { provide: ClearanceApi, useValue: api },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
      ],
    });
    const fixture = TestBed.createComponent(ScanDetail);
    await fixture.whenStable();
    return { page: fixture.nativeElement as HTMLElement, fixture, api };
  }

  it('shows the counts and every component', async () => {
    const { page } = await render();

    expect(text(page)).toContain('demo-app');
    expect(page.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('shows the licence as written next to its normalized form and verdict', async () => {
    const { page } = await render();

    const row = text(page.querySelectorAll('tbody tr')[1] as HTMLElement);
    expect(row).toContain('GPL-3.0');
    expect(row).toContain('GPL-3.0-only');
    expect(row).toContain('Denied');
  });

  it('filters by status when a chip is clicked', async () => {
    const { page, fixture, api } = await render();

    const deniedChip = page.querySelector('[data-filter="DENIED"]') as HTMLButtonElement;
    deniedChip.click();
    await fixture.whenStable();

    expect(api.getComponents).toHaveBeenLastCalledWith(7, 'DENIED');
  });

  it('links to the audit trail and the CSV report', async () => {
    const { page } = await render();

    expect(page.querySelector('a[href="/scans/7/audit"]')).not.toBeNull();
    expect(page.querySelector('a[href="/api/scans/7/report.csv"]')).not.toBeNull();
  });

  it('records a decision from the dialog and updates the row', async () => {
    const api = fakeApi();
    api.decide.mockReturnValue(of({ ...gplLib, decision: 'ACCEPTED', decidedBy: 'tejal', decisionComment: 'internal' }));
    const { page, fixture } = await render(api);

    (page.querySelector('[data-review="2"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    const dialog = page.querySelector('[role="dialog"]') as HTMLElement;
    expect(text(dialog)).toContain('gpl-lib');

    (dialog.querySelector('input[value="ACCEPTED"]') as HTMLInputElement).click();
    const reviewer = dialog.querySelector('input[name="reviewer"]') as HTMLInputElement;
    reviewer.value = 'tejal';
    reviewer.dispatchEvent(new Event('input'));
    const comment = dialog.querySelector('textarea') as HTMLTextAreaElement;
    comment.value = 'internal';
    comment.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    (dialog.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(api.decide).toHaveBeenCalledWith(2, { decision: 'ACCEPTED', reviewer: 'tejal', comment: 'internal' });
    expect(page.querySelector('[role="dialog"]')).toBeNull();
    expect(text(page.querySelectorAll('tbody tr')[1] as HTMLElement)).toContain('Accepted by tejal');
  });

  it('keeps the dialog open and shows the error when the decision fails', async () => {
    const api = fakeApi();
    api.decide.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { message: 'Component 2 was already ACCEPTED' },
    })));
    const { page, fixture } = await render(api);

    (page.querySelector('[data-review="2"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    const dialog = page.querySelector('[role="dialog"]') as HTMLElement;
    (dialog.querySelector('input[value="REJECTED"]') as HTMLInputElement).click();
    for (const [selector, value] of [['input[name="reviewer"]', 'tejal'], ['textarea', 'no']]) {
      const field = dialog.querySelector(selector) as HTMLInputElement;
      field.value = value;
      field.dispatchEvent(new Event('input'));
    }
    await fixture.whenStable();
    (dialog.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(text(page.querySelector('[role="dialog"]') as HTMLElement)).toContain('already ACCEPTED');
  });
});
