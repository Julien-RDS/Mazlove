import React from "react";
export default function EtaCard({
  pos,
  stale,
  connected,
  signalLost,
  providerName = "Le prestataire",
}) {
  return (
    <div role="status">
      {!connected && <p>Reconnexion…</p>}
      {pos && (
        <p>
          {providerName} est à{" "}
          {(pos.distanceMeters / 1000).toLocaleString("fr-FR", {
            maximumFractionDigits: 1,
          })}{" "}
          km. Arrivée {pos.estimated ? "estimée " : ""}dans{" "}
          {Math.max(1, Math.ceil(pos.etaSeconds / 60))} min.
        </p>
      )}
      {(stale || signalLost) && (
        <p>
          Signal perdu
          {pos
            ? ` depuis ${Math.max(0, Math.floor((Date.now() - pos.ts) / 1000))} s`
            : ""}
          . La position affichée peut être ancienne.
        </p>
      )}
    </div>
  );
}
