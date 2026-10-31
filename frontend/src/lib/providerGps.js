import { wsClient } from "./ws";
import { meters } from "./geo";

const FRESH_AFTER = 10000;
const MAX_AGE = 30000;

/** One watch, one buffered point, and fresh stationary fixes; no offline position queue. */
export function startProviderGps({
  id,
  token,
  onState,
  geolocation = navigator.geolocation,
  wakeLock = navigator.wakeLock,
  visibility = document,
  makeClient = wsClient,
}) {
  let alive = true,
    watch,
    screenLock,
    waking = false,
    reading = false;
  let latest = null,
    last = null,
    sentAt = 0,
    sequence = Date.now();
  let sendTimer,
    freshTimer,
    connectedOnce = false,
    stateKey = "";
  let fixReceivedAt = 0;
  const report = (phase, error = "") => {
    const key = `${phase}:${error}`;
    if (key !== stateKey) {
      stateKey = key;
      onState({ phase, error });
    }
  };
  const clearTimers = () => {
    clearTimeout(sendTimer);
    clearTimeout(freshTimer);
  };
  const releaseScreen = () => {
    screenLock?.release().catch(() => {});
    screenLock = null;
  };
  const wake = async () => {
    if (
      !alive ||
      visibility.hidden ||
      waking ||
      (screenLock && !screenLock.released)
    )
      return;
    waking = true;
    try {
      const lock = await wakeLock?.request("screen");
      if (alive && !visibility.hidden) screenLock = lock;
      else await lock?.release();
    } catch {
    } finally {
      waking = false;
    }
  };
  const stop = () => {
    if (!alive) return;
    alive = false;
    latest = null;
    clearTimers();
    if (watch !== undefined) geolocation?.clearWatch(watch);
    releaseScreen();
    visibility.removeEventListener("visibilitychange", visible);
    client.deactivate();
  };
  const refreshLater = () => {
    clearTimeout(freshTimer);
    if (alive && client.connected && !visibility.hidden)
      freshTimer = setTimeout(readFresh, FRESH_AFTER);
  };
  const send = () => {
    clearTimeout(sendTimer);
    if (!alive || !latest || !client.connected) return;
    const now = Date.now();
    if (now - latest.ts > MAX_AGE || (last && latest.ts <= last.ts)) {
      latest = null;
      refreshLater();
      return;
    }
    const stationary =
      last && meters([last.lng, last.lat], [latest.lng, latest.lat]) <= 5;
    const remaining = sentAt + (stationary ? FRESH_AFTER : 3000) - now;
    if (sentAt && remaining > 0) {
      sendTimer = setTimeout(send, remaining);
      return;
    }
    try {
      client.publish({
        destination: `/app/tracking/${id}/position`,
        body: JSON.stringify({ ...latest, seq: ++sequence }),
      });
      last = latest;
      sentAt = now;
      latest = null;
      report(
        visibility.hidden ? "background" : "sharing",
        visibility.hidden
          ? "Gardez l’application au premier plan pour maintenir le GPS."
          : "",
      );
    } catch {
      latest = null;
      report(
        "reconnecting",
        "Connexion interrompue. Le suivi reprendra avec une position récente.",
      );
      refreshLater();
    }
  };
  const accept = (p) => {
    if (!alive) return;
    const {
      latitude: lat,
      longitude: lng,
      accuracy,
      heading,
      speed,
    } = p.coords;
    const ts = p.timestamp,
      now = Date.now();
    if (
      ![lat, lng, accuracy, ts].every(Number.isFinite) ||
      Math.abs(lat) > 90 ||
      Math.abs(lng) > 180 ||
      accuracy < 0 ||
      accuracy > 100 ||
      ts < now - MAX_AGE ||
      ts > now + 5000
    ) {
      latest = null;
      report(
        "weak",
        "GPS faible ou position ancienne : recherchez un endroit dégagé.",
      );
      refreshLater();
      return;
    }
    // A slower getCurrentPosition callback must not replace a newer watch fix.
    if (ts <= Math.max(last?.ts || 0, latest?.ts || 0)) {
      refreshLater();
      return;
    }
    fixReceivedAt = now;
    latest =
      client.connected || !connectedOnce
        ? {
            lat,
            lng,
            accuracy,
            heading:
              Number.isFinite(heading) && heading >= 0 && heading <= 360
                ? heading
                : null,
            speed: Number.isFinite(speed) && speed >= 0 ? speed : null,
            ts,
          }
        : null;
    if (client.connected) send();
    else
      report(
        "reconnecting",
        "Connexion interrompue. Le suivi reprendra avec une position récente.",
      );
    refreshLater();
  };
  const failed = (error) => {
    if (!alive) return;
    latest = null;
    clearTimeout(sendTimer);
    if (error.code === 1) {
      report(
        "denied",
        "Autorisez la localisation dans les réglages du navigateur, puis reprenez le suivi.",
      );
      stop();
    } else {
      report("weak", "Position indisponible. Gardez l’application ouverte.");
      refreshLater();
    }
  };
  function readFresh() {
    if (!alive || !client.connected || visibility.hidden || reading) return;
    reading = true;
    const requestedAt = Date.now();
    try {
      geolocation.getCurrentPosition(
        (p) => {
          reading = false;
          accept(p);
        },
        (error) => {
          reading = false;
          if (fixReceivedAt <= requestedAt) failed(error);
          else refreshLater();
        },
        { enableHighAccuracy: true, maximumAge: 0, timeout: 10000 },
      );
    } catch {
      reading = false;
      failed({ code: 1 });
    }
  }
  function visible() {
    if (!alive) return;
    if (visibility.hidden) {
      clearTimeout(freshTimer);
      releaseScreen();
      report(
        "background",
        "Gardez l’application au premier plan pour maintenir le GPS.",
      );
    } else {
      wake();
      readFresh();
    }
  }
  const client = makeClient(token, {
    onConnect: () => {
      if (!alive) return;
      if (connectedOnce) latest = null;
      connectedOnce = true;
      report("locating", "Recherche d’une position récente…");
      if (latest) send();
      readFresh();
    },
    onWebSocketClose: (event) => {
      if (!alive) return;
      latest = null;
      clearTimers();
      if (event.code === 1008) {
        report(
          "expired",
          "Session expirée. Reconnectez-vous pour reprendre le suivi.",
        );
        stop();
      } else
        report(
          "reconnecting",
          "Connexion interrompue. Le suivi reprendra avec une position récente.",
        );
    },
    onStompError: () => {
      if (!alive) return;
      report(
        "expired",
        "Connexion GPS refusée. Reconnectez-vous pour reprendre le suivi.",
      );
      stop();
    },
  });
  if (!geolocation) {
    report(
      "unsupported",
      "La géolocalisation nécessite HTTPS et un navigateur compatible.",
    );
    stop();
    return stop;
  }
  report("locating", "Recherche de votre position…");
  visibility.addEventListener("visibilitychange", visible);
  try {
    watch = geolocation.watchPosition(accept, failed, {
      enableHighAccuracy: true,
      maximumAge: 1000,
      timeout: 10000,
    });
  } catch {
    failed({ code: 1 });
  }
  if (alive) {
    client.activate();
    wake();
  }
  return stop;
}
