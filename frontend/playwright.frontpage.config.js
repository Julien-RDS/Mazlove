import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  testMatch: ["frontpage.spec.js", "providerJourney.spec.js"],
  timeout: 45000,
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:4174",
    launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE }
      : {},
  },
  webServer: {
    command: "npm run build && npm run preview -- --host 127.0.0.1 --port 4174",
    url: "http://127.0.0.1:4174",
    timeout: 120000,
    reuseExistingServer: false,
    env: { VITE_DEMO: "true" },
  },
});
