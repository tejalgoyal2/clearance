import { HttpErrorResponse } from '@angular/common/http';
import { describeError } from './errors';

describe('describeError', () => {
  it('uses the message from the API error body', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { status: 409, error: 'Conflict', message: 'Component 5 was already ACCEPTED' },
    });

    expect(describeError(error)).toBe('Component 5 was already ACCEPTED');
  });

  it('explains when the server cannot be reached', () => {
    expect(describeError(new HttpErrorResponse({ status: 0 }))).toContain('reach the server');
  });

  it('falls back to the HTTP status', () => {
    expect(describeError(new HttpErrorResponse({ status: 502, statusText: 'Bad Gateway' }))).toContain('502');
  });

  it('handles anything else', () => {
    expect(describeError('boom')).toBe('Something went wrong.');
  });
});
