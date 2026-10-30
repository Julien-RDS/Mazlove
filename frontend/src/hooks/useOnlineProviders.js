import { useEffect, useState } from "react";
import { trackingApi } from "../lib/trackingApi";
import { startVisiblePolling } from "../lib/visiblePolling";
export function useOnlineProviders(
  token,
  lat = 48.8584,
  lng = 2.347,
  enabled = true,
) {
  const [state, setState] = useState({
    key: null,
    providers: [],
    loading: false,
    error: "",
  });
  const key = `${token}:${lat}:${lng}`;
  useEffect(() => {
    if (!token || !enabled) return;
    let alive = true;
    setState((s) => ({
      ...s,
      key,
      providers: s.key === key ? s.providers : [],
      loading: true,
      error: "",
    }));
    const polling = startVisiblePolling(
      async (signal) => {
        const providers = await trackingApi(
          `/providers/online?lat=${lat}&lng=${lng}`,
          token,
          "GET",
          undefined,
          { signal },
        );
        if (alive) setState({ key, providers, loading: false, error: "" });
      },
      15000,
      {
        onError: (error) =>
          alive &&
          setState((s) => ({ ...s, loading: false, error: error.message })),
      },
    );
    return () => {
      alive = false;
      polling.stop();
    };
  }, [token, lat, lng, enabled, key]);
  return token && enabled && state.key === key
    ? state
    : { providers: [], loading: !!token && enabled, error: "" };
}
