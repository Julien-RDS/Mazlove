import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  testMatch: "tracking.spec.js",
  timeout: 180000,
  use: {
    baseURL: process.env.BASE_URL || "http://localhost:3000",
    launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE }
      : {},
  },
  workers: 1,
});
