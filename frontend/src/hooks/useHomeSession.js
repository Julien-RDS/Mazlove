import { useEffect, useState } from "react";
import { trackingApi } from "../lib/trackingApi";
import { startVisiblePolling } from "../lib/visiblePolling";
import { SESSION_UPDATED_EVENT } from "../lib/sessionTracking";
export function useHomeSession(token) {
  const [state, setState] = useState({
    token: null,
    session: null,
    loading: false,
    error: "",
  });
  useEffect(() => {
    if (!token) return;
    let alive = true;
    setState({ token, session: null, loading: true, error: "" });
    const polling = startVisiblePolling(
      async (signal) => {
        const session = await trackingApi(
          "/users/me/active-session",
          token,
          "GET",
          undefined,
          { signal },
        );
        if (alive)
          setState({
            token,
            session,
            loading: false,
            error: "",
          });
      },
      30000,
      {
        onError: (error) =>
          alive &&
          setState((s) => ({ ...s, loading: false, error: error.message })),
      },
    );
    const refresh = () => polling.refresh();
    window.addEventListener(SESSION_UPDATED_EVENT, refresh);
    return () => {
      alive = false;
      polling.stop();
      window.removeEventListener(SESSION_UPDATED_EVENT, refresh);
    };
  }, [token]);
  return state.token === token
    ? state
    : { session: null, loading: !!token, error: "" };
}
