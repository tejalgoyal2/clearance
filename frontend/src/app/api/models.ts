// The JSON shapes the backend returns. They mirror the Java records in
// backend/src/main/java/me/tgoyal/clearance/web.

export type PolicyStatus = 'APPROVED' | 'REVIEW' | 'UNKNOWN' | 'DENIED';

/** Least to most severe, the same order as the backend's enum. */
export const POLICY_STATUSES: readonly PolicyStatus[] = ['APPROVED', 'REVIEW', 'UNKNOWN', 'DENIED'];

export type Decision = 'NONE' | 'ACCEPTED' | 'REJECTED';

export type Ecosystem = 'MAVEN' | 'NPM';

export interface ScanSummary {
  id: number;
  projectName: string;
  ecosystem: Ecosystem;
  createdAt: string;
  componentCount: number;
  counts: Record<PolicyStatus, number>;
}

export interface ComponentView {
  id: number;
  name: string;
  version: string | null;
  rawLicense: string | null;
  licenseSource: string | null;
  spdxExpression: string | null;
  policyStatus: PolicyStatus;
  policyNote: string | null;
  decision: Decision;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionComment: string | null;
}

export type AuditAction = 'SCAN_CREATED' | 'DECISION_MADE' | 'REPORT_EXPORTED';

export interface AuditEvent {
  id: number;
  componentId: number | null;
  actor: string;
  action: AuditAction;
  detail: string;
  timestamp: string;
}

export interface Policy {
  approved: string[];
  review: string[];
  denied: string[];
  aliases: Record<string, string>;
  exceptions: Record<string, PolicyStatus>;
}

export interface DecisionRequest {
  decision: 'ACCEPTED' | 'REJECTED';
  reviewer: string;
  comment: string;
}

/** Every error from the API has this shape (see ApiExceptionHandler.java). */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
