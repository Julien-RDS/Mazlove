import React, { useEffect, useRef, useState } from "react";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import "./tracking.css";
import { splitRoute, heading } from "../../lib/geo";
import { animateMarker } from "../../lib/animateMarker";
export default function TrackingMap({
  pos,
  meetingPoint,
  route,
  stale,
  onPick,
  providers = [],
}) {
  const host = useRef(),
    map = useRef(),
    marker = useRef(),
    meeting = useRef(),
    last = useRef(),
    cancel = useRef(),
    follow = useRef(true),
    pick = useRef(onPick);
  pick.current = onPick;
  const [moved, setMoved] = useState(false),
    [ready, setReady] = useState(false),
    [error, setError] = useState("");
  useEffect(() => {
    let m;
    try {
      m = new maplibregl.Map({
        container: host.current,
        style:
          import.meta.env.VITE_MAP_STYLE_URL ||
          "https://tiles.openfreemap.org/styles/liberty",
        center: [2.347, 48.8584],
        zoom: 12,
        attributionControl: true,
      });
      map.current = m;
      m.addControl(new maplibregl.NavigationControl());
      m.on("load", () => setReady(true));
      m.on("error", () =>
        setError(
          "Fond de carte indisponible ; le suivi textuel reste accessible.",
        ),
      );
      m.on("dragstart", () => {
        follow.current = false;
        setMoved(true);
      });
      m.on("click", (e) =>
        pick.current?.({
          lat: e.lngLat.lat,
          lng: e.lngLat.lng,
          label: "Point choisi sur la carte",
        }),
      );
    } catch {
      setError("Carte indisponible sur ce navigateur.");
    }
    return () => {
      cancel.current?.();
      m?.remove();
      map.current = null;
      marker.current = null;
      meeting.current = null;
      last.current = null;
      setReady(false);
    };
  }, []);
  useEffect(() => {
    if (!ready || !map.current) return;
    const m = map.current;
    if (meetingPoint) {
      if (!meeting.current)
        meeting.current = new maplibregl.Marker({ color: "#c96b46" }).addTo(m);
      meeting.current.setLngLat([meetingPoint.lng, meetingPoint.lat]);
    }
    if (!pos) {
      marker.current?.remove();
      marker.current = null;
      last.current = null;
      if (meetingPoint)
        m.easeTo({ center: [meetingPoint.lng, meetingPoint.lat], zoom: 14 });
    }
    if (pos) {
      const to = [pos.lng, pos.lat];
      if (!marker.current) {
        const el = document.createElement("div");
        el.className = "tracking-marker";
        el.textContent = "▲";
        marker.current = new maplibregl.Marker({
          element: el,
          rotationAlignment: "map",
        })
          .setLngLat(to)
          .addTo(m);
        if (meetingPoint)
          m.fitBounds([to, [meetingPoint.lng, meetingPoint.lat]], {
            padding: 60,
            maxZoom: 15,
          });
      }
      marker.current.getElement().classList.toggle("stale", !!stale);
      marker.current.setRotation(
        pos.heading ?? (last.current ? heading(last.current.coords, to) : 0),
      );
      cancel.current?.();
      const current = marker.current.getLngLat();
      cancel.current = animateMarker(
        marker.current,
        [current.lng, current.lat],
        to,
        last.current ? pos.ts - last.current.ts : 1000,
      );
      last.current = { coords: to, ts: pos.ts };
      if (follow.current) m.easeTo({ center: to, duration: 900 });
    }
    const coords = route?.coordinates;
    const parts =
      coords?.length > 1
        ? splitRoute(coords, pos ? [pos.lng, pos.lat] : coords[0])
        : null;
    for (const [id, color] of [
      ["passed", "#8e9098"],
      ["remaining", "#4d6655"],
    ]) {
      const data = parts?.[id] || { type: "FeatureCollection", features: [] };
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
  }, [ready, pos, meetingPoint, route, stale]);
  useEffect(() => {
    if (!ready) return;
    const markers = providers.map((p) =>
      new maplibregl.Marker({ color: "#7368a0" })
        .setLngLat([p.lng, p.lat])
        .addTo(map.current),
    );
    return () => markers.forEach((m) => m.remove());
  }, [ready, providers]);
  return (
    <section aria-label="Carte du suivi de trajet">
      <div className="tracking-map" ref={host} />
      {error && <p role="status">{error}</p>}
      {moved && (
        <button
          type="button"
          onClick={() => {
            follow.current = true;
            setMoved(false);
            if (pos) map.current?.easeTo({ center: [pos.lng, pos.lat] });
          }}
        >
          Recentrer
        </button>
      )}
    </section>
  );
}
