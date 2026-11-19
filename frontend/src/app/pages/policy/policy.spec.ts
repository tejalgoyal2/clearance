import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ClearanceApi } from '../../api/clearance-api';
import { fakeApi, text } from '../../testing';
import { PolicyPage } from './policy';

describe('PolicyPage', () => {
  it('lists every licence under its verdict', async () => {
    const api = fakeApi();
    api.getPolicy.mockReturnValue(of({
      approved: ['MIT'], review: ['MPL-2.0'], denied: ['GPL-3.0-only'],
      aliases: { 'Apache 2.0': 'Apache-2.0' },
      exceptions: { 'GPL-2.0-only WITH Classpath-exception-2.0': 'REVIEW' },
    }));
    TestBed.configureTestingModule({
      imports: [PolicyPage],
      providers: [{ provide: ClearanceApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(PolicyPage);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;

    expect(text(page.querySelector('[data-list="APPROVED"]') as HTMLElement)).toContain('MIT');
    expect(text(page.querySelector('[data-list="DENIED"]') as HTMLElement)).toContain('GPL-3.0-only');
    expect(text(page)).toContain('Apache 2.0');
    expect(text(page)).toContain('Classpath-exception-2.0');
  });
});
