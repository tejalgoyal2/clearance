import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ClearanceApi } from '../../api/clearance-api';
import { fakeApi, text } from '../../testing';
import { Audit } from './audit';

describe('Audit', () => {
  it('shows who did what, and when, in order', async () => {
    const api = fakeApi();
    api.getAuditTrail.mockReturnValue(of([
      { id: 1, componentId: null, actor: 'anonymous', action: 'SCAN_CREATED', detail: 'Scanned pom.xml', timestamp: '2026-11-19T10:00:00Z' },
      { id: 2, componentId: 2, actor: 'tejal', action: 'DECISION_MADE', detail: 'ACCEPTED gpl-lib', timestamp: '2026-11-19T10:05:00Z' },
    ]));
    TestBed.configureTestingModule({
      imports: [Audit],
      providers: [
        provideRouter([]),
        { provide: ClearanceApi, useValue: api },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
      ],
    });
    const fixture = TestBed.createComponent(Audit);
    await fixture.whenStable();

    const entries = (fixture.nativeElement as HTMLElement).querySelectorAll('li');
    expect(entries.length).toBe(2);
    expect(text(entries[1] as HTMLElement)).toContain('tejal');
    expect(text(entries[1] as HTMLElement)).toContain('Decision made');
    expect(text(entries[1] as HTMLElement)).toContain('ACCEPTED gpl-lib');
  });
});
