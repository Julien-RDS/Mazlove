import React, { memo, useEffect, useRef, useState } from "react";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import "./tracking.css";
import { splitRoute, heading } from "../../lib/geo";
import { animateMarker } from "../../lib/animateMarker";
const EMPTY_PROVIDERS = [];
const EMPTY_DATA = { type: "FeatureCollection", features: [] };
function TrackingMap({
  pos,
  meetingPoint,
  route,
  stale,
  onPick,
  providers = EMPTY_PROVIDERS,
  center,
  ariaLabel = "Carte du suivi de trajet",
}) {
  const host = useRef(),
    map = useRef(),
    marker = useRef(),
    meeting = useRef(),
    last = useRef(),
    cancel = useRef(),
    follow = useRef(true),
    pick = useRef(onPick),
    nearby = useRef(new Map()),
    initialCenter = useRef(pos || meetingPoint || center);
  pick.current = onPick;
  const [moved, setMoved] = useState(false),
    [ready, setReady] = useState(false),
    [error, setError] = useState("");
  useEffect(() => {
    let m,
      resize,
      disposed = false;
    try {
      const c = initialCenter.current;
      m = new maplibregl.Map({
        container: host.current,
        style:
          import.meta.env.VITE_MAP_STYLE_URL ||
          "https://tiles.openfreemap.org/styles/liberty",
        center: c ? [c.lng, c.lat] : [2.347, 48.8584],
        zoom: 12,
        attributionControl: true,
      });
      map.current = m;
      m.addControl(new maplibregl.NavigationControl());
      m.on("load", () => {
        if (!disposed) setReady(true);
      });
      m.on("error", () => {
        if (!disposed)
          setError(
            "Fond de carte indisponible ; le suivi textuel reste accessible.",
          );
      });
      // Camera events carry originalEvent for mouse/touch/keyboard and navigation controls.
      // Programmatic easeTo/fitBounds must not disable follow themselves.
      const stopFollowing = (event) => {
        if (!event.originalEvent) return;
        follow.current = false;
        setMoved(true);
      };
      for (const event of [
        "dragstart",
        "zoomstart",
        "rotatestart",
        "pitchstart",
      ])
        m.on(event, stopFollowing);
      m.on("click", (e) =>
        pick.current?.({
          lat: e.lngLat.lat,
          lng: e.lngLat.lng,
          label: "Point choisi sur la carte",
        }),
      );
      if (typeof ResizeObserver !== "undefined") {
        resize = new ResizeObserver(() => m.resize());
        resize.observe(host.current);
      }
    } catch {
      setError("Carte indisponible sur ce navigateur.");
    }
    return () => {
      disposed = true;
      resize?.disconnect();
      cancel.current?.();
      nearby.current.forEach((m) => m.remove());
      nearby.current.clear();
      m?.remove();
      map.current = null;
      marker.current = null;
      meeting.current = null;
      last.current = null;
      setReady(false);
    };
  }, []);
  useEffect(() => {
    if (!ready) return;
    if (!meetingPoint) {
      meeting.current?.remove();
      meeting.current = null;
      return;
    }
    if (!meeting.current)
      meeting.current = new maplibregl.Marker({ color: "#c96b46" })
        .setLngLat([meetingPoint.lng, meetingPoint.lat])
        .addTo(map.current);
    meeting.current.setLngLat([meetingPoint.lng, meetingPoint.lat]);
    if (!last.current && follow.current)
      map.current.easeTo({
        center: [meetingPoint.lng, meetingPoint.lat],
        zoom: 14,
        duration: window.matchMedia("(prefers-reduced-motion: reduce)").matches
          ? 0
          : 900,
      });
  }, [ready, meetingPoint?.lat, meetingPoint?.lng]);
  useEffect(() => {
    if (!ready) return;
    if (!pos) {
      cancel.current?.();
      marker.current?.remove();
      marker.current = null;
      last.current = null;
      return;
    }
    const m = map.current,
      to = [pos.lng, pos.lat],
      fresh = !marker.current;
    if (fresh) {
      const el = document.createElement("div");
      el.className = "tracking-marker";
      el.textContent = "▲";
      marker.current = new maplibregl.Marker({
        element: el,
        rotationAlignment: "map",
      })
        .setLngLat(to)
        .addTo(m);
    }
    const previous = last.current;
    const changed =
      !previous || previous.coords[0] !== to[0] || previous.coords[1] !== to[1];
    // A fresh timestamp or ETA does not mean the person moved. Keep the bearing
    // when stationary instead of deriving a north-facing bearing from identical points.
    if (Number.isFinite(pos.heading)) marker.current.setRotation(pos.heading);
    else if (previous && changed)
      marker.current.setRotation(heading(previous.coords, to));
    if (changed) {
      cancel.current?.();
      const current = marker.current.getLngLat();
      if (!fresh)
        cancel.current = animateMarker(
          marker.current,
          [current.lng, current.lat],
          to,
          previous ? pos.ts - previous.ts : 1000,
        );
      const duration = window.matchMedia("(prefers-reduced-motion: reduce)")
        .matches
        ? 0
        : 900;
      if (follow.current) {
        if (fresh && meetingPoint)
          m.fitBounds([to, [meetingPoint.lng, meetingPoint.lat]], {
            padding: 60,
            maxZoom: 15,
            duration,
          });
        else m.easeTo({ center: to, duration });
      }
    }
    last.current = { coords: to, ts: pos.ts };
  }, [ready, pos?.lat, pos?.lng, pos?.heading, pos?.ts]);
  useEffect(() => {
    marker.current?.getElement().classList.toggle("stale", !!stale);
  }, [ready, pos, stale]);
  useEffect(() => {
    if (!ready) return;
    const m = map.current,
      coords = route?.coordinates;
    const parts =
      coords?.length > 1
        ? splitRoute(coords, pos ? [pos.lng, pos.lat] : coords[0])
        : null;
    for (const [id, color] of [
      ["passed", "#8e9098"],
      ["remaining", "#4d6655"],
    ]) {
      const data = parts?.[id] || EMPTY_DATA;
      if (m.getSource(id)) m.getSource(id).setData(data);
      else {
        m.addSource(id, { type: "geojson", data });
        m.addLayer({
          id,
          type: "line",
          source: id,
          paint: { "line-color": color, "line-width": 5 },
        });
      }
    }
  }, [ready, route, pos?.lng, pos?.lat]);
  useEffect(() => {
    if (!ready) return;
    const ids = new Set(providers.map((p) => String(p.providerId)));
    nearby.current.forEach((m, id) => {
      if (!ids.has(id)) {
        m.remove();
        nearby.current.delete(id);
      }
    });
    for (const p of providers) {
      const id = String(p.providerId);
      let entry = nearby.current.get(id);
      if (!entry) {
        const link = document.createElement("a");
        link.href = `#/praticien/${encodeURIComponent(id)}`;
        link.className = "online-provider-marker";
        link.textContent = "♡";
        link.setAttribute(
          "aria-label",
          "Voir le profil de ce praticien disponible",
        );
        entry = new maplibregl.Marker({ element: link })
          .setLngLat([p.lng, p.lat])
          .addTo(map.current);
        nearby.current.set(id, entry);
      } else entry.setLngLat([p.lng, p.lat]);
    }
  }, [ready, providers]);
  return (
    <section aria-label={ariaLabel}>
      <div className="tracking-map" ref={host} />
      {error && <p role="status">{error}</p>}
      {moved && (
        <button
          type="button"
          onClick={() => {
            follow.current = true;
            setMoved(false);
            const point = pos || meetingPoint || center;
            if (point)
              map.current?.easeTo({
                center: [point.lng, point.lat],
                duration: window.matchMedia("(prefers-reduced-motion: reduce)")
                  .matches
                  ? 0
                  : 900,
              });
          }}
        >
          Recentrer
        </button>
      )}
    </section>
  );
}
export default memo(TrackingMap);
