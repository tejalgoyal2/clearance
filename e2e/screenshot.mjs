// Captures the README screenshots into docs/screenshots from a running stack
// (docker compose up). Usage: node screenshot.mjs
import { chromium } from '@playwright/test';

const BASE = 'http://localhost:8080';
const OUT = '../docs/screenshots';

const browser = await chromium.launch();

async function shoot(name, path, { dark = false, fullPage = false, before } = {}) {
  const page = await browser.newPage({
    viewport: { width: 1280, height: 860 },
    colorScheme: dark ? 'dark' : 'light',
    deviceScaleFactor: 2,
  });
  await page.goto(BASE + path);
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts.ready);
  if (before) await before(page);
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage });
  await page.close();
}

await shoot('scan-detail', '/scans/1');
await shoot('scan-detail-dark', '/scans/1', { dark: true });
await shoot('scans', '/');
await shoot('decision-dialog', '/scans/2', {
  before: async (page) => {
    const deniedRow = page.locator('tbody tr').filter({ has: page.locator('.chip--denied') }).first();
    await deniedRow.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('dialog').waitFor();
  },
});
await shoot('audit', '/scans/1/audit');
await shoot('policy', '/policy', { fullPage: true });
await shoot('new-scan', '/scans/new');

await browser.close();
