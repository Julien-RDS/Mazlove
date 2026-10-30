import React, { useState } from "react";
import MeetingPointPicker from "./MeetingPointPicker";
import { trackingApi } from "../../lib/trackingApi";
import { useLiveTracking } from "../../hooks/useLiveTracking";
import TrackingMap from "./LazyTrackingMap";
import TripStatusStepper from "./TripStatusStepper";
import EtaCard from "./EtaCard";
import ProviderConsole from "./ProviderConsole";
import ShareTripButton from "./ShareTripButton";
import SosButton from "./SosButton";
import TrustedContacts from "./TrustedContacts";
export default function TripPanel({
  session,
  token,
  simulated = false,
  onChange,
}) {
  const [chosen, setChosen] = useState(null),
    [pointError, setPointError] = useState("");
  const live = useLiveTracking(session.id, token);
  const status = live.status || session.status;
  const meetingPoint = live.meetingPoint || session.meetingPoint;
  return (
    <section className="tracking-panel">
      <TripStatusStepper status={status} />
      {session.viewerRole === "CLIENT" &&
        !meetingPoint &&
        ["PENDING", "ACCEPTED"].includes(status) && (
          <section>
            <MeetingPointPicker
              token={token}
              value={chosen}
              onChange={setChosen}
            />
            <button
              disabled={!chosen}
              onClick={async () => {
                try {
                  const next = await trackingApi(
                    `/sessions/${session.id}/meeting-point`,
                    token,
                    "PUT",
                    chosen,
                  );
                  onChange?.(next);
                } catch (e) {
                  setPointError(e.message);
                }
              }}
            >
              Enregistrer le rendez-vous
            </button>
            <p role="alert">{pointError}</p>
          </section>
        )}
      {status !== "PENDING" && (
        <TrackingMap
          pos={live.pos}
          meetingPoint={meetingPoint}
          route={live.route}
          stale={live.stale}
        />
      )}
      {status === "EN_ROUTE" && <EtaCard {...live} />}
      <p role="status">{live.message || live.error}</p>
      {session.viewerRole === "PROVIDER" && (
        <ProviderConsole
          id={session.id}
          token={token}
          status={status}
          meetingPoint={meetingPoint}
          clientName={session.clientName}
          simulated={simulated}
          onChange={onChange}
        />
      )}
      {session.viewerRole === "CLIENT" && status === "ARRIVED" && (
        <p>
          Votre code d’arrivée : <strong>{session.arrivalCode}</strong>
        </p>
      )}
      {status === "EN_ROUTE" && (
        <ShareTripButton id={session.id} token={token} />
      )}
      {["EN_ROUTE", "ARRIVED", "IN_PROGRESS"].includes(status) && (
        <SosButton id={session.id} token={token} />
      )}
      <TrustedContacts token={token} />
    </section>
  );
}
