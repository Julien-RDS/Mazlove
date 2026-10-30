import HomeTrackingSection from "./tracking/HomeTrackingSection";
import { tokenStore } from "../api";
import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowRight,
  ArrowUpRight,
  Asterisk,
  BadgeCheck,
  CalendarDays,
  Heart,
  Leaf,
  MapPin,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Star,
  X,
} from "lucide-react";
import { Api } from "../api";
import { loginPath, navigate } from "../lib/router";
import { formatRating, toLocalDate, DAY_LABELS } from "../lib/format";
import { providerVisual } from "../lib/providerVisual";
import { Avatar, Button, Notice, Sheet } from "./ui";
import "./discover.css";

const CATEGORIES = [
  ["", "Tout explorer", Sparkles],
  ["relaxation", "Relaxation", Heart],
  ["therapeutique", "Accompagnement", ShieldCheck],
  ["meditation", "Méditation", Leaf],
];
const normalize = (text) =>
  text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

export default function DiscoverScreen({ query, position, user, notify }) {
  const favoritesOnly = query.vue === "favoris";
  const [category, setCategory] = useState("");
  const [search, setSearch] = useState("");
  const [date, setDate] = useState("");
  const [minRating, setMinRating] = useState("");
  const [verified, setVerified] = useState(false);
  const [sort, setSort] = useState("distance");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [explanation, setExplanation] = useState(false);
  const [providers, setProviders] = useState([]);
  const [favorites, setFavorites] = useState([]);
  const [favoriteError, setFavoriteError] = useState("");
  const [busy, setBusy] = useState([]);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [page, setPage] = useState(0);
  const [more, setMore] = useState(false);
  const resultsRef = useRef(null);

  useEffect(() => {
    let active = true;
    setFavorites([]);
    setFavoriteError("");
    if (user)
      Api.favorites()
        .then((data) => {
          if (active) setFavorites(data);
        })
        .catch((e) => {
          if (active) setFavoriteError(e.message);
        });
    return () => {
      active = false;
    };
  }, [user, retry]);

  useEffect(() => {
    let active = true;
    setStatus("loading");
    setError("");
    Api.providersNearby({
      ...position,
      category,
      minRating,
      date,
      page,
      size: 50,
    })
      .then((data) => {
        if (!active) return;
        setProviders((old) =>
          page
            ? [...old, ...data.filter((p) => !old.some((x) => x.id === p.id))]
            : data,
        );
        setMore(data.length === 50);
        setStatus("ready");
      })
      .catch((e) => {
        if (active) {
          setError(e.message);
          setStatus("error");
        }
      });
    return () => {
      active = false;
    };
  }, [position.lat, position.lng, category, minRating, date, page, retry]);

  const updateFilter = (setter, value) => {
    setPage(0);
    setter(value);
  };
  const reset = () => {
    setPage(0);
    setCategory("");
    setSearch("");
    setDate("");
    setMinRating("");
    setVerified(false);
    setSort("distance");
  };
  const activeCount = Number(!!date) + Number(!!minRating) + Number(verified);
  const favoriteIds = new Set(favorites.map((p) => p.id));
  const visible = useMemo(() => {
    let data = favoritesOnly ? favorites : providers;
    if (favoritesOnly) {
      if (category)
        data = data.filter((p) => normalize(p.category) === category);
      if (minRating) data = data.filter((p) => p.rating >= Number(minRating));
      if (date) {
        const day = [
          "SUNDAY",
          "MONDAY",
          "TUESDAY",
          "WEDNESDAY",
          "THURSDAY",
          "FRIDAY",
          "SATURDAY",
        ][new Date(date + "T12:00:00").getDay()];
        data = data.filter((p) =>
          p.availability?.some((s) => s.dayOfWeek === day),
        );
      }
    }
    const term = normalize(search.trim());
    data = data.filter(
      (p) =>
        (!verified || p.verified) &&
        (!term ||
          normalize(p.userName + " " + p.category + " " + p.bio).includes(
            term,
          )),
    );
    return [...data].sort((a, b) =>
      sort === "rating"
        ? b.rating - a.rating
        : (a.distanceKm ?? Infinity) - (b.distanceKm ?? Infinity),
    );
  }, [
    providers,
    favorites,
    favoritesOnly,
    search,
    verified,
    sort,
    category,
    date,
    minRating,
  ]);

  const toggleFavorite = async (provider) => {
    if (!user) {
      navigate(loginPath("/?vue=favoris"));
      return;
    }
    if (favoriteError) {
      notify(favoriteError);
      return;
    }
    setBusy((ids) => [...ids, provider.id]);
    try {
      if (favoriteIds.has(provider.id)) {
        await Api.removeFavorite(provider.id);
        setFavorites((old) => old.filter((p) => p.id !== provider.id));
        notify("Praticien retiré de vos favoris.");
      } else {
        await Api.addFavorite(provider.id);
        setFavorites((old) => [...old, provider]);
        notify("Praticien ajouté à vos favoris.");
      }
    } catch (e) {
      notify(e.message);
    } finally {
      setBusy((ids) => ids.filter((id) => id !== provider.id));
    }
  };

  const scrollToResults = () =>
    resultsRef.current?.scrollIntoView({
      behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
        ? "auto"
        : "smooth",
      block: "start",
    });
  return (
    <div className="discovery">
      <div className="discovery-topline">
        <span>
          <span className="location-dot" /> À votre rythme. Près de vous.
        </span>
        <span>
          <MapPin size={14} /> Paris et ses alentours
        </span>
      </div>
      {!favoritesOnly && (
        <section className="discovery-hero" aria-labelledby="page-title">
          <div className="hero-copy">
            <span className="eyebrow">LE BIEN-ÊTRE COMMENCE PAR UN LIEN</span>
            <h1 id="page-title" tabIndex={-1}>
              Moins de bruit.
              <br />
              Plus de <em>lien.</em>
              <Asterisk className="hero-spark" aria-hidden="true" />
            </h1>
            <p>
              Une pause, une présence, un câlin.
              <br />
              Trouvez la personne avec qui vous sentir bien.
            </p>
            <button className="pill-button dark" onClick={scrollToResults}>
              Trouver ma parenthèse <ArrowUpRight size={20} />
            </button>
            <button
              type="button"
              className="home-hero-follow"
              onClick={() => {
                const section = document.getElementById("home-tracking-title");
                section?.scrollIntoView({
                  behavior: window.matchMedia(
                    "(prefers-reduced-motion: reduce)",
                  ).matches
                    ? "auto"
                    : "smooth",
                  block: "center",
                });
                section?.focus({ preventScroll: true });
              }}
            >
              Retrouver le suivi de ma séance{" "}
              <ArrowRight size={15} aria-hidden="true" />
            </button>
            <div className="hero-assurance">
              <ShieldCheck size={17} />
              <span>Des séances professionnelles, toujours non sexuelles.</span>
            </div>
          </div>
          <div className="hero-visual">
            <img
              src="/images/hero-friends.jpg"
              alt="Un groupe d’amis enlacés face à la mer"
              fetchPriority="high"
            />
            <div className="hero-photo-note">
              <Asterisk className="note-flower" aria-hidden="true" />
              <span>
                Le temps de souffler.
                <br />
                <strong>Le plaisir de se retrouver.</strong>
              </span>
            </div>
            <span className="hero-sticker">
              un peu de
              <br />
              <em>douceur</em> ♡
            </span>
          </div>
        </section>
      )}

      {!favoritesOnly && (
        <HomeTrackingSection
          user={user}
          token={user ? tokenStore.get() : null}
          position={position}
        />
      )}

      <div className="discovery-layout">
        <section
          className="discovery-results"
          ref={resultsRef}
          aria-labelledby={favoritesOnly ? "page-title" : "results-title"}
        >
          <div className="section-heading">
            <div>
              <span className="eyebrow">
                {favoritesOnly
                  ? "VOTRE SÉLECTION"
                  : "UNE RENCONTRE QUI VOUS RESSEMBLE"}
              </span>
              <h2
                id={favoritesOnly ? "page-title" : "results-title"}
                tabIndex={-1}
              >
                {favoritesOnly
                  ? "Vos coups de cœur"
                  : "Avec qui faire une pause ?"}
              </h2>
            </div>
            <span className="heading-heart" aria-hidden="true">
              <Heart size={24} />
            </span>
          </div>
          <div className="category-tabs" aria-label="Type de séance">
            {CATEGORIES.map(([value, label, Icon]) => (
              <button
                key={value}
                aria-pressed={category === value}
                onClick={() => updateFilter(setCategory, value)}
                className={category === value ? "active" : ""}
              >
                <Icon size={17} />
                {label}
              </button>
            ))}
          </div>
          <div className="search-row">
            <label className="discovery-search">
              <Search size={19} />
              <span className="sr-only">
                Rechercher un praticien dans les profils chargés
              </span>
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Un prénom, une envie…"
              />
              {search && (
                <button
                  onClick={() => setSearch("")}
                  aria-label="Effacer la recherche"
                >
                  <X size={16} />
                </button>
              )}
            </label>
            <button
              className={
                "filter-button " + (filtersOpen || activeCount ? "active" : "")
              }
              aria-expanded={filtersOpen}
              aria-controls="discovery-filters"
              onClick={() => setFiltersOpen(!filtersOpen)}
            >
              <SlidersHorizontal size={17} />
              Filtres{activeCount > 0 && <b>{activeCount}</b>}
            </button>
          </div>
          {filtersOpen && (
            <div className="filter-panel" id="discovery-filters">
              <label>
                Jour souhaité
                <input
                  type="date"
                  aria-label="Jour souhaité"
                  min={toLocalDate(new Date())}
                  value={date}
                  onChange={(e) => updateFilter(setDate, e.target.value)}
                />
              </label>
              <label>
                Note minimale
                <select
                  value={minRating}
                  onChange={(e) => updateFilter(setMinRating, e.target.value)}
                >
                  <option value="">Toutes les notes</option>
                  <option value="4">4 étoiles et plus</option>
                  <option value="4.5">4,5 étoiles et plus</option>
                </select>
              </label>
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={verified}
                  onChange={(e) => setVerified(e.target.checked)}
                />
                Identité vérifiée
              </label>
              <button onClick={reset} className="reset-link">
                Tout réinitialiser
              </button>
              {date && (
                <p>
                  Selon les jours d’accueil publiés. Le créneau exact se
                  confirme à la réservation.
                </p>
              )}
            </div>
          )}
          <div className="results-toolbar">
            <span aria-live="polite">
              {status === "loading" && !favoritesOnly
                ? "Recherche en cours…"
                : visible.length +
                  " profil" +
                  (visible.length > 1 ? "s" : "") +
                  (favoritesOnly ? " en favoris" : " à découvrir")}
            </span>
            <label>
              Trier par{" "}
              <select
                aria-label="Trier les praticiens"
                value={sort}
                onChange={(e) => setSort(e.target.value)}
              >
                <option value="distance">Proximité</option>
                <option value="rating">Meilleures notes</option>
              </select>
            </label>
          </div>
          {favoritesOnly && !user ? (
            <div className="empty-results">
              <Heart size={32} />
              <h3>Gardez vos belles rencontres.</h3>
              <p>Connectez-vous pour retrouver vos praticiens favoris.</p>
              <a
                className="pill-button dark"
                href={"#" + loginPath("/?vue=favoris")}
              >
                Se connecter <ArrowRight size={17} />
              </a>
            </div>
          ) : status === "error" && !favoritesOnly ? (
            <Notice
              tone="error"
              action={
                <Button onClick={() => setRetry(retry + 1)}>Réessayer</Button>
              }
            >
              {error}
            </Notice>
          ) : favoriteError && favoritesOnly ? (
            <Notice
              tone="error"
              action={
                <Button onClick={() => setRetry(retry + 1)}>Réessayer</Button>
              }
            >
              {favoriteError}
            </Notice>
          ) : status === "loading" && !favoritesOnly ? (
            <div
              className="provider-grid"
              role="status"
              aria-label="Chargement des praticiens"
            >
              {[1, 2, 3].map((n) => (
                <div key={n} className="provider-skeleton" />
              ))}
            </div>
          ) : visible.length === 0 ? (
            <div className="empty-results">
              <Search size={32} />
              <h3>
                {favoritesOnly
                  ? "Votre sélection commence ici."
                  : "Pas encore de rencontre ici."}
              </h3>
              <p>
                {favoritesOnly
                  ? "Touchez le cœur sur une fiche pour la retrouver plus tard."
                  : "Essayez un autre jour ou élargissez vos critères."}
              </p>
              <button
                className="pill-button dark"
                onClick={() => {
                  reset();
                  if (favoritesOnly) navigate("/");
                }}
              >
                Explorer les profils <ArrowRight size={17} />
              </button>
            </div>
          ) : (
            <div className="provider-grid">
              {visible.map((p) => (
                <ProviderCard
                  key={p.id}
                  provider={p}
                  favorite={favoriteIds.has(p.id)}
                  busy={busy.includes(p.id)}
                  onFavorite={() => toggleFavorite(p)}
                />
              ))}
            </div>
          )}
          {more && !favoritesOnly && status === "ready" && (
            <Button variant="secondary" onClick={() => setPage(page + 1)}>
              Charger plus de profils
            </Button>
          )}
          <p className="results-footnote">
            <MapPin size={13} /> Recherche dans un rayon de 50 km autour de
            Paris. Photos de démonstration signalées.
          </p>
        </section>

        <aside className="discovery-sidebar">
          <div className="pause-card">
            <div className="pause-card-top">
              <span className="eyebrow">VOTRE PROCHAINE PAUSE</span>
              <CalendarDays size={21} />
            </div>
            <h2>
              On se garde
              <br />
              un moment ?
            </h2>
            <p>
              Choisissez votre jour pour découvrir les praticiens qui reçoivent.
            </p>
            <div className="date-strip">
              {Array.from({ length: 5 }, (_, i) => {
                const d = new Date();
                d.setDate(d.getDate() + i);
                const value = toLocalDate(d);
                return (
                  <button
                    key={value}
                    aria-pressed={date === value}
                    aria-label={d.toLocaleDateString("fr-FR", {
                      weekday: "long",
                      day: "numeric",
                      month: "long",
                    })}
                    className={date === value ? "active" : ""}
                    onClick={() => {
                      updateFilter(setDate, date === value ? "" : value);
                      setFiltersOpen(true);
                      scrollToResults();
                    }}
                  >
                    <span>
                      {d
                        .toLocaleDateString("fr-FR", { weekday: "short" })
                        .replace(".", "")}
                    </span>
                    <strong>{d.getDate()}</strong>
                  </button>
                );
              })}
            </div>
            <button
              className="pause-date-link"
              onClick={() => {
                setFiltersOpen(true);
                scrollToResults();
              }}
            >
              Choisir une autre date <ArrowRight size={16} />
            </button>
          </div>
          <div className="reassurance-card">
            <span className="reassurance-icon">
              <Heart size={24} />
            </span>
            <h3>
              Votre confort.
              <br />
              Vos règles.
            </h3>
            <p>
              Vous définissez vos limites. Vous pouvez faire une pause ou
              arrêter à tout moment.
            </p>
            <button onClick={() => setExplanation(true)}>
              Comment ça marche <ArrowUpRight size={16} />
            </button>
          </div>
        </aside>
      </div>
      <section className="discovery-bottom">
        <Asterisk className="bottom-flower" aria-hidden="true" />
        <div>
          <span className="eyebrow">UNE PREMIÈRE FOIS ?</span>
          <h2>Vous pouvez juste être vous.</h2>
          <p>
            Pas de performance. Pas d’attente. Un moment pour prendre soin de
            vous.
          </p>
        </div>
        <button
          className="pill-button light"
          onClick={() => setExplanation(true)}
        >
          Découvrir l’expérience <ArrowRight size={18} />
        </button>
      </section>
      {explanation && (
        <Sheet
          title="Une rencontre, en toute simplicité"
          onClose={() => setExplanation(false)}
        >
          <ol className="experience-steps">
            {[
              [
                "Choisissez votre praticien",
                "Consultez sa présentation, ses prestations et les avis de clients.",
              ],
              [
                "Réservez votre moment",
                "Connectez-vous, choisissez un créneau et partagez vos envies et vos limites.",
              ],
              [
                "Prenez le temps",
                "Votre code d’arrivée démarre la séance. Vous gardez la liberté de faire une pause ou d’arrêter.",
              ],
            ].map(([title, text], i) => (
              <li key={title}>
                <span>{i + 1}</span>
                <div>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </div>
              </li>
            ))}
          </ol>
          <Notice>
            Les séances sont strictement non sexuelles. L’annulation est
            possible au moins 24 heures avant le début.
          </Notice>
        </Sheet>
      )}
    </div>
  );
}

