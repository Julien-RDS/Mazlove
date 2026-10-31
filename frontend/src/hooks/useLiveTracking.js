import { useEffect, useState } from "react";
import { subscribeLiveTracking } from "../lib/liveTrackingSession";
export { applyUpdate } from "../lib/liveTrackingSession";
const empty = { status: null, pos: null, route: null, connected: false };
export function useLiveTracking(id, token) {
  const key = `${token}:${id}`;
  const [record, setRecord] = useState({ key: null, state: empty });
  useEffect(() => {
    if (!id || !token) return;
    return subscribeLiveTracking(id, token, (state) =>
      setRecord({ key, state }),
    );
  }, [id, token, key]);
  return record.key === key ? record.state : empty;
}
