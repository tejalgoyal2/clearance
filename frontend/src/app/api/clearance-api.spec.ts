import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClearanceApi } from './clearance-api';

// HttpTestingController catches every request, so these tests check the exact
// URL, method and body without a server.
describe('ClearanceApi', () => {
  let api: ClearanceApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ClearanceApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify()); // fails if a test left a request unanswered

  it('lists scans', () => {
    let result: unknown;
    api.listScans().subscribe((scans) => (result = scans));

    http.expectOne({ method: 'GET', url: '/api/scans' }).flush([{ id: 1 }]);

    expect(result).toEqual([{ id: 1 }]);
  });

  it('uploads a scan as multipart form data', () => {
    const file = new File(['<project/>'], 'pom.xml');
    api.createScan('demo', file).subscribe();

    const request = http.expectOne({ method: 'POST', url: '/api/scans' });
    const body = request.request.body as FormData;
    expect(body.get('projectName')).toBe('demo');
    expect((body.get('file') as File).name).toBe('pom.xml');
    request.flush({ id: 9 });
  });

  it('filters components by status only when one is given', () => {
    api.getComponents(3, 'DENIED').subscribe();
    api.getComponents(3, null).subscribe();

    http.expectOne('/api/scans/3/components?status=DENIED').flush([]);
    http.expectOne('/api/scans/3/components').flush([]);
  });

  it('posts a decision', () => {
    api.decide(5, { decision: 'ACCEPTED', reviewer: 'tejal', comment: 'ok' }).subscribe();

    const request = http.expectOne({ method: 'POST', url: '/api/components/5/decision' });
    expect(request.request.body).toEqual({ decision: 'ACCEPTED', reviewer: 'tejal', comment: 'ok' });
    request.flush({});
  });

  it('reads the audit trail and the policy', () => {
    api.getAuditTrail(3).subscribe();
    api.getPolicy().subscribe();

    http.expectOne('/api/scans/3/audit').flush([]);
    http.expectOne('/api/policy').flush({});
  });

  it('builds the report download link', () => {
    expect(api.reportUrl(3)).toBe('/api/scans/3/report.csv');
  });
});
