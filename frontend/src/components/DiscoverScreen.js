import React, { useEffect, useRef, useState } from 'react';
import { ArrowRight, ChevronLeft, ChevronRight, Heart, Loader2, Search, Star } from 'lucide-react';
import { Api } from '../api';
import { navigate } from '../lib/router';
import { formatRating } from '../lib/format';
import { Avatar, Button, Notice, VerifiedBadge, cx, inputClass } from './ui';

/*
 * Parcours de sélection des praticiens, en 4 étapes : taille → traits → recherche → résultat.
 *
 * IMPORTANT : les critères de sélection (taille, visage, poitrine, hanches, cuisses, barre de
 * recherche et filtres « Calins / Les mieux notés / Durée / Tr... ») sont repris À L'IDENTIQUE de
 * la version d'origine, libellés compris. Ils seront redéfinis plus tard par l'équipe produit :
 * ne pas les modifier ni leur ajouter de logique ici.
 * Comme dans l'original, ils sont conservés dans l'état du composant mais ne filtrent pas la liste :
 * l'API ne connaît pas encore ces champs. La liste vient de l'API (50 km autour de Paris centre,
 * du plus proche au plus éloigné) à la place des profils fictifs.
 *
 * L'étape courante est portée par l'adresse (#/?etape=recherche) : le bouton « précédent » du
 * navigateur fonctionne et l'on revient au bon endroit après avoir consulté une fiche.
 */

const STEPS = {
  home: undefined, // #/
  customize: 'traits',
  search: 'recherche',
  results: 'resultats',
};

const goTo = (step) => navigate(step ? `/?etape=${step}` : '/');

