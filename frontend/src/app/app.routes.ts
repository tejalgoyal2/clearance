import { Routes } from '@angular/router';
import { Audit } from './pages/audit/audit';
import { NewScan } from './pages/new-scan/new-scan';
import { PolicyPage } from './pages/policy/policy';
import { ScanDetail } from './pages/scan-detail/scan-detail';
import { ScanList } from './pages/scan-list/scan-list';

export const routes: Routes = [
  { path: '', component: ScanList, title: 'Scans · Clearance' },
  { path: 'scans/new', component: NewScan, title: 'New scan · Clearance' },
  { path: 'scans/:id', component: ScanDetail, title: 'Scan · Clearance' },
  { path: 'scans/:id/audit', component: Audit, title: 'Audit trail · Clearance' },
  { path: 'policy', component: PolicyPage, title: 'Policy · Clearance' },
  { path: '**', redirectTo: '' },
];
