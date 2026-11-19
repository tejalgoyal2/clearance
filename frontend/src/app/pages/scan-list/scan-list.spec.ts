import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ClearanceApi } from '../../api/clearance-api';
import { fakeApi, SCAN, text } from '../../testing';
import { ScanList } from './scan-list';

describe('ScanList', () => {
  async function render(api = fakeApi()) {
    TestBed.configureTestingModule({
      imports: [ScanList],
      providers: [provideRouter([]), { provide: ClearanceApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(ScanList);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows one row per scan with its counts', async () => {
    const page = await render();

    const rows = page.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(text(rows[0] as HTMLElement)).toContain('demo-app');
    expect(rows[0].querySelector('a')?.getAttribute('href')).toBe('/scans/7');
    expect(text(rows[0] as HTMLElement)).toContain('1 denied');
  });

  it('has a New scan button', async () => {
    const page = await render();

    expect(page.querySelector('a[href="/scans/new"]')).not.toBeNull();
  });

  it('says so when there are no scans yet', async () => {
    const api = fakeApi();
    api.listScans.mockReturnValue(of([]));

    const page = await render(api);

    expect(text(page)).toContain('No scans yet');
  });
});
