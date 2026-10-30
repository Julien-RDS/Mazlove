import React, { useState } from "react";
import {
  ArrowUpRight,
  Navigation,
  MapPin,
  ShieldCheck,
  Clock3,
  Route,
} from "lucide-react";
import { loginPath } from "../../lib/router";
import { useHomeSession } from "../../hooks/useHomeSession";
import { useLiveTracking } from "../../hooks/useLiveTracking";
import { labels } from "./TripStatusStepper";
import EtaCard from "./EtaCard";
import TrackingMap from "./LazyTrackingMap";
import OnlineProviders from "./OnlineProviders";
import "./home-tracking.css";
function ActiveTrip({ session, token }) {
  const live = useLiveTracking(session.id, token),
    [mapOpen, setMapOpen] = useState(false);
  const status = live.status || session.status,
    provider = session.viewerRole === "PROVIDER";
  const title = provider
    ? `Votre rendez-vous avec ${session.clientName}`
    : `Votre rendez-vous avec ${session.providerName}`;
  return (
    <article className="home-trip" aria-label="Votre séance à suivre">
      <div className="home-trip-top">
        <span
          className={`home-trip-status ${status === "EN_ROUTE" ? "is-live" : ""}`}
        >
          <span aria-hidden="true" />
          {labels[status] || "Séance à venir"}
        </span>
        <Clock3 size={18} aria-hidden="true" />
      </div>
      <h3>{title}</h3>
      <p className="home-trip-date">
        {new Date(session.datetime).toLocaleString("fr-FR", {
          day: "numeric",
          month: "long",
          hour: "2-digit",
          minute: "2-digit",
        })}
      </p>
      {status === "EN_ROUTE" ? (
        <EtaCard {...live} />
      ) : (
        <p>
          {status === "ARRIVED"
            ? "Le prestataire est arrivé. Retrouvez-vous et validez le code d’arrivée."
            : status === "IN_PROGRESS"
              ? "Votre séance a commencé. Retrouvez les actions et l’aide dans son détail."
              : status === "DONE"
                ? "La séance est terminée. Merci de votre confiance."
                : status === "CANCELLED"
                  ? "Cette séance a été annulée."
                  : provider
                    ? "Retrouvez la console pour accepter la réservation et préparer votre départ."
                    : "Le suivi sera disponible dès le départ de votre prestataire."}
        </p>
      )}
      {live.error && <p role="alert">{live.error}</p>}
      <a className="pill-button dark" href={`#/seances/${session.id}`}>
        {provider
          ? "Ouvrir ma console"
          : status === "EN_ROUTE"
            ? "Suivre son arrivée"
            : "Voir ma séance"}
        <ArrowUpRight size={18} aria-hidden="true" />
      </a>
      {status === "EN_ROUTE" && (
        <>
          <button
            type="button"
            className="home-map-toggle"
            aria-expanded={mapOpen}
            aria-controls="home-trip-map"
            onClick={() => setMapOpen((v) => !v)}
          >
            <MapPin size={16} aria-hidden="true" />
            {mapOpen ? "Masquer le trajet" : "Afficher le trajet ici"}
          </button>
          {mapOpen && (
            <div id="home-trip-map">
              <TrackingMap
                pos={live.pos}
                route={live.route}
                meetingPoint={live.meetingPoint || session.meetingPoint}
                stale={live.stale}
              />
            </div>
          )}
        </>
      )}
    </article>
  );
}
export default function HomeTrackingSection({ user, token, position }) {
  const { session, loading, error } = useHomeSession(token);
  return (
    <section
      className="home-tracking"
      aria-labelledby="home-tracking-title"
      id="suivi-accueil"
    >
      <div className="home-tracking-grid">
        <div className="home-tracking-copy">
          <span className="eyebrow">
            <Navigation size={14} aria-hidden="true" />
            LE LIEN COMMENCE AVANT LE RENDEZ-VOUS
          </span>
          <h2 id="home-tracking-title" tabIndex={-1}>
            Du départ au
            <br />
            <em>premier bonjour.</em>
          </h2>
          <p>
            Votre prestataire vient à vous. Retrouvez son trajet et son heure
            d’arrivée estimée, directement depuis votre séance.
          </p>
          <ol className="home-tracking-steps">
            <li>
              <Route size={19} aria-hidden="true" />
              <span>
                <strong>Suivez son arrivée</strong>Le trajet se met à jour
                pendant le déplacement.
              </span>
            </li>
            <li>
              <ShieldCheck size={19} aria-hidden="true" />
              <span>
                <strong>Restez serein</strong>Partagez le trajet avec un proche
                et gardez l’aide à portée de main.
              </span>
            </li>
            <li>
              <MapPin size={19} aria-hidden="true" />
              <span>
                <strong>Gardez le contrôle</strong>Le partage de position
                s’arrête à l’arrivée.
              </span>
            </li>
          </ol>
          {import.meta.env.VITE_DEMO === "true" && (
            <a className="home-demo-link" href="/demo">
              Essayer la démonstration
              <ArrowUpRight size={16} aria-hidden="true" />
            </a>
          )}
        </div>
        <div className="home-tracking-current">
          {session ? (
            <ActiveTrip
              key={`${user.id}:${session.id}`}
              session={session}
              token={token}
            />
          ) : (
            <article className="home-trip home-trip-empty">
              <span className="home-empty-icon">
                <Navigation size={28} aria-hidden="true" />
              </span>
              <h3>
                {loading
                  ? "Nous retrouvons votre séance…"
                  : error
                    ? "Votre séance reste accessible"
                    : user
                      ? "Votre prochain rendez-vous commence ici"
                      : "Un rendez-vous, en toute confiance"}
              </h3>
              <p role={error ? "alert" : "status"}>
                {error ||
                  (loading
                    ? "Encore un instant."
                    : user
                      ? "Réservez une séance : son suivi apparaîtra ici dès qu’elle sera confirmée."
                      : "Connectez-vous pour retrouver vos séances et suivre l’arrivée de votre prestataire.")}
              </p>
              <a
                className="pill-button dark"
                href={user ? "#/seances" : `#${loginPath("/seances")}`}
              >
                {user ? "Retrouver mes séances" : "Me connecter"}
                <ArrowUpRight size={18} aria-hidden="true" />
              </a>
            </article>
          )}
        </div>
      </div>
      <OnlineProviders
        key={user?.id || "visitor"}
        token={token}
        user={user}
        position={position}
      />
    </section>
  );
}
