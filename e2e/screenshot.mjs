// Captures docs/screenshot.png for the README from a running stack.
// Usage: node screenshot.mjs [scanId]
import { chromium } from '@playwright/test';

const scanId = process.argv[2] ?? '1';
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 900 }, colorScheme: 'light', deviceScaleFactor: 2 });
await page.goto(`http://localhost:8080/scans/${scanId}`);
await page.locator('tbody tr').first().waitFor();
await page.evaluate(() => document.fonts.ready);
await page.screenshot({ path: '../docs/screenshot.png' });
await browser.close();
