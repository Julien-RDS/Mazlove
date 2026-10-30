import distance from "@turf/distance";
import bearing from "@turf/bearing";
import nearestPointOnLine from "@turf/nearest-point-on-line";
import lineSlice from "@turf/line-slice";
import { lineString, point } from "@turf/helpers";
export const meters = (a, b) =>
  distance(point(a), point(b), { units: "meters" });
export const heading = (a, b) => (bearing(point(a), point(b)) + 360) % 360;
export function splitRoute(coords, pos) {
  if (!coords || coords.length < 2) return null;
  const line = lineString(coords),
    snap = nearestPointOnLine(line, point(pos));
  return {
    passed: lineSlice(point(coords[0]), snap, line),
    remaining: lineSlice(snap, point(coords.at(-1)), line),
  };
}
