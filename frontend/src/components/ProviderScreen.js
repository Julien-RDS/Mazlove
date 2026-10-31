import MeetingPointPicker from "./tracking/MeetingPointPicker";
import { tokenStore } from "../api";
import React, { useEffect, useMemo, useState } from "react";
import {
  ArrowUpRight,
  CalendarDays,
  ChevronLeft,
  Clock3,
  MapPin,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { Api } from "../api";
import { loginPath, navigate } from "../lib/router";
import { providerVisual } from "../lib/providerVisual";
import {
  DAY_LABELS,
  capitalize,
  dayOfWeekOf,
  distanceKm,
  firstName,
  formatDistance,
  formatLongDate,
  formatMonthYear,
  formatRating,
  formatDayMonth,
  formatWeekday,
  fromMinutes,
  isSameDay,
  parseLocalDateTime,
  toLocalDate,
  toMinutes,
} from "../lib/format";
import {
  Avatar,
  Button,
  CharCount,
  Field,
  Notice,
  Sheet,
  Spinner,
  Stars,
  VerifiedBadge,
  cx,
  inputClass,
} from "./ui";

/** Fiche détaillée d'un praticien, avec réservation dans un panneau. */
export default function ProviderScreen({ id, user, position, notify }) {
  const [provider, setProvider] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [services, setServices] = useState([]);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState(null);
  const [booking, setBooking] = useState(false);
  const [selectedService, setSelectedService] = useState(null);

  const load = async () => {
    setStatus("loading");
    try {
      const [p, r, s] = await Promise.all([
        Api.provider(id),
        Api.providerReviews(id),
        Api.providerServices(id),
      ]);
      setProvider(p);
      setReviews(r);
      setServices(s);
      setStatus("ready");
    } catch (e) {
      setError(e);
      setStatus("error");
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (status === "loading") {
    return (
      <>
        <h1 id="page-title" tabIndex={-1} className="sr-only outline-none">
          Fiche du praticien
        </h1>
        <Spinner label="Chargement de la fiche" />
      </>
    );
  }

  if (status === "error") {
    return (
      <div className="space-y-4">
        <BackLink />
        <h1
          id="page-title"
          tabIndex={-1}
          className="text-3xl font-semibold outline-none"
        >
          Fiche indisponible
        </h1>
        <Notice
          tone="error"
          action={
            error.status === 404 ? (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => navigate("/?etape=recherche")}
              >
                Voir les autres praticiens
              </Button>
            ) : (
              <Button size="sm" variant="secondary" onClick={load}>
                Réessayer
              </Button>
            )
          }
        >
          {error.message}
        </Notice>
      </div>
    );
  }

  const name = provider.userName;
  const isOwnProfile = user?.providerId === provider.id;
  const distance = formatDistance(distanceKm(position, provider.location));
  const startBooking = (service = services[0]) => {
    setSelectedService(service || null);
    if (!user) navigate(loginPath(`/praticien/${provider.id}`));
    else setBooking(true);
  };
  const startingPrice = services.length
    ? Math.min(...services.map((service) => service.price))
    : null;
  const visual = providerVisual(provider);

  return (
    <div className="provider-detail space-y-8 pb-8">
      <BackLink />

      <section className="provider-hero grid overflow-hidden rounded-[2rem] bg-rose-pale sm:grid-cols-[minmax(0,22rem)_1fr] sm:items-center">
        <div className="relative">
          <Gallery
            name={name}
            images={visual.isIllustration ? [visual.src] : provider.images}
          />
          {visual.isIllustration && (
            <span className="absolute bottom-3 left-3 rounded-full bg-black/50 px-3 py-1 text-xs text-white">
              Photo d’illustration
            </span>
          )}
        </div>
        <div className="space-y-5 p-6 sm:p-9">
          <span className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.18em]">
            <Sparkles size={15} aria-hidden="true" /> Votre prochaine parenthèse
          </span>
          <h1
            id="page-title"
            tabIndex={-1}
            className="text-5xl font-semibold outline-none sm:text-6xl"
          >
            {name}
          </h1>
          <p className="text-lg text-nuit-800">{provider.category}</p>
          {distance && (
            <p className="flex items-center gap-2 text-sm">
              <MapPin size={16} aria-hidden="true" /> {distance} de{" "}
              {position.label}
            </p>
          )}
          <div className="flex flex-wrap items-center gap-3">
            {provider.verified && <VerifiedBadge />}
            {provider.reviewCount > 0 ? (
              <span className="inline-flex items-center gap-2">
                <Stars value={provider.rating} />
                <span className="font-bold" aria-hidden="true">
                  {formatRating(provider.rating)}
                </span>
                <span className="text-nuit-800">
                  ({provider.reviewCount} avis)
                </span>
              </span>
            ) : (
              <span className="text-nuit-800">Nouveau sur Mazlove</span>
            )}
          </div>
          <p className="max-w-prose whitespace-pre-line pt-2">{provider.bio}</p>
          <button
            type="button"
            onClick={() =>
              document
                .getElementById(services.length ? "prestations" : "dispos")
                ?.scrollIntoView({
                  behavior: window.matchMedia(
                    "(prefers-reduced-motion: reduce)",
                  ).matches
                    ? "auto"
                    : "smooth",
                  block: "start",
                })
            }
            className="inline-flex items-center gap-2 rounded-full bg-nuit px-6 py-3 font-bold text-sur-nuit"
          >
            Découvrir les séances <ArrowUpRight size={18} aria-hidden="true" />
          </button>
        </div>
      </section>

      <div className="grid gap-10 lg:grid-cols-[1fr_22rem]">
        <div className="space-y-10">
          {services.length > 0 && (
            <section aria-labelledby="prestations" className="space-y-4">
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-nuit-doux">
                Un moment à votre rythme
              </p>
              <h2
                id="prestations"
                className="scroll-mt-28 text-3xl font-semibold"
              >
                Choisissez votre séance
              </h2>
              <ul className="space-y-2">
                {services.map((service) => (
                  <li
                    key={service.id}
                    className="flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-lilas bg-carte p-5 sm:p-6"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="font-bold text-lg">{service.name}</p>
                      <p className="mt-1 text-sm text-nuit-doux">
                        {service.description}
                      </p>
                      <p className="mt-3 flex items-center gap-1.5 text-sm">
                        <Clock3 size={14} aria-hidden="true" />{" "}
                        {service.duration} h
                      </p>
                    </div>
                    <div className="space-y-3 text-right">
                      <p className="text-2xl font-bold">
                        {new Intl.NumberFormat("fr-FR", {
                          style: "currency",
                          currency: "EUR",
                          maximumFractionDigits: 0,
                        }).format(service.price)}
                      </p>
                      {!isOwnProfile && (
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => startBooking(service)}
                          disabled={provider.availability.length === 0}
                          aria-label={`Choisir ${service.name}`}
                        >
                          Choisir <ArrowUpRight size={15} aria-hidden="true" />
                        </Button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <section
            aria-labelledby="deroule"
            className="space-y-5 rounded-3xl bg-rose-pale p-6"
          >
            <h2 id="deroule" className="text-2xl font-semibold">
              Comment se passe une séance
            </h2>
            <ol className="space-y-4">
              {[
                [
                  "Vous réservez",
                  "Choisissez un créneau et indiquez vos limites : zones à éviter, positions, envie de parler ou non.",
                ],
                [
                  "Vous donnez votre code",
                  `À l'arrivée, vous montrez votre code à ${firstName(name)}. La séance démarre quand il est saisi.`,
                ],
                [
                  "Vous restez libre",
                  "Chacun peut faire une pause ou arrêter, sans se justifier. Après la séance, vous laissez un avis.",
                ],
              ].map(([title, text], index) => (
                <li key={title} className="grid grid-cols-[2.25rem_1fr] gap-3">
                  <span
                    aria-hidden="true"
                    className="flex h-9 w-9 items-center justify-center rounded-full bg-carte font-display text-lg font-semibold"
                  >
                    {index + 1}
                  </span>
                  <div>
                    <p className="font-bold">{title}</p>
                    <p className="text-nuit-doux">{text}</p>
                  </div>
                </li>
              ))}
            </ol>
          </section>

          <section aria-labelledby="avis" className="space-y-4">
            <h2 id="avis" className="text-2xl font-semibold">
              Avis{" "}
              {reviews.length > 0 && (
                <span className="text-nuit-doux">({reviews.length})</span>
              )}
            </h2>
            {reviews.length === 0 ? (
              <p className="text-nuit-doux">
                Pas encore d'avis. Sur Mazlove, seuls les clients d'une séance
                terminée peuvent en laisser un.
              </p>
            ) : (
              <ul className="space-y-3">
                {reviews.map((review) => (
                  <li
                    key={review.id}
                    className="space-y-1.5 rounded-2xl bg-carte p-4"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="font-bold">{review.authorName}</p>
                      <Stars value={review.rating} />
                    </div>
                    {review.comment && <p>{review.comment}</p>}
                    <p className="text-[0.9rem] text-nuit-doux">
                      {capitalize(
                        formatMonthYear(parseLocalDateTime(review.createdAt)),
                      )}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <aside
          aria-labelledby="dispos"
          className="space-y-4 lg:sticky lg:top-24 lg:self-start"
        >
          <div className="space-y-5 rounded-[2rem] border border-lilas bg-carte p-6 shadow-sm">
            <span className="inline-flex items-center gap-2 rounded-full bg-rose-pale px-3 py-1.5 text-xs font-bold uppercase tracking-wider">
              <CalendarDays size={14} aria-hidden="true" /> Votre moment
              commence ici
            </span>
            {startingPrice !== null && (
              <p className="text-sm text-nuit-doux">
                À partir de{" "}
                <strong className="ml-1 text-3xl text-nuit">
                  {startingPrice} €
                </strong>
                <span className="ml-1">/ séance</span>
              </p>
            )}
            <h2 id="dispos" className="text-2xl font-semibold">
              Disponibilités
            </h2>
            <AvailabilityList availability={provider.availability} />
            {isOwnProfile ? (
              <Notice>
                C'est votre fiche : les clients la voient exactement ainsi.
              </Notice>
            ) : (
              <Button
                size="lg"
                className="w-full"
                onClick={() => startBooking()}
                disabled={provider.availability.length === 0}
              >
                {user ? "Choisir mon créneau" : "Se connecter pour réserver"}{" "}
                <ArrowUpRight size={18} aria-hidden="true" />
              </Button>
            )}
            <p className="flex items-start gap-2 text-xs leading-relaxed text-nuit-doux">
              <ShieldCheck size={17} className="shrink-0" aria-hidden="true" />{" "}
              Un cadre clair, vos limites respectées. Annulation possible
              jusqu'à 24 h avant la séance.
            </p>
          </div>
        </aside>
      </div>

      {booking && (
        <BookingSheet
          provider={provider}
          services={services}
          initialService={selectedService}
          onClose={() => setBooking(false)}
          onBooked={(session) => {
            setBooking(false);
            notify("Séance réservée.");
            navigate(`/seances/${session.id}`);
          }}
        />
      )}
    </div>
  );
}

function BackLink() {
  return (
    <a
      href="#/?etape=recherche"
      className="inline-flex items-center gap-1 font-bold text-nuit-doux hover:text-nuit"
    >
      <ChevronLeft className="h-4 w-4" aria-hidden="true" />
      Retour à la recherche
    </a>
  );
}

function Gallery({ name, images = [] }) {
  if (images.length <= 1) {
    return (
      <Avatar
        name={name}
        src={images[0]}
        className="aspect-[4/5] w-full text-7xl"
      />
    );
  }
  return (
    <ul
      className="flex snap-x snap-mandatory gap-3 overflow-x-auto pb-2"
      aria-label={`Photos de ${name}`}
    >
      {images.map((src, index) => (
        <li key={src} className="w-[70%] shrink-0 snap-start sm:w-full">
          <Avatar
            name={`${name} (${index + 1}/${images.length})`}
            src={src}
            className="aspect-[4/5] w-full rounded-3xl"
          />
        </li>
      ))}
    </ul>
  );
}

function AvailabilityList({ availability }) {
  if (availability.length === 0) {
    return (
      <p className="text-nuit-doux">Aucun créneau publié pour le moment.</p>
    );
  }
  return (
    <dl className="space-y-1.5">
      {availability.map((slot) => (
        <div
          key={`${slot.dayOfWeek}-${slot.startTime}`}
          className="flex justify-between gap-4"
        >
          <dt>{capitalize(DAY_LABELS[slot.dayOfWeek])}</dt>
          <dd className="tabular-nums text-nuit-doux">
            {slot.startTime} – {slot.endTime}
          </dd>
        </div>
      ))}
    </dl>
  );
}

// ─── Réservation ────────────────────────────────────────────────────────────

const DURATIONS = [1, 2, 3];
const SLOT_STEP_MINUTES = 30;
/** Pas de réservation à moins d'une heure : le praticien doit avoir le temps de s'organiser. */
const MIN_LEAD_MINUTES = 60;

/** Heures de début possibles un jour donné pour une durée donnée, d'après les créneaux hebdo. */
function startTimesFor(availability, day, durationHours, now) {
  const weekday = dayOfWeekOf(day);
  const earliest = isSameDay(day, now)
    ? now.getHours() * 60 + now.getMinutes() + MIN_LEAD_MINUTES
    : 0;
  const starts = new Set();
  availability
    .filter((slot) => slot.dayOfWeek === weekday)
    .forEach((slot) => {
      const end = toMinutes(slot.endTime);
      for (
        let t = toMinutes(slot.startTime);
        t + durationHours * 60 <= end;
        t += SLOT_STEP_MINUTES
      ) {
        if (t >= earliest) starts.add(t);
      }
    });
  return [...starts].sort((a, b) => a - b).map(fromMinutes);
}

function BookingSheet({
  provider,
  services,
  initialService,
  onClose,
  onBooked,
}) {
  const name = firstName(provider.userName);
  const [now] = useState(() => new Date());

  // Les 14 prochains jours où le praticien reçoit (cherchés sur 4 semaines).
  const days = useMemo(() => {
    const result = [];
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    for (let i = 0; i < 28 && result.length < 14; i += 1) {
      const day = new Date(
        start.getFullYear(),
        start.getMonth(),
        start.getDate() + i,
      );
      if (startTimesFor(provider.availability, day, 1, now).length > 0)
        result.push(day);
    }
    return result;
  }, [provider.availability, now]);

  const [day, setDay] = useState(days[0] || null);
  const [duration, setDuration] = useState(
    initialService?.duration || services[0]?.duration || 1,
  );
  const [serviceId, setServiceId] = useState(
    String(initialService?.id || services[0]?.id || ""),
  );
  const [time, setTime] = useState(null);
  const [notes, setNotes] = useState("");
  const [meetingPoint, setMeetingPoint] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const times = useMemo(
    () => (day ? startTimesFor(provider.availability, day, duration, now) : []),
    [provider.availability, day, duration, now],
  );

  // Un horaire qui ne tient plus (jour ou durée changés) est désélectionné plutôt que gardé invalide.
  useEffect(() => {
    if (time && !times.includes(time)) setTime(null);
  }, [times, time]);

  const endTime = time ? fromMinutes(toMinutes(time) + duration * 60) : null;
  const chosenService = services.find(
    (service) => String(service.id) === serviceId,
  );

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const session = await Api.createSession({
        providerId: provider.id,
        startTime: `${toLocalDate(day)}T${time}:00`,
        duration,
        serviceItemId: serviceId ? Number(serviceId) : null,
        notes: notes.trim() || null,
        meetingPoint,
      });
      onBooked(session);
    } catch (e) {
      setError(e.fields ? Object.values(e.fields)[0] : e.message);
      setSubmitting(false);
    }
  };

  return (
    <Sheet
      title={`Réserver avec ${name}`}
      onClose={onClose}
      footer={
        <div className="booking-summary space-y-3">
          {error && <Notice tone="error">{error}</Notice>}
          <div
            className="flex items-start justify-between gap-4"
            aria-live="polite"
          >
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-nuit-doux">
                Votre réservation
              </p>
              <p className="mt-1 text-sm font-bold">
                {day && time
                  ? `${formatLongDate(day)}, ${time} – ${endTime}`
                  : "Choisissez un jour et une heure"}
              </p>
            </div>
            <p className="shrink-0 text-xl font-bold">
              {chosenService ? `${chosenService.price} €` : "Tarif à convenir"}
            </p>
          </div>
          <p className="text-xs text-nuit-doux">
            Annulation jusqu'à 24 h avant. Aucun paiement en ligne à cette
            étape.
          </p>
          <Button
            size="lg"
            className="w-full"
            disabled={!day || !time}
            loading={submitting}
            onClick={submit}
          >
            Confirmer la réservation
          </Button>
        </div>
      }
    >
      {days.length === 0 ? (
        <Notice>
          {name} n'a aucun créneau libre dans les quatre prochaines semaines.
        </Notice>
      ) : (
        <div className="space-y-6 pt-2">
          <div className="flex items-center gap-3 rounded-2xl bg-rose-pale p-4">
            <CalendarDays className="shrink-0" size={24} aria-hidden="true" />
            <p className="text-sm">
              Un moment pour vous, avec <strong>{name}</strong>. Choisissez
              votre prestation, puis votre créneau.
            </p>
          </div>
          {services.length > 0 && (
            <Field label="Prestation">
              {(props) => (
                <select
                  {...props}
                  className={inputClass}
                  value={serviceId}
                  onChange={(event) => {
                    const id = event.target.value;
                    setServiceId(id);
                    const selected = services.find(
                      (item) => String(item.id) === id,
                    );
                    if (selected) setDuration(selected.duration);
                  }}
                >
                  {services.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name} · {item.duration} h · {item.price} €
                    </option>
                  ))}
                </select>
              )}
            </Field>
          )}
          <fieldset className="min-w-0 space-y-2">
            <legend className="font-bold">Quel jour vous convient ?</legend>
            <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
              {days.map((d) => {
                const selected = day && isSameDay(d, day);
                return (
                  <button
                    key={d.toISOString()}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => setDay(d)}
                    className={cx(
                      "flex min-h-[5rem] min-w-[4.5rem] shrink-0 flex-col items-center justify-center rounded-full px-3 py-3 transition-colors",
                      selected
                        ? "bg-nuit text-sur-nuit"
                        : "bg-lilas-pale hover:bg-lilas",
                    )}
                  >
                    <span className="text-[0.85rem]">{formatWeekday(d)}</span>
                    <span className="font-bold">{formatDayMonth(d)}</span>
                  </button>
                );
              })}
            </div>
          </fieldset>

          {!serviceId && (
            <fieldset className="space-y-2">
              <legend className="font-bold">Durée</legend>
              <div className="inline-flex rounded-full bg-brume p-1">
                {DURATIONS.map((h) => (
                  <button
                    key={h}
                    type="button"
                    aria-pressed={duration === h}
                    onClick={() => setDuration(h)}
                    className={cx(
                      "rounded-full px-5 py-1.5 font-bold transition-colors",
                      duration === h
                        ? "bg-carte text-nuit shadow-sm"
                        : "text-nuit-doux hover:text-nuit",
                    )}
                  >
                    {h} h
                  </button>
                ))}
              </div>
            </fieldset>
          )}

          <fieldset className="space-y-2">
            <legend className="font-bold">Choisissez votre horaire</legend>
            <p className="text-xs text-nuit-doux">
              Horaires proposés selon le planning hebdomadaire. La disponibilité
              est vérifiée à la confirmation.
            </p>
            {times.length === 0 ? (
              <p className="text-nuit-doux">
                Pas de créneau de {duration} h ce jour-là. Essayez une durée
                plus courte ou un autre jour.
              </p>
            ) : (
              <div className="grid grid-cols-4 gap-2 sm:grid-cols-5">
                {times.map((t) => (
                  <button
                    key={t}
                    type="button"
                    aria-pressed={time === t}
                    onClick={() => setTime(t)}
                    className={cx(
                      "rounded-full py-3 font-bold tabular-nums transition-colors",
                      time === t
                        ? "bg-nuit text-sur-nuit"
                        : "bg-lilas-pale hover:bg-lilas",
                    )}
                  >
                    {t}
                  </button>
                ))}
              </div>
            )}
          </fieldset>

          <Field
            label={`Ce que ${name} doit savoir`}
            optional
            hint="Vous pourrez toujours changer d'avis pendant la séance."
          >
            {(props) => (
              <>
                <textarea
                  {...props}
                  rows={3}
                  maxLength={1000}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Vos limites, les zones à éviter, une position que vous préférez…"
                  className={inputClass}
                />
                <CharCount value={notes} max={1000} />
              </>
            )}
          </Field>
          <MeetingPointPicker
            value={meetingPoint}
            onChange={setMeetingPoint}
            token={tokenStore.get()}
          />
        </div>
      )}
    </Sheet>
  );
}
