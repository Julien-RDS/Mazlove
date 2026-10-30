import { describe, it, expect } from "vitest";
import { meters, heading, splitRoute } from "./geo";
describe("géométrie", () => {
  it("distance et cap connus", () => {
    expect(meters([0, 0], [1, 0])).toBeCloseTo(111195, -1);
    expect(heading([0, 0], [1, 0])).toBe(90);
  });
  it("découpe le trajet à la position projetée", () => {
    const r = splitRoute(
      [
        [0, 0],
        [0.01, 0],
        [0.02, 0],
      ],
      [0.01, 0.001],
    );
    expect(r.passed.geometry.coordinates[0]).toEqual([0, 0]);
    expect(r.remaining.geometry.coordinates.at(-1)).toEqual([0.02, 0]);
  });
});
