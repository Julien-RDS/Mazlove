import { it, expect } from "vitest";
import { selectTrackedSession } from "./sessionTracking";
const now = new Date("2026-10-30T12:00:00").getTime();
it("privilégie le trajet en route sans modifier les données reçues", () => {
  const sessions = [
    { id: 1, status: "ACCEPTED", datetime: "2026-10-31T12:00:00" },
    { id: 2, status: "EN_ROUTE", datetime: "2026-10-30T11:00:00" },
  ];
  expect(selectTrackedSession(sessions, now).id).toBe(2);
  expect(sessions[0].id).toBe(1);
});
it("écarte les réservations anciennes, annulées et terminées", () => {
  expect(
    selectTrackedSession(
      [
        { id: 1, status: "ACCEPTED", datetime: "2026-10-20T12:00:00" },
        { id: 2, status: "DONE" },
        { id: 3, status: "CANCELLED" },
      ],
      now,
    ),
  ).toBeNull();
});
it("choisit le rendez-vous futur le plus proche", () => {
  expect(
    selectTrackedSession(
      [
        { id: 1, status: "ACCEPTED", datetime: "2026-11-01T12:00:00" },
        { id: 2, status: "ACCEPTED", datetime: "2026-10-31T12:00:00" },
      ],
      now,
    ).id,
  ).toBe(2);
});
it("conserve une séance en cours même si son horaire prévu est dépassé", () => {
  expect(
    selectTrackedSession(
      [{ id: 1, status: "IN_PROGRESS", datetime: "2026-10-29T12:00:00" }],
      now,
    ).id,
  ).toBe(1);
});
