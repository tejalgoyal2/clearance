import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditEvent, ComponentView, DecisionRequest, Policy, PolicyStatus, ScanSummary } from './models';

/**
 * Every call to the backend goes through this one service.
 *
 * Each method returns an Observable: nothing is sent until a component subscribes,
 * and HttpClient completes it after the single response.
 *
 * URLs are relative ("/api/..."). In development the Angular dev server proxies them
 * to the backend (proxy.conf.json); in Docker, nginx does (frontend/nginx.conf).
 */
@Injectable({ providedIn: 'root' })
export class ClearanceApi {
  private readonly http = inject(HttpClient);

  listScans(): Observable<ScanSummary[]> {
    return this.http.get<ScanSummary[]>('/api/scans');
  }

  getScan(id: number): Observable<ScanSummary> {
    return this.http.get<ScanSummary>(`/api/scans/${id}`);
  }

  /** Multipart upload. The browser sets the Content-Type boundary itself. */
  createScan(projectName: string, file: File): Observable<ScanSummary> {
    const form = new FormData();
    form.append('projectName', projectName);
    form.append('file', file);
    return this.http.post<ScanSummary>('/api/scans', form);
  }

  /** @param status only this verdict, or null for every component */
  getComponents(scanId: number, status: PolicyStatus | null): Observable<ComponentView[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<ComponentView[]>(`/api/scans/${scanId}/components`, { params });
  }

  decide(componentId: number, request: DecisionRequest): Observable<ComponentView> {
    return this.http.post<ComponentView>(`/api/components/${componentId}/decision`, request);
  }

  getAuditTrail(scanId: number): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`/api/scans/${scanId}/audit`);
  }

  getPolicy(): Observable<Policy> {
    return this.http.get<Policy>('/api/policy');
  }

  /** A plain link, so the browser downloads the file itself. */
  reportUrl(scanId: number): string {
    return `/api/scans/${scanId}/report.csv`;
  }
}
