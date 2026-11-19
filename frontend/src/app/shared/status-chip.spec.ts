import { TestBed } from '@angular/core/testing';
import { StatusChip } from './status-chip';

describe('StatusChip', () => {
  it('shows the verdict as a word, coloured by a status class', async () => {
    const fixture = TestBed.createComponent(StatusChip);
    fixture.componentRef.setInput('status', 'DENIED');
    await fixture.whenStable();

    const chip = fixture.nativeElement.querySelector('.chip') as HTMLElement;
    expect(chip.textContent?.trim()).toBe('Denied');
    expect(chip.classList).toContain('chip--denied');
  });
});