export default function DiscoverScreen({ query, position }) {
  const step = query.etape;

  // ─── Critères d'origine (valeurs et forme inchangées) ───────────────────
  const [selectedHeight] = useState('1m72');
  const [customizations] = useState({
    face: 'selected',
    chest: 'medium',
    hips: 'medium',
    thighs: 'medium',
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  // ─── Données réelles (remplacent les profils fictifs) ───────────────────
  const [providers, setProviders] = useState([]);
  const [status, setStatus] = useState('idle'); // idle | loading | ready | error
  const [error, setError] = useState(null);
  const requestId = useRef(0);

  const loadProviders = async () => {
    const current = ++requestId.current;
    setStatus('loading');
    setError(null);
    try {
      // Équivaut à fetch('/api/providers?lat=48.87&lng=2.35'), via le client commun (erreurs, JWT).
      const results = await Api.providersNearby({ lat: position.lat, lng: position.lng });
      if (current !== requestId.current) return null;
      setProviders(results);
      setStatus('ready');
      return results;
    } catch (e) {
      if (current !== requestId.current) return null;
      setError(e.message);
      setStatus('error');
      return null;
    }
  };

  // La liste est chargée dès qu'on arrive sur la recherche ou le résultat (y compris par un lien direct).
  useEffect(() => {
    if ((step === STEPS.search || step === STEPS.results) && status === 'idle') loadProviders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step]);

  // Remplace l'ancien délai simulé de 2 s : on recharge réellement la liste puis on affiche le résultat.
  const handleSearch = async () => {
    setIsSearching(true);
    await loadProviders();
    setIsSearching(false);
    goTo(STEPS.results);
  };

  switch (step) {
    case STEPS.customize:
      return <ScreenTwo />;
    case STEPS.search:
      return (
        <ScreenThree
          searchQuery={searchQuery}
          onSearchQueryChange={setSearchQuery}
          providers={providers}
          status={status}
          error={error}
          onRetry={loadProviders}
          isSearching={isSearching}
          onSearch={handleSearch}
        />
      );
    case STEPS.results:
      return <ScreenFour providers={providers} status={status} error={error} onRetry={loadProviders} />;
    default:
      return <ScreenOne selectedHeight={selectedHeight} customizations={customizations} />;
  }
}

// ─── Éléments communs aux étapes ────────────────────────────────────────────

/** Colonne étroite façon application mobile, comme la maquette d'origine. */
function StepFrame({ children }) {
  return <div className="mx-auto flex w-full max-w-md flex-col gap-6 pt-2">{children}</div>;
}

/** En-tête d'étape : retour éventuel + rappel « Traits physiques » de l'original. */
function StepHeader({ onBack }) {
  return (
    <div className="flex items-center justify-between">
      {onBack ? (
        <button
          type="button"
          onClick={onBack}
          className="-ml-2 inline-flex h-10 w-10 items-center justify-center rounded-full text-nuit-doux hover:bg-white hover:text-nuit"
          aria-label="Étape précédente"
        >
          <ChevronLeft className="h-5 w-5" aria-hidden="true" />
        </button>
      ) : (
        <span />
      )}
      <span className="inline-flex items-center gap-1 text-[0.95rem] text-nuit-doux">
        Traits physiques
        <ChevronRight className="h-4 w-4" aria-hidden="true" />
      </span>
    </div>
  );
}

/** Silhouette neutre (placeholder de l'original, en attendant une illustration). */
function Silhouette({ children }) {
  return (
    <div className="relative mx-auto h-80 w-48" aria-hidden="true">
      <div className="absolute left-1/2 top-0 h-16 w-16 -translate-x-1/2 rounded-full bg-lilas" />
      <div className="absolute inset-x-6 bottom-0 top-[4.5rem] rounded-[3rem] bg-lilas" />
      {children}
    </div>
  );
}

// ─── Étape 1 : taille ───────────────────────────────────────────────────────

function ScreenOne({ selectedHeight }) {
  return (
    <StepFrame>
      <StepHeader />
      <div className="space-y-3">
        <h1 id="page-title" tabIndex={-1} className="text-[2.1rem] font-semibold outline-none">
          Des câlins professionnels, près de chez vous
        </h1>
        <p className="text-nuit-doux">
          Des séances non sexuelles, sur rendez-vous. Vous posez vos limites avant de commencer et pouvez arrêter à tout
          moment.
        </p>
      </div>
      <Silhouette />
      <div className="text-center">
        <p className="text-nuit-doux">Définissez la taille</p>
        <p className="font-display text-4xl font-semibold">{selectedHeight}</p>
      </div>
      <Button size="lg" className="mx-auto w-full max-w-xs" onClick={() => goTo(STEPS.customize)}>
        Continuer
      </Button>
    </StepFrame>
  );
}

// ─── Étape 2 : traits ───────────────────────────────────────────────────────

/** Points de personnalisation de l'original, positions et libellés inchangés. */
const CUSTOMIZATION_POINTS = [
  { label: 'Face', top: 'top-12' },
  { label: 'Poitrine', top: 'top-32' },
  { label: 'Hanches', top: 'top-48' },
  { label: 'Cuisse', top: 'top-64' },
];

function ScreenTwo() {
  return (
    <StepFrame>
      <StepHeader onBack={() => goTo(STEPS.home)} />
      <h1 id="page-title" tabIndex={-1} className="sr-only outline-none">
        Traits physiques
      </h1>
      <div className="relative mx-auto">
        <Silhouette />
        <ul className="absolute inset-0">
          {CUSTOMIZATION_POINTS.map((point) => (
            <li key={point.label} className={cx('absolute left-2 flex items-center gap-2', point.top)}>
              <span className="h-3 w-3 rounded-full bg-nuit" aria-hidden="true" />
              <span className="rounded-md bg-nuit px-2 py-1 text-[0.8rem] font-bold text-white">{point.label}</span>
            </li>
          ))}
        </ul>
      </div>
      <Button size="lg" className="w-full" onClick={() => goTo(STEPS.search)}>
        Rechercher
      </Button>
    </StepFrame>
  );
}

// ─── Étape 3 : recherche ────────────────────────────────────────────────────

/** Filtres de l'original : affichés à l'identique, sans effet (comme dans la version d'origine). */
const ORIGINAL_FILTERS = [
  { label: 'Calins', icon: Heart },
  { label: 'Les mieux notés' },
  { label: 'Durée', trailing: ChevronRight },
  { label: 'Tr...' },
];

function ScreenThree({ searchQuery, onSearchQueryChange, providers, status, error, onRetry, isSearching, onSearch }) {
  return (
    <StepFrame>
      <StepHeader onBack={() => goTo(STEPS.customize)} />
      <h1 id="page-title" tabIndex={-1} className="sr-only outline-none">
        Recherche de praticiens
      </h1>

      <label className="relative block">
        <span className="sr-only">Rechercher votre désir</span>
        <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-nuit-doux" aria-hidden="true" />
        <input
          type="search"
          placeholder="Rechercher votre désir"
          value={searchQuery}
          onChange={(e) => onSearchQueryChange(e.target.value)}
          className={cx(inputClass, 'rounded-full pl-10')}
        />
      </label>

      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
        {ORIGINAL_FILTERS.map(({ label, icon: Icon, trailing: Trailing }) => (
          <button
            key={label}
            type="button"
            className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-white px-4 py-1.5 text-[0.95rem] font-bold text-nuit-doux"
          >
            {Icon && <Icon className="h-4 w-4" aria-hidden="true" />}
            {label}
            {Trailing && <Trailing className="h-3.5 w-3.5" aria-hidden="true" />}
          </button>
        ))}
      </div>

      <p className="text-[0.9rem] text-nuit-doux">Découvrez comment les profils sont sélectionnés. En savoir plus</p>

      <div className="flex items-center justify-between rounded-2xl bg-white px-4 py-3">
        <div>
          <p className="font-bold">À découvrir sur Mazlove</p>
          <p className="text-[0.85rem] text-nuit-doux">Sponsorisé</p>
        </div>
        <ArrowRight className="h-4 w-4 text-nuit-doux" aria-hidden="true" />
      </div>

      <section aria-labelledby="profils" className="space-y-3">
        <h2 id="profils" className="sr-only">
          Profils
        </h2>
        {(status === 'loading' || status === 'idle') && <ProfilesSkeleton />}
        {status === 'error' && (
          <Notice
            tone="error"
            title="Les profils n'ont pas pu être chargés."
            action={
              <Button size="sm" variant="secondary" onClick={onRetry}>
                Réessayer
              </Button>
            }
          >
            {error}
          </Notice>
        )}
        {status === 'ready' && providers.length === 0 && (
          <Notice title="Aucun praticien n'est disponible dans la zone pour le moment." />
        )}
        {status === 'ready' && providers.length > 0 && (
          <ul className="grid grid-cols-2 gap-3">
            {providers.map((provider) => (
              <li key={provider.id}>
                <ProfileCard provider={provider} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <Button size="lg" className="sticky bottom-4 w-full shadow-lg" onClick={onSearch} loading={isSearching}>
        {isSearching ? 'Recherche en cours...' : 'Trouver mon calin idéal'}
      </Button>
    </StepFrame>
  );
}

/** Carte de la grille : photo (ou initiales), nom, note, puis le type de séance en étiquette. */
function ProfileCard({ provider }) {
  return (
    <a href={`#/praticien/${provider.id}`} className="group block overflow-hidden rounded-2xl bg-white ring-1 ring-transparent hover:ring-lilas">
      <div className="relative">
        <Avatar name={provider.userName} src={provider.images?.[0]} className="aspect-[3/4] w-full text-3xl" />
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-nuit/80 to-transparent px-3 pb-2.5 pt-10 text-white">
          <p className="truncate font-display text-lg font-semibold">{provider.userName}</p>
          {provider.reviewCount > 0 ? (
            <p className="flex items-center gap-1 text-[0.85rem]">
              <Star className="h-3.5 w-3.5 fill-abricot text-abricot" aria-hidden="true" />
              <span className="sr-only">Note</span>
              {formatRating(provider.rating)}
              <span className="text-white/80">({provider.reviewCount})</span>
            </p>
          ) : (
            <p className="text-[0.85rem] text-white/90">Nouveau</p>
          )}
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-1.5 p-2.5">
        <span className="rounded-md bg-brume px-2 py-0.5 text-[0.8rem]">{provider.category}</span>
        {provider.verified && <VerifiedBadge compact />}
      </div>
    </a>
  );
}

function ProfilesSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-3" role="status">
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="aspect-[3/4] rounded-2xl bg-lilas-pale motion-safe:animate-pulse" aria-hidden="true" />
      ))}
      <span className="sr-only">Chargement des profils…</span>
    </div>
  );
}

// ─── Étape 4 : résultat ─────────────────────────────────────────────────────

/** Mise en avant du premier profil renvoyé par l'API (le plus proche), comme la carte « Lila » d'origine. */
function ScreenFour({ providers, status, error, onRetry }) {
  const featured = providers[0];
  return (
    <StepFrame>
      <div className="space-y-6 rounded-3xl bg-nuit px-5 py-6 text-white">
        <h1 id="page-title" tabIndex={-1} className="text-center font-sans text-lg font-normal text-white outline-none">
          {status === 'ready'
            ? `${providers.length} résultat${providers.length > 1 ? 's' : ''}`
            : 'Résultat'}
        </h1>

        {status === 'loading' || status === 'idle' ? (
          <div className="flex justify-center py-16" role="status">
            <Loader2 className="h-6 w-6 animate-spin" aria-hidden="true" />
            <span className="sr-only">Chargement…</span>
          </div>
        ) : status === 'error' ? (
          <Notice
            tone="error"
            action={
              <Button size="sm" variant="secondary" onClick={onRetry}>
                Réessayer
              </Button>
            }
          >
            {error}
          </Notice>
        ) : !featured ? (
          <p className="py-10 text-center">Aucun praticien n'est disponible dans la zone pour le moment.</p>
        ) : (
          <>
            <div className="relative mx-auto w-64 overflow-hidden rounded-3xl">
              <Avatar name={featured.userName} src={featured.images?.[0]} className="aspect-[4/5] w-full text-5xl" />
              <span className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full bg-nuit/60 px-2.5 py-1 text-[0.8rem]">
                <span className="h-2 w-2 rounded-full bg-white" aria-hidden="true" />
                {featured.category}
              </span>
              <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-nuit/80 to-transparent px-4 pb-4 pt-12">
                <p className="font-display text-4xl font-medium text-white">{featured.userName}</p>
              </div>
            </div>
            <div className="flex justify-center">
              <a
                href={`#/praticien/${featured.id}`}
                className="inline-flex h-12 items-center rounded-full bg-white px-6 font-bold text-nuit hover:bg-brume"
              >
                Voir le profil
              </a>
            </div>
          </>
        )}
      </div>
      <Button size="lg" variant="secondary" className="w-full" onClick={() => goTo(STEPS.home)}>
        Nouvelle recherche
      </Button>
    </StepFrame>
  );
}
