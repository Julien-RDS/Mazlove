import React, { useEffect, useMemo, useState } from 'react';
import { ChevronLeft } from 'lucide-react';
import { Api } from '../api';
import { loginPath, navigate } from '../lib/router';
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
} from '../lib/format';
import { Avatar, Button, CharCount, Field, Notice, Sheet, Spinner, Stars, VerifiedBadge, cx, inputClass } from './ui';

/** Fiche détaillée d'un praticien, avec réservation dans un panneau. */
export default function ProviderScreen({ id, user, position, notify }) {
  const [provider, setProvider] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [services, setServices] = useState([]);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState(null);
  const [booking, setBooking] = useState(false);

  const load = async () => {
    setStatus('loading');
    try {
      const [p, r, s] = await Promise.all([Api.provider(id), Api.providerReviews(id), Api.providerServices(id)]);
      setProvider(p);
      setReviews(r);
      setServices(s);
      setStatus('ready');
    } catch (e) {
      setError(e);
      setStatus('error');
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (status === 'loading') {
    return (
      <>
        <h1 id="page-title" tabIndex={-1} className="sr-only outline-none">
          Fiche du praticien
        </h1>
        <Spinner label="Chargement de la fiche" />
      </>
    );
  }

  if (status === 'error') {
    return (
      <div className="space-y-4">
        <BackLink />
        <h1 id="page-title" tabIndex={-1} className="text-3xl font-semibold outline-none">
          Fiche indisponible
        </h1>
        <Notice
          tone="error"
          action={
            error.status === 404 ? (
              <Button size="sm" variant="secondary" onClick={() => navigate('/?etape=recherche')}>
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
  const startBooking = () => {
    if (!user) navigate(loginPath(`/praticien/${provider.id}`));
    else setBooking(true);
  };

  return (
    <div className="space-y-10 pb-8">
      <BackLink />

      <section className="grid gap-6 sm:grid-cols-[minmax(0,15rem)_1fr] sm:items-start">
        <Gallery name={name} images={provider.images} />
        <div className="space-y-3">
          <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none sm:text-5xl">
            {name}
          </h1>
          <p className="text-lg text-nuit-doux">
            {provider.category}
            {distance && `, ${distance} de ${position.label}`}
          </p>
          <div className="flex flex-wrap items-center gap-3">
            {provider.verified && <VerifiedBadge />}
            {provider.reviewCount > 0 ? (
              <span className="inline-flex items-center gap-2">
                <Stars value={provider.rating} />
                <span className="font-bold" aria-hidden="true">
                  {formatRating(provider.rating)}
                </span>
                <span className="text-nuit-doux">({provider.reviewCount} avis)</span>
              </span>
            ) : (
              <span className="text-nuit-doux">Nouveau sur Mazlove</span>
            )}
          </div>
          <p className="max-w-prose whitespace-pre-line pt-2">{provider.bio}</p>
        </div>
      </section>

      <div className="grid gap-10 lg:grid-cols-[1fr_22rem]">
        <div className="space-y-10">
          {services.length > 0 && (
            <section aria-labelledby="prestations" className="space-y-3">
              <h2 id="prestations" className="text-2xl font-semibold">Prestations</h2>
              <ul className="space-y-2">
                {services.map((service) => (
                  <li key={service.id} className="rounded-2xl bg-white p-4">
                    <p className="font-bold">{service.name} · {service.duration} h · {service.price} €</p>
                    <p>{service.description}</p>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <section aria-labelledby="deroule" className="space-y-4">
            <h2 id="deroule" className="text-2xl font-semibold">
              Comment se passe une séance
            </h2>
            <ol className="space-y-4">
              {[
                ['Vous réservez', 'Choisissez un créneau et indiquez vos limites : zones à éviter, positions, envie de parler ou non.'],
                ['Vous donnez votre code', `À l'arrivée, vous montrez votre code à ${firstName(name)}. La séance démarre quand il est saisi.`],
                ['Vous restez libre', 'Chacun peut faire une pause ou arrêter, sans se justifier. Après la séance, vous laissez un avis.'],
              ].map(([title, text], index) => (
                <li key={title} className="grid grid-cols-[2.25rem_1fr] gap-3">
                  <span
                    aria-hidden="true"
                    className="flex h-9 w-9 items-center justify-center rounded-full bg-white font-display text-lg font-semibold"
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
              Avis {reviews.length > 0 && <span className="text-nuit-doux">({reviews.length})</span>}
            </h2>
            {reviews.length === 0 ? (
              <p className="text-nuit-doux">
                Pas encore d'avis. Sur Mazlove, seuls les clients d'une séance terminée peuvent en laisser un.
              </p>
            ) : (
              <ul className="space-y-3">
                {reviews.map((review) => (
                  <li key={review.id} className="space-y-1.5 rounded-2xl bg-white p-4">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="font-bold">{review.authorName}</p>
                      <Stars value={review.rating} />
                    </div>
                    {review.comment && <p>{review.comment}</p>}
                    <p className="text-[0.9rem] text-nuit-doux">{capitalize(formatMonthYear(parseLocalDateTime(review.createdAt)))}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <aside aria-labelledby="dispos" className="space-y-4 lg:sticky lg:top-24 lg:self-start">
          <div className="space-y-4 rounded-3xl bg-white p-5">
            <h2 id="dispos" className="text-2xl font-semibold">
              Disponibilités
            </h2>
            <AvailabilityList availability={provider.availability} />
            {isOwnProfile ? (
              <Notice>C'est votre fiche : les clients la voient exactement ainsi.</Notice>
            ) : (
              <Button size="lg" className="w-full" onClick={startBooking} disabled={provider.availability.length === 0}>
                {user ? 'Réserver une séance' : 'Se connecter pour réserver'}
              </Button>
            )}
          </div>
        </aside>
      </div>

      {booking && (
        <BookingSheet
          provider={provider}
          services={services}
          onClose={() => setBooking(false)}
          onBooked={(session) => {
            setBooking(false);
            notify('Séance réservée.');
            navigate(`/seances/${session.id}`);
          }}
        />
      )}
    </div>
  );
}

function BackLink() {
  return (
    <a href="#/?etape=recherche" className="inline-flex items-center gap-1 font-bold text-nuit-doux hover:text-nuit">
      <ChevronLeft className="h-4 w-4" aria-hidden="true" />
      Retour à la recherche
    </a>
  );
}

function Gallery({ name, images = [] }) {
  if (images.length <= 1) {
    return <Avatar name={name} src={images[0]} className="aspect-[4/5] w-full max-w-[15rem] rounded-3xl text-5xl" />;
  }
  return (
    <ul className="flex snap-x snap-mandatory gap-3 overflow-x-auto pb-2" aria-label={`Photos de ${name}`}>
      {images.map((src, index) => (
        <li key={src} className="w-[70%] shrink-0 snap-start sm:w-full">
          <Avatar name={`${name} (${index + 1}/${images.length})`} src={src} className="aspect-[4/5] w-full rounded-3xl" />
        </li>
      ))}
    </ul>
  );
}

function AvailabilityList({ availability }) {
  if (availability.length === 0) {
    return <p className="text-nuit-doux">Aucun créneau publié pour le moment.</p>;
  }
  return (
    <dl className="space-y-1.5">
      {availability.map((slot) => (
        <div key={`${slot.dayOfWeek}-${slot.startTime}`} className="flex justify-between gap-4">
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
  const earliest = isSameDay(day, now) ? now.getHours() * 60 + now.getMinutes() + MIN_LEAD_MINUTES : 0;
  const starts = new Set();
  availability
    .filter((slot) => slot.dayOfWeek === weekday)
    .forEach((slot) => {
      const end = toMinutes(slot.endTime);
      for (let t = toMinutes(slot.startTime); t + durationHours * 60 <= end; t += SLOT_STEP_MINUTES) {
        if (t >= earliest) starts.add(t);
      }
    });
  return [...starts].sort((a, b) => a - b).map(fromMinutes);
}

function BookingSheet({ provider, services, onClose, onBooked }) {
  const name = firstName(provider.userName);
  const [now] = useState(() => new Date());

  // Les 14 prochains jours où le praticien reçoit (cherchés sur 4 semaines).
  const days = useMemo(() => {
    const result = [];
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    for (let i = 0; i < 28 && result.length < 14; i += 1) {
      const day = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      if (startTimesFor(provider.availability, day, 1, now).length > 0) result.push(day);
    }
    return result;
  }, [provider.availability, now]);

  const [day, setDay] = useState(days[0] || null);
  const [duration, setDuration] = useState(1);
  const [serviceId, setServiceId] = useState('');
  const [time, setTime] = useState(null);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const times = useMemo(
    () => (day ? startTimesFor(provider.availability, day, duration, now) : []),
    [provider.availability, day, duration, now]
  );

  // Un horaire qui ne tient plus (jour ou durée changés) est désélectionné plutôt que gardé invalide.
  useEffect(() => {
    if (time && !times.includes(time)) setTime(null);
  }, [times, time]);

  const endTime = time ? fromMinutes(toMinutes(time) + duration * 60) : null;

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
        <div className="space-y-3">
          {error && <Notice tone="error">{error}</Notice>}
          <p className="min-h-[1.6em] font-bold" aria-live="polite">
            {day && time ? `${formatLongDate(day)}, ${time} – ${endTime}` : 'Choisissez un jour et une heure'}
          </p>
          <Button size="lg" className="w-full" disabled={!day || !time} loading={submitting} onClick={submit}>
            Confirmer la réservation
          </Button>
        </div>
      }
    >
      {days.length === 0 ? (
        <Notice>{name} n'a aucun créneau libre dans les quatre prochaines semaines.</Notice>
      ) : (
        <div className="space-y-6 pt-2">
          {services.length > 0 && (
            <Field label="Prestation">
              {(props) => (
                <select {...props} className={inputClass} value={serviceId} onChange={(event) => {
                  const id = event.target.value;
                  setServiceId(id);
                  const selected = services.find((item) => String(item.id) === id);
                  if (selected) setDuration(selected.duration);
                }}>
                  <option value="">Séance sans prestation choisie</option>
                  {services.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.duration} h · {item.price} €</option>)}
                </select>
              )}
            </Field>
          )}
          <fieldset className="space-y-2">
            <legend className="font-bold">Jour</legend>
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
                      'flex min-w-[4.5rem] shrink-0 flex-col items-center rounded-2xl px-3 py-2 transition-colors',
                      selected ? 'bg-nuit text-white' : 'bg-brume hover:bg-lilas-pale'
                    )}
                  >
                    <span className="text-[0.85rem]">{formatWeekday(d)}</span>
                    <span className="font-bold">{formatDayMonth(d)}</span>
                  </button>
                );
              })}
            </div>
          </fieldset>

          {!serviceId && <fieldset className="space-y-2">
            <legend className="font-bold">Durée</legend>
            <div className="inline-flex rounded-full bg-brume p-1">
              {DURATIONS.map((h) => (
                <button
                  key={h}
                  type="button"
                  aria-pressed={duration === h}
                  onClick={() => setDuration(h)}
                  className={cx(
                    'rounded-full px-5 py-1.5 font-bold transition-colors',
                    duration === h ? 'bg-white text-nuit shadow-sm' : 'text-nuit-doux hover:text-nuit'
                  )}
                >
                  {h} h
                </button>
              ))}
            </div>
          </fieldset>}

          <fieldset className="space-y-2">
            <legend className="font-bold">Heure de début</legend>
            {times.length === 0 ? (
              <p className="text-nuit-doux">
                Pas de créneau de {duration} h ce jour-là. Essayez une durée plus courte ou un autre jour.
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
                      'rounded-lg py-2 font-bold tabular-nums transition-colors',
                      time === t ? 'bg-nuit text-white' : 'bg-brume hover:bg-lilas-pale'
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
        </div>
      )}
    </Sheet>
  );
}
