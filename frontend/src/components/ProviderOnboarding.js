import React, { useEffect, useState } from 'react';
import { LocateFixed } from 'lucide-react';
import { Api } from '../api';
import { navigate } from '../lib/router';
import { DAY_LABELS, DAYS, capitalize } from '../lib/format';
import { Button, CharCount, Field, Notice, cx, inputClass } from './ui';

const CATEGORY_SUGGESTIONS = [
  'Câlins assis',
  'Câlins assis et allongés',
  'Cuillère',
  'Première séance',
  'Accompagnement émotionnel',
];

const initialDays = () =>
  Object.fromEntries(DAYS.map((day) => [day, { on: false, start: '10:00', end: '18:00' }]));

/**
 * Création de la fiche praticien (comptes PROVIDER sans fiche).
 * Les champs suivent l'ordre dans lequel un client lit une fiche : type, présentation,
 * zone, puis disponibilités.
 */
export default function ProviderOnboarding({ user, position, onCreated }) {
  const [category, setCategory] = useState('');
  const [bio, setBio] = useState('');
  const [location, setLocation] = useState({ lat: position.lat, lng: position.lng, label: position.label });
  const [days, setDays] = useState(initialDays);
  const [imagesText, setImagesText] = useState('');
  const [locating, setLocating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [fields, setFields] = useState({});

  // Fiche déjà publiée : on l'affiche plutôt que de proposer un formulaire qui serait refusé.
  useEffect(() => {
    if (user.providerId) navigate(`/praticien/${user.providerId}`, { replace: true });
  }, [user.providerId]);

  if (user.role !== 'PROVIDER') {
    return (
      <div className="mx-auto max-w-2xl space-y-4 pt-2">
        <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
          Fiche praticien
        </h1>
        <Notice>
          Votre compte est un compte client. Pour proposer des séances, créez un compte praticien avec une autre adresse
          email.
        </Notice>
      </div>
    );
  }

  const setDay = (day, patch) => setDays((current) => ({ ...current, [day]: { ...current[day], ...patch } }));

  const locate = () => {
    if (!navigator.geolocation) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (result) => {
        setLocating(false);
        setLocation({ lat: result.coords.latitude, lng: result.coords.longitude, label: 'votre position actuelle' });
      },
      () => {
        setLocating(false);
        setFields((f) => ({ ...f, location: 'Position indisponible : autorisez la localisation dans votre navigateur.' }));
      },
      { timeout: 10000 }
    );
  };

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setFields({});

    // Vérifications immédiates, pour ne pas attendre l'aller-retour serveur sur les cas évidents.
    const availability = DAYS.filter((day) => days[day].on).map((day) => ({
      dayOfWeek: day,
      startTime: days[day].start,
      endTime: days[day].end,
    }));
    const local = {};
    if (!category.trim()) local.category = 'Choisissez un type de séance.';
    if (!bio.trim()) local.bio = 'Présentez-vous en quelques lignes.';
    if (availability.length === 0) local.availability = 'Cochez au moins un jour.';
    const badDay = availability.find((slot) => slot.endTime <= slot.startTime);
    if (badDay) local.availability = `Le ${DAY_LABELS[badDay.dayOfWeek]}, l'heure de fin doit suivre l'heure de début.`;
    if (Object.keys(local).length) {
      setFields(local);
      setError('Certains champs sont à corriger.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await Api.createProvider({
        category: category.trim(),
        bio: bio.trim(),
        location: { latitude: location.lat, longitude: location.lng },
        availability,
        images: imagesText.split(/\s+/).filter(Boolean),
      });
      await onCreated(created);
    } catch (e) {
      const apiFields = e.fields || {};
      // Les erreurs de listes arrivent sous la forme "images[0]" ou "availability[2].startTime".
      setFields({
        category: apiFields.category,
        bio: apiFields.bio,
        location: apiFields['location.latitude'] || apiFields['location.longitude'] || apiFields.location,
        availability: Object.entries(apiFields).find(([key]) => key.startsWith('availability'))?.[1],
        images: Object.entries(apiFields).find(([key]) => key.startsWith('images'))?.[1],
      });
      setError(e.fields ? 'Certains champs sont à corriger.' : e.message);
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6 pt-2">
      <div className="space-y-2">
        <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
          Publier ma fiche
        </h1>
        <p className="text-lg text-nuit-doux">
          C'est ce que les clients verront avant de réserver. Vous pourrez la compléter plus tard.
        </p>
      </div>

      <form onSubmit={submit} className="space-y-6 rounded-3xl bg-carte p-5 sm:p-6" noValidate>
        {error && <Notice tone="error">{error}</Notice>}

        <Field label="Type de séance" hint="Choisissez une proposition ou écrivez la vôtre." error={fields.category}>
          {(props) => (
            <>
              <input
                {...props}
                list="categories"
                maxLength={100}
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className={inputClass}
              />
              <datalist id="categories">
                {CATEGORY_SUGGESTIONS.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </>
          )}
        </Field>

        <Field
          label="Présentation"
          hint="Votre approche, comment vous posez le cadre, ce qu'un client peut attendre d'une première séance."
          error={fields.bio}
        >
          {(props) => (
            <>
              <textarea {...props} rows={5} maxLength={500} value={bio} onChange={(e) => setBio(e.target.value)} className={inputClass} />
              <CharCount value={bio} max={500} />
            </>
          )}
        </Field>

        <fieldset className="space-y-2">
          <legend className="font-bold">Zone d'intervention</legend>
          <p>
            Autour de <strong>{location.label}</strong>
          </p>
          <p className="text-[0.95rem] text-nuit-doux">
            Votre fiche affiche une position arrondie à environ 1 km : votre adresse exacte n'est jamais visible.
          </p>
          <Button variant="secondary" size="sm" onClick={locate} loading={locating}>
            {!locating && <LocateFixed className="h-4 w-4" aria-hidden="true" />}
            Utiliser ma position actuelle
          </Button>
          {fields.location && <p className="text-[0.95rem] text-alerte">{fields.location}</p>}
        </fieldset>

        <fieldset className="space-y-3">
          <legend className="font-bold">Disponibilités chaque semaine</legend>
          {fields.availability && <p className="text-[0.95rem] text-alerte">{fields.availability}</p>}
          <ul className="space-y-2">
            {DAYS.map((day) => {
              const value = days[day];
              const label = capitalize(DAY_LABELS[day]);
              return (
                <li
                  key={day}
                  className={cx(
                    'grid grid-cols-[7.5rem_1fr] items-center gap-3 rounded-2xl px-3 py-2',
                    value.on ? 'bg-lilas-pale' : 'bg-brume'
                  )}
                >
                  <label className="flex items-center gap-2 font-bold">
                    <input type="checkbox" checked={value.on} onChange={(e) => setDay(day, { on: e.target.checked })} className="h-5 w-5 accent-nuit" />
                    {label}
                  </label>
                  {value.on ? (
                    <div className="flex flex-wrap items-center gap-2">
                      <input
                        type="time"
                        step={1800}
                        aria-label={`${label}, début`}
                        value={value.start}
                        onChange={(e) => setDay(day, { start: e.target.value })}
                        className={cx(inputClass, 'w-auto py-1.5')}
                      />
                      <span aria-hidden="true">à</span>
                      <input
                        type="time"
                        step={1800}
                        aria-label={`${label}, fin`}
                        value={value.end}
                        onChange={(e) => setDay(day, { end: e.target.value })}
                        className={cx(inputClass, 'w-auto py-1.5')}
                      />
                    </div>
                  ) : (
                    <span className="text-nuit-doux">Ne reçoit pas</span>
                  )}
                </li>
              );
            })}
          </ul>
        </fieldset>

        <Field label="Photos" optional hint="Une adresse https par ligne. Sans photo, vos initiales s'affichent." error={fields.images}>
          {(props) => (
            <textarea
              {...props}
              rows={3}
              value={imagesText}
              onChange={(e) => setImagesText(e.target.value)}
              placeholder="https://…"
              className={inputClass}
            />
          )}
        </Field>

        <Button type="submit" size="lg" className="w-full" loading={submitting}>
          Publier ma fiche
        </Button>
      </form>
    </div>
  );
}
