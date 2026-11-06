import { defineConfig } from "@playwright/test";
import tracking from "./playwright.config.js";

export default defineConfig({
  ...tracking,
  testMatch: "mailOutage.spec.js",
  timeout: 180000,
});
