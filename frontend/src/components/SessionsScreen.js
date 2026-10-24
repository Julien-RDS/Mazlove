import React, { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Star } from 'lucide-react';
import { Api } from '../api';
import { navigate } from '../lib/router';
import {
  firstName,
  formatLongDate,
  formatMonthShort,
  formatTimeRange,
  parseLocalDateTime,
} from '../lib/format';
import { Button, CharCount, Field, Notice, Spinner, StatusBadge, cx, inputClass } from './ui';

const UPCOMING = ['PENDING', 'ACCEPTED'];
/** Rafraîchissement d'une séance en attente : le client voit passer « En cours » sans recharger. */
const POLL_INTERVAL_MS = 20000;

// ─── Liste « Mes séances » ──────────────────────────────────────────────────

export function SessionsScreen({ user }) {
  const [sessions, setSessions] = useState([]);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState(null);
  const [tab, setTab] = useState('upcoming');

  const load = async () => {
    setStatus('loading');
    try {
      setSessions(await Api.mySessions());
      setStatus('ready');
    } catch (e) {
      setError(e.message);
      setStatus('error');
    }
  };

  useEffect(() => {
    load();
  }, []);

  const { upcoming, past } = useMemo(() => {
    const byDate = (a, b) => parseLocalDateTime(a.datetime) - parseLocalDateTime(b.datetime);
    return {
      upcoming: sessions.filter((s) => UPCOMING.includes(s.status)).sort(byDate),
      past: sessions.filter((s) => !UPCOMING.includes(s.status)).sort((a, b) => byDate(b, a)),
    };
  }, [sessions]);

  const list = tab === 'upcoming' ? upcoming : past;

  return (
    <div className="mx-auto max-w-3xl space-y-6 pt-2">
      <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
        Mes séances
      </h1>

      {user.role === 'PROVIDER' && !user.providerId && (
        <Notice
          title="Votre fiche n'est pas encore publiée."
          action={
            <Button size="sm" onClick={() => navigate('/profil-praticien')}>
              Publier ma fiche
            </Button>
          }
        >
          Les clients ne peuvent pas encore vous trouver ni réserver.
        </Notice>
      )}

      <div className="inline-flex rounded-full bg-white p-1" role="group" aria-label="Période">
        {[
          ['upcoming', `À venir (${upcoming.length})`],
          ['past', `Historique (${past.length})`],
        ].map(([value, label]) => (
          <button
            key={value}
            type="button"
            aria-pressed={tab === value}
            onClick={() => setTab(value)}
            className={cx(
              'rounded-full px-5 py-2 font-bold transition-colors',
              tab === value ? 'bg-nuit text-white' : 'text-nuit-doux hover:text-nuit'
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {status === 'loading' && <Spinner label="Chargement de vos séances" />}
      {status === 'error' && (
        <Notice tone="error" action={<Button size="sm" variant="secondary" onClick={load}>Réessayer</Button>}>
          {error}
        </Notice>
      )}
      {status === 'ready' && list.length === 0 && (
        <Notice
          title={tab === 'upcoming' ? 'Aucune séance à venir.' : 'Aucune séance passée.'}
          action={
            tab === 'upcoming' && user.role !== 'PROVIDER' ? (
              <Button size="sm" onClick={() => navigate('/')}>
                Trouver un praticien
              </Button>
            ) : null
          }
        />
      )}
      {status === 'ready' && list.length > 0 && (
        <ul className="space-y-3">
          {list.map((session) => (
            <li key={session.id}>
              <SessionRow session={session} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function SessionRow({ session }) {
  const start = parseLocalDateTime(session.datetime);
  const asClient = session.viewerRole === 'CLIENT';
  const counterpart = asClient ? session.providerName : session.clientName;
  const reviewPending = asClient && session.status === 'DONE' && !session.reviewed;
  return (
    <a
      href={`#/seances/${session.id}`}
      className="grid grid-cols-[3.5rem_1fr_auto] items-center gap-4 rounded-3xl bg-white p-4 ring-1 ring-transparent hover:ring-lilas"
    >
      <span className="flex flex-col items-center rounded-2xl bg-brume py-1.5" aria-hidden="true">
        <span className="font-display text-2xl font-semibold leading-none">{start.getDate()}</span>
        <span className="text-[0.85rem] text-nuit-doux">{formatMonthShort(start)}</span>
      </span>
      <span className="min-w-0">
        <span className="block truncate font-bold">{counterpart}</span>
        <span className="block text-[0.95rem] text-nuit-doux">
          <span className="sr-only">{formatLongDate(start)}, </span>
          {formatTimeRange(start, session.duration)}
          {!asClient && ', réservation reçue'}
        </span>
        {reviewPending && (
          <span className="mt-1 inline-flex items-center gap-1 text-[0.95rem] font-bold">
            <Star className="h-4 w-4 fill-abricot text-abricot" aria-hidden="true" />
            Avis à laisser
          </span>
        )}
      </span>
      <span className="flex items-center gap-2">
        <StatusBadge status={session.status} />
        <ChevronRight className="hidden h-5 w-5 text-nuit-doux sm:block" aria-hidden="true" />
      </span>
    </a>
  );
}

// ─── Détail d'une séance ────────────────────────────────────────────────────

export function SessionDetailScreen({ id, notify }) {
  const [session, setSession] = useState(null);
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState(null);

  const load = async ({ silent = false } = {}) => {
    if (!silent) setStatus('loading');
    try {
      setSession(await Api.session(id));
      setStatus('ready');
    } catch (e) {
      if (!silent) {
        setError(e.message);
        setStatus('error');
      }
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  // Tant que la séance n'est pas terminée, on la relit régulièrement (onglet visible uniquement).
  useEffect(() => {
    if (!session || !UPCOMING.includes(session.status)) return undefined;
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') load({ silent: true });
    }, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.status, id]);

  if (status === 'loading') {
    return (
      <>
        <h1 id="page-title" tabIndex={-1} className="sr-only outline-none">
          Séance
        </h1>
        <Spinner label="Chargement de la séance" />
      </>
    );
  }
  if (status === 'error') {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <BackToSessions />
        <h1 id="page-title" tabIndex={-1} className="text-3xl font-semibold outline-none">
          Séance indisponible
        </h1>
        <Notice tone="error" action={<Button size="sm" variant="secondary" onClick={() => load()}>Réessayer</Button>}>
          {error}
        </Notice>
      </div>
    );
  }

  const asClient = session.viewerRole === 'CLIENT';
  const counterpart = asClient ? session.providerName : session.clientName;
  const start = parseLocalDateTime(session.datetime);
  const update = (next, message) => {
    setSession(next);
    if (message) notify(message);
  };

  return (
    <div className="mx-auto max-w-2xl space-y-8 pt-2">
      <BackToSessions />
      <header className="space-y-3">
        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge status={session.status} />
          {!asClient && <span className="text-nuit-doux">Réservation reçue</span>}
        </div>
        <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
          Séance avec {counterpart}
        </h1>
        <p className="text-lg">
          {formatLongDate(start)}, {formatTimeRange(start, session.duration)}{' '}
          <span className="text-nuit-doux">({session.duration} h)</span>
        </p>
        {asClient && (
          <a href={`#/praticien/${session.providerId}`} className="inline-block font-bold underline decoration-lilas decoration-2 underline-offset-4 hover:decoration-nuit">
            Voir la fiche de {firstName(session.providerName)}
          </a>
        )}
      </header>

      {session.status === 'PENDING' && asClient && <ArrivalTicket code={session.arrivalCode} providerName={session.providerName} />}
      {session.status === 'PENDING' && !asClient && (
        <ArrivalCodeForm session={session} onValidated={(next) => update(next, 'Séance démarrée.')} />
      )}

      {session.notes && (
        <section className="space-y-1 rounded-3xl bg-white p-5">
          <h2 className="text-xl font-semibold">{asClient ? 'Vos consignes' : `Consignes de ${firstName(session.clientName)}`}</h2>
          <p className="whitespace-pre-line">{session.notes}</p>
        </section>
      )}

      {session.status === 'ACCEPTED' && <EndSession session={session} onEnded={(next) => update(next, 'Séance terminée.')} />}

      {session.status === 'DONE' && asClient && !session.reviewed && (
        <ReviewForm session={session} onPublished={() => update({ ...session, reviewed: true }, 'Avis publié.')} />
      )}
      {session.status === 'DONE' && asClient && session.reviewed && (
        <Notice tone="success">Merci, votre avis est publié sur la fiche de {firstName(session.providerName)}.</Notice>
      )}
      {session.status === 'DONE' && !asClient && <Notice>Séance terminée. Merci d'avoir pris soin de {firstName(session.clientName)}.</Notice>}
      {session.status === 'CANCELLED' && <Notice>Cette séance a été annulée.</Notice>}

      {session.status === 'PENDING' && (
        <CancelSession session={session} onCancelled={(next) => update(next, 'Séance annulée.')} />
      )}
    </div>
  );
}

function BackToSessions() {
  return (
    <a href="#/seances" className="inline-flex items-center gap-1 font-bold text-nuit-doux hover:text-nuit">
      <ChevronLeft className="h-4 w-4" aria-hidden="true" />
      Mes séances
    </a>
  );
}

/**
 * Le « billet » du client : l'élément le plus important de l'application.
 * Il matérialise le rendez-vous et la règle de sécurité (le praticien saisit ce code sur place).
 */
function ArrivalTicket({ code, providerName }) {
  return (
    <section aria-labelledby="code-arrivee" className="relative rounded-3xl bg-abricot px-6 py-7 motion-safe:animate-ticket-in">
      <h2 id="code-arrivee" className="font-sans text-lg font-bold">
        Votre code d'arrivée
      </h2>
      <p className="mt-2 font-display text-5xl font-semibold tracking-[0.18em] sm:text-6xl">
        <span className="sr-only">{code.split('').join(' ')}</span>
        <span aria-hidden="true">{code}</span>
      </p>
      {/* Pointillés et encoches façon billet détachable (purement décoratif). */}
      <div className="relative my-6 border-t-2 border-dashed border-nuit/25" aria-hidden="true">
        <span className="absolute -left-9 -top-[13px] h-6 w-6 rounded-full bg-brume" />
        <span className="absolute -right-9 -top-[13px] h-6 w-6 rounded-full bg-brume" />
      </div>
      <p className="max-w-md">
        Montrez ce code à {firstName(providerName)} quand vous vous retrouvez. C'est {firstName(providerName)} qui le
        saisit pour démarrer la séance : gardez-le pour vous d'ici là.
      </p>
    </section>
  );
}

function ArrivalCodeForm({ session, onValidated }) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const client = firstName(session.clientName);

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      onValidated(await Api.validateArrival(session.id, code));
    } catch (e) {
      setError(e.fields?.code || e.message);
      setLoading(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-4 rounded-3xl bg-white p-5" noValidate>
      <h2 className="text-2xl font-semibold">Démarrer la séance</h2>
      <p className="text-nuit-doux">Demandez son code d'arrivée à {client} et saisissez-le ici.</p>
      <Field label={`Code donné par ${client}`} error={error}>
        {(props) => (
          <input
            {...props}
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6))}
            className={cx(inputClass, 'font-display text-2xl tracking-[0.3em]')}
          />
        )}
      </Field>
      <Button type="submit" size="lg" loading={loading} disabled={code.length !== 6}>
        Démarrer la séance
      </Button>
    </form>
  );
}

function EndSession({ session, onEnded }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const end = async () => {
    setLoading(true);
    setError(null);
    try {
      onEnded(await Api.confirmEnd(session.id));
    } catch (e) {
      setError(e.message);
      setLoading(false);
    }
  };
  return (
    <section className="space-y-4 rounded-3xl bg-sauge-pale p-5">
      <h2 className="text-2xl font-semibold">Séance en cours</h2>
      <p>Chacun peut faire une pause ou arrêter à tout moment, sans avoir à se justifier.</p>
      {error && <Notice tone="error">{error}</Notice>}
      <Button size="lg" loading={loading} onClick={end}>
        Terminer la séance
      </Button>
    </section>
  );
}

/** Annulation en deux temps : un clic par erreur ne supprime rien. */
function CancelSession({ session, onCancelled }) {
  const [confirming, setConfirming] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const cancel = async () => {
    setLoading(true);
    setError(null);
    try {
      onCancelled(await Api.cancelSession(session.id));
    } catch (e) {
      setError(e.message);
      setLoading(false);
    }
  };

  if (!confirming) {
    return (
      <Button variant="quiet" onClick={() => setConfirming(true)}>
        Annuler la réservation
      </Button>
    );
  }
  return (
    <div className="space-y-3 rounded-3xl bg-white p-5" role="group" aria-label="Confirmer l'annulation">
      <p className="font-bold">Annuler cette séance ?</p>
      <p className="text-nuit-doux">L'autre personne verra l'annulation dans ses séances.</p>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="flex flex-wrap gap-3">
        <Button variant="danger" loading={loading} onClick={cancel}>
          Oui, annuler la séance
        </Button>
        <Button variant="secondary" onClick={() => setConfirming(false)}>
          Garder la séance
        </Button>
      </div>
    </div>
  );
}

const RATING_LABELS = ['Décevante', 'Moyenne', 'Bien', 'Très bien', 'Excellente'];

function ReviewForm({ session, onPublished }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const provider = firstName(session.providerName);

  const submit = async (event) => {
    event.preventDefault();
    if (!rating) {
      setError('Choisissez une note de 1 à 5 étoiles.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await Api.addReview(session.providerId, { sessionId: session.id, rating, comment: comment.trim() || null });
      onPublished();
    } catch (e) {
      setError(e.fields ? Object.values(e.fields)[0] : e.message);
      setLoading(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-5 rounded-3xl bg-white p-5" noValidate>
      <h2 className="text-2xl font-semibold">Comment s'est passée la séance ?</h2>
      {error && <Notice tone="error">{error}</Notice>}
      <fieldset className="space-y-2">
        <legend className="font-bold">Votre note</legend>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex">
            {[1, 2, 3, 4, 5].map((n) => (
              <label key={n} className="cursor-pointer p-1">
                <input
                  type="radio"
                  name="rating"
                  value={n}
                  checked={rating === n}
                  onChange={() => setRating(n)}
                  className="peer sr-only"
                />
                <span className="sr-only">
                  {n} sur 5, {RATING_LABELS[n - 1]}
                </span>
                <Star
                  aria-hidden="true"
                  className={cx(
                    'h-9 w-9 rounded-sm transition-colors peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-nuit',
                    n <= rating ? 'fill-abricot text-abricot' : 'fill-transparent text-lilas hover:text-abricot'
                  )}
                />
              </label>
            ))}
          </div>
          <span className="font-bold" aria-hidden="true">
            {rating ? RATING_LABELS[rating - 1] : ''}
          </span>
        </div>
      </fieldset>
      <Field
        label="Votre commentaire"
        optional
        hint={`Publié avec votre prénom et l'initiale de votre nom. Il aide les autres à choisir, et ${provider} à progresser.`}
      >
        {(props) => (
          <>
            <textarea {...props} rows={4} maxLength={1000} value={comment} onChange={(e) => setComment(e.target.value)} className={inputClass} />
            <CharCount value={comment} max={1000} />
          </>
        )}
      </Field>
      <Button type="submit" size="lg" loading={loading}>
        Publier l'avis
      </Button>
    </form>
  );
}
