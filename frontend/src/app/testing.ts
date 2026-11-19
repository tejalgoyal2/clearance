import { Observable, of } from 'rxjs';
import { AuditEvent, ComponentView, Policy, ScanSummary } from './api/models';

// Shared test data and a fake API, so page tests need no HTTP at all.

export const SCAN: ScanSummary = {
  id: 7,
  projectName: 'demo-app',
  ecosystem: 'NPM',
  createdAt: '2026-11-19T10:00:00Z',
  componentCount: 3,
  counts: { APPROVED: 1, REVIEW: 0, UNKNOWN: 1, DENIED: 1 },
};

export function component(overrides: Partial<ComponentView>): ComponentView {
  return {
    id: 1,
    name: 'lodash',
    version: '4.17.21',
    rawLicense: 'MIT',
    licenseSource: 'lockfile',
    spdxExpression: 'MIT',
    policyStatus: 'APPROVED',
    policyNote: null,
    decision: 'NONE',
    decidedBy: null,
    decidedAt: null,
    decisionComment: null,
    ...overrides,
  };
}

/** Every method is a Vitest mock; tests override the ones they care about. */
export function fakeApi() {
  return {
    listScans: vi.fn(() => of([SCAN])),
    getScan: vi.fn(() => of(SCAN)),
    createScan: vi.fn(() => of(SCAN)),
    getComponents: vi.fn(() => of([] as ComponentView[])),
    decide: vi.fn((): Observable<ComponentView> => of(component({}))),
    getAuditTrail: vi.fn(() => of([] as AuditEvent[])),
    getPolicy: vi.fn((): Observable<Policy> => of({ approved: [], review: [], denied: [], aliases: {}, exceptions: {} })),
    reportUrl: vi.fn((id: number) => `/api/scans/${id}/report.csv`),
  };
}

export function text(element: HTMLElement): string {
  return element.textContent?.replace(/\s+/g, ' ').trim() ?? '';
}
