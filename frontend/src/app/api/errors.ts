import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

/** Turns a failed request into one sentence a person can act on. */
export function describeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Something went wrong.';
  }
  if (error.status === 0) {
    return 'Could not reach the server. Is the backend running?';
  }
  const body = error.error as Partial<ApiError> | null;
  if (body && typeof body.message === 'string') {
    return body.message;
  }
  return `The server answered ${error.status} ${error.statusText}`.trim();
}