function ProviderCard({ provider, favorite, busy, onFavorite }) {
  const visual = providerVisual(provider);
  const days = [
    ...new Set(
      provider.availability?.map((s) => DAY_LABELS[s.dayOfWeek]) || [],
    ),
  ];
  return (
    <article className="discovery-provider-card">
      <div className="provider-photo">
        <a
          href={"#/praticien/" + provider.id}
          aria-label={"Voir le profil de " + provider.userName}
        >
          <Avatar
            name={provider.userName}
            src={visual.src}
            className="provider-portrait"
          />
        </a>
        <span className="provider-distance">
          <MapPin size={12} />
          {provider.distanceKm != null
            ? provider.distanceKm.toLocaleString("fr-FR") + " km"
            : "Voir la fiche"}
        </span>
        <button
          className={"favorite-toggle " + (favorite ? "selected" : "")}
          aria-label={
            (favorite ? "Retirer " : "Ajouter ") +
            provider.userName +
            (favorite ? " des favoris" : " aux favoris")
          }
          aria-pressed={favorite}
          disabled={busy}
          onClick={onFavorite}
        >
          <Heart size={19} fill={favorite ? "currentColor" : "none"} />
        </button>
        {visual.isIllustration && (
          <span className="photo-illustration">Photo d’illustration</span>
        )}
      </div>
      <div className="provider-card-info">
        <div className="provider-card-title">
          <a href={"#/praticien/" + provider.id}>
            <h3>{provider.userName}</h3>
          </a>
          {provider.reviewCount > 0 && (
            <span className="provider-rating">
              <Star size={13} fill="currentColor" />
              {formatRating(provider.rating)}{" "}
              <small>({provider.reviewCount})</small>
            </span>
          )}
        </div>
        <p className="provider-category">
          {CATEGORIES.find(([key]) => key === provider.category)?.[1] ||
            provider.category}
        </p>
        <div className="provider-badges">
          {provider.verified && (
            <span>
              <BadgeCheck size={14} />
              Identité vérifiée
            </span>
          )}
        </div>
        <div className="provider-card-footer">
          <span>
            <CalendarDays size={14} />
            {days.length
              ? days
                  .slice(0, 2)
                  .map((d) => d.slice(0, 3) + ".")
                  .join(" · ")
              : "Voir les disponibilités"}
          </span>
          <a
            href={"#/praticien/" + provider.id}
            aria-label={"Découvrir " + provider.userName}
          >
            <ArrowUpRight size={22} />
          </a>
        </div>
      </div>
    </article>
  );
}
