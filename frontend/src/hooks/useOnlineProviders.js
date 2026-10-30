import { useEffect, useState } from "react";
import { trackingApi as api } from "../lib/trackingApi";
export function useOnlineProviders(token, lat = 48.8584, lng = 2.347) {
  const [providers, set] = useState([]);
  useEffect(() => {
    if (!token) return;
    let alive = true;
    const load = () =>
      api(`/providers/online?lat=${lat}&lng=${lng}`, token)
        .then((r) => alive && set(r))
        .catch(() => {});
    load();
    const t = setInterval(load, 15000);
    return () => {
      alive = false;
      clearInterval(t);
    };
  }, [token, lat, lng]);
  return providers;
}
