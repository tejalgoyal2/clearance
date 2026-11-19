import { expect, test } from '@playwright/test';
import path from 'node:path';

const POM = path.join(__dirname, '..', '..', 'samples', 'pom.xml');

/**
 * The journey from SPEC.md: upload samples/pom.xml, find a DENIED component,
 * accept it with a comment, and see the decision in the audit view.
 *
 * This uses the real backend, which calls deps.dev, so it needs internet access.
 */
test('a reviewer accepts a denied component and the audit trail records it', async ({ page }) => {
  const project = `e2e-${Date.now()}`;
  const reviewer = 'e2e-reviewer';

  // Upload
  await page.goto('/scans/new');
  await page.getByPlaceholder('e.g. billing-service').fill(project);
  await page.locator('input[type="file"]').setInputFiles(POM);
  await page.getByRole('button', { name: 'Run scan' }).click();

  // The scan page opens with every verdict represented
  await expect(page.getByRole('heading', { level: 1, name: project })).toBeVisible({ timeout: 45_000 });
  await expect(page.locator('.tile--denied .tile__count')).not.toHaveText('0');

  // Find the DENIED component
  await page.locator('[data-filter="DENIED"]').click();
  const deniedRow = page.locator('tbody tr').filter({ has: page.locator('.chip--denied') }).first();
  await expect(deniedRow).toBeVisible();
  const componentName = (await deniedRow.locator('.strong').first().innerText()).trim();

  // Accept it with a comment
  await deniedRow.getByRole('button', { name: 'Review' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Accept').check();
  await dialog.locator('input[name="reviewer"]').fill(reviewer);
  await dialog.locator('textarea').fill('Used only in an internal tool that is never distributed.');
  await dialog.getByRole('button', { name: 'Record decision' }).click();

  await expect(dialog).toBeHidden();
  await expect(deniedRow).toContainText(`Accepted by ${reviewer}`);
  // The verdict itself does not change.
  await expect(deniedRow.locator('.chip--denied')).toBeVisible();

  // The audit view shows who decided what, and when
  await page.getByRole('link', { name: 'Audit trail' }).click();
  const decision = page.locator('li').filter({ hasText: 'Decision made' });
  await expect(decision).toContainText(reviewer);
  await expect(decision).toContainText(componentName);
  await expect(decision.locator('time')).not.toBeEmpty();
  await expect(page.locator('li').filter({ hasText: 'Scan created' })).toBeVisible();
});

test('the CSV report downloads', async ({ page, request }) => {
  const scans = await (await request.get('/api/scans')).json();
  test.skip(scans.length === 0, 'needs at least one scan');

  await page.goto(`/scans/${scans[0].id}`);
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('link', { name: 'Download CSV' }).click();
  const download = await downloadPromise;

  expect(download.suggestedFilename()).toBe(`clearance-scan-${scans[0].id}.csv`);
});
