import React from "react";
import PrivacyNotice from "./PrivacyNotice";
export default function ConsentModal({ onAccept, busy }) {
  return (
    <section className="tracking-dialog" aria-label="Consentement au partage">
      <h3>Partager ma position pendant le trajet</h3>
      <PrivacyNotice />
      <button disabled={busy} onClick={onAccept}>
        J’accepte le partage de ma position
      </button>
    </section>
  );
}
