import { defineConfig } from "vitest/config";
export default defineConfig({
  test: {
    include: ["src/**/*.test.js"],
    pool: "forks",
    poolOptions: { forks: { singleFork: true } },
  },
});
