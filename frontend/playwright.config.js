import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  testMatch: "tracking.spec.js",
  timeout: 180000,
  use: { baseURL: process.env.BASE_URL || "http://localhost:3000" },
  workers: 1,
});
