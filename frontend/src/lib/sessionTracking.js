export const SESSION_UPDATED_EVENT = "mazlove:session-updated";
const PRIORITY = {
  EN_ROUTE: 0,
  ARRIVED: 1,
  IN_PROGRESS: 2,
  ACCEPTED: 3,
  PENDING: 4,
};
/** Prefer an ongoing journey, then the nearest future appointment; never show a past booking as live. */
export function selectTrackedSession(sessions, now = Date.now()) {
  return (
    sessions
      .filter((s) => {
        if (!(s.status in PRIORITY)) return false;
        if (PRIORITY[s.status] < 3) return true;
        const start = new Date(s.datetime).getTime();
        return (
          Number.isFinite(start) && start + (s.duration || 1) * 3600000 >= now
        );
      })
      .sort(
        (a, b) =>
          PRIORITY[a.status] - PRIORITY[b.status] ||
          new Date(a.datetime) - new Date(b.datetime) ||
          a.id - b.id,
      )[0] || null
  );
}
