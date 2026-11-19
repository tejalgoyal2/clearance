import { defineConfig } from '@playwright/test';

// Runs against the full stack started by `docker compose up` (UI on port 8080).
export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? 'github' : 'list',
  use: {
    baseURL: process.env['BASE_URL'] ?? 'http://localhost:8080',
    trace: 'retain-on-failure',
  },
});
