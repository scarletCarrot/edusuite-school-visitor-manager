import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  // Screens refresh every 5 seconds, so changes made by another user can take that long to appear.
  expect: { timeout: 10_000 },
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // Uses the stack if it is already running; otherwise starts it. /api/me answers 401 once the API is up.
  webServer: {
    command: 'docker compose up --build',
    cwd: '..',
    url: 'http://localhost:4200/api/me',
    reuseExistingServer: true,
    timeout: 600_000,
  },
});
