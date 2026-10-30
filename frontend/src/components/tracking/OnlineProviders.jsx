import React, { useEffect, useState } from "react";
import { MapPin, ChevronDown, ShieldCheck } from "lucide-react";
import { useOnlineProviders } from "../../hooks/useOnlineProviders";
import { trackingApi } from "../../lib/trackingApi";
import { startVisiblePolling } from "../../lib/visiblePolling";
import TrackingMap from "./LazyTrackingMap";
export default function OnlineProviders({ token, user, position }) {
  const [open, setOpen] = useState(false),
    [online, setOnline] = useState(false),
    [availability, setAvailability] = useState("offline"),
    [gpsError, setGpsError] = useState("");
  const { providers, loading, error } = useOnlineProviders(
    token,
    position.lat,
    position.lng,
    open,
  );
  useEffect(() => {
    if (!online || !token || !user?.providerId) return;
    let active = true,
      pending = Promise.resolve();
    setAvailability("pending");
    setGpsError("");
    const offline = () =>
      trackingApi("/providers/me/online", token, "PUT", {
        online: false,
      }).catch(() => {});
    const polling = startVisiblePolling(
      async () => {
        if (!navigator.geolocation)
          throw new Error(
            "La localisation nécessite HTTPS et un navigateur compatible.",
          );
        const point = await new Promise((resolve, reject) =>
          navigator.geolocation.getCurrentPosition(
            resolve,
            () =>
              reject(
                new Error(
                  "Autorisez la localisation pour apparaître en ligne.",
                ),
              ),
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 10000 },
          ),
        );
        if (!active) return;
        if (point.coords.accuracy > 100)
          throw new Error(
            "GPS trop imprécis. Réessayez dans un endroit dégagé.",
          );
        pending = trackingApi("/providers/me/online", token, "PUT", {
          online: true,
          lat: point.coords.latitude,
          lng: point.coords.longitude,
        });
        await pending;
        if (active) {
          setAvailability("online");
          setGpsError("");
        }
      },
      30000,
      {
        onError: (e) => {
          if (active) {
            setGpsError(e.message);
            setAvailability("offline");
            setOnline(false);
          }
        },
        onVisibility: (visible) => {
          if (visible && active) setAvailability("pending");
          else if (active) setAvailability("paused");
        },
      },
    );
    return () => {
      active = false;
      polling.stop();
      pending.catch(() => {}).then(offline);
    };
  }, [online, token, user?.providerId]);
  if (!token) return null;
  return (
    <section className="home-nearby" aria-label="Disponibilités autour de vous">
      <div className="home-nearby-heading">
        <button
          type="button"
          className="home-nearby-toggle"
          aria-expanded={open}
          aria-controls="nearby-map"
          onClick={() => setOpen((v) => !v)}
        >
          <MapPin size={18} aria-hidden="true" />
          Disponibles autour de {position.label || "votre point de recherche"}
          <ChevronDown
            size={18}
            aria-hidden="true"
            className={open ? "is-open" : ""}
          />
        </button>
        {!!user?.providerId && (
          <label className="home-online-switch">
            <input
              type="checkbox"
              checked={online}
              onChange={(e) => {
                setOnline(e.target.checked);
                if (!e.target.checked) setAvailability("offline");
              }}
            />
            {!online
              ? "Me rendre disponible"
              : availability === "online"
                ? "En ligne"
                : availability === "paused"
                  ? "Disponibilité en pause"
                  : "Localisation en cours…"}
          </label>
        )}
      </div>
      {gpsError && <p role="alert">{gpsError}</p>}
      {open && (
        <div id="nearby-map">
          <p role="status">
            {error ||
              (loading
                ? "Recherche des disponibilités…"
                : providers.length
                  ? `${providers.length} prestataire${providers.length > 1 ? "s" : ""} disponible${providers.length > 1 ? "s" : ""} dans cette zone.`
                  : "Aucun prestataire en ligne dans cette zone pour le moment.")}
          </p>
          <TrackingMap
            providers={providers}
            center={position}
            ariaLabel="Carte des prestataires disponibles"
          />
          <p className="home-privacy">
            <ShieldCheck size={15} aria-hidden="true" />
            Positions approximatives pour protéger la vie privée.
          </p>
        </div>
      )}
    </section>
  );
}
