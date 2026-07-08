import { defineConfig, devices } from "@playwright/test";

// E2E functional tests for the Tessera SPA, driven against a running stack
// (docker compose). Locally the stack is on http://localhost:8000; CI overrides
// with E2E_BASE_URL. Tests run serially on a single worker because several of
// them create/mutate shared data on one backend.
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:8000",
    headless: true,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
