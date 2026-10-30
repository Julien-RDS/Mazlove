import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import TrackingMap from "./TrackingMap";
export default function MeetingPointPicker({ value, onChange, token }) {
  const [query, set] = useState(""),
    [results, R] = useState([]),
    [error, E] = useState("");
  useEffect(() => {
    let active = true;
    if (query.length < 3) {
      R([]);
      return;
    }
    const t = setTimeout(
      () =>
        api(`/geocode?q=${encodeURIComponent(query)}`, token)
          .then((r) => active && R(r.features || []))
          .catch((e) => active && E(e.message)),
      400,
    );
    return () => {
      active = false;
      clearTimeout(t);
    };
  }, [query, token]);
  return (
    <section className="tracking-panel">
      <h3>Point de rendez-vous</h3>
      <label>
        Rechercher une adresse
        <input
          value={query}
          onChange={(e) => set(e.target.value)}
          maxLength={200}
        />
      </label>
      <ul>
        {results.map((r, i) => (
          <li key={i}>
            <button
              type="button"
              onClick={() => {
                onChange({
                  lng: r.geometry.coordinates[0],
                  lat: r.geometry.coordinates[1],
                  label: r.properties.label,
                });
                R([]);
              }}
            >
              {r.properties.label}
            </button>
          </li>
        ))}
      </ul>
      <p>{value?.label || "Choisissez une adresse ou cliquez sur la carte."}</p>
      <TrackingMap meetingPoint={value} onPick={onChange} />
      <p role="status">{error}</p>
    </section>
  );
}
