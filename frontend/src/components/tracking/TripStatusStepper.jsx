import React from "react";
export const labels = {
  PENDING: "En attente",
  ACCEPTED: "Acceptée",
  EN_ROUTE: "En route",
  ARRIVED: "Arrivé",
  IN_PROGRESS: "En cours",
  DONE: "Terminé",
  CANCELLED: "Annulée",
};
export default function TripStatusStepper({ status }) {
  return (
    <div aria-live="polite">
      <strong>Statut : {labels[status] || "Chargement…"}</strong>
      <ol className="trip-steps">
        {["ACCEPTED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS", "DONE"].map((s) => (
          <li key={s} aria-current={status === s ? "step" : undefined}>
            {labels[s]}
          </li>
        ))}
      </ol>
    </div>
  );
}
