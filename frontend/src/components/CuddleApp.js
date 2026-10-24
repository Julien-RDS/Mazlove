import React, { useCallback, useEffect, useRef, useState } from 'react';
import { CalendarDays, LogOut, UserRound } from 'lucide-react';
import { Api, SESSION_EXPIRED_EVENT, tokenStore } from '../api';
import { loginPath, navigate, useRoute } from '../lib/router';
import AuthScreen from './AuthScreen';
import DiscoverScreen from './DiscoverScreen';
import ProviderOnboarding from './ProviderOnboarding';
import ProviderScreen from './ProviderScreen';
import { SessionDetailScreen, SessionsScreen } from './SessionsScreen';
import { Button, Spinner, cx } from './ui';

/** Point de recherche d'origine (Paris centre), utilisé tel quel pour la sélection des praticiens. */
const DEFAULT_POSITION = { lat: 48.87, lng: 2.35, label: 'Paris centre' };

/** Écrans qui exigent d'être connecté. */
const PROTECTED_ROUTES = ['sessions', 'session', 'onboarding'];

/**
 * Coquille de l'application : session utilisateur, routage, en-tête et notifications.
 * Chaque écran reçoit seulement ce dont il a besoin (utilisateur, position, notify).
 */
export default function CuddleApp() {
  const route = useRoute();
  const [user, setUser] = useState(null);
  // Sans jeton stocké, on sait tout de suite que personne n'est connecté.
  const [authReady, setAuthReady] = useState(() => !tokenStore.get());
  // Position fixe tant que les critères de sélection n'ont pas été revus (cf. DiscoverScreen).
  const position = DEFAULT_POSITION;
  const [toasts, setToasts] = useState([]);
  const firstRender = useRef(true);

  const notify = useCallback((message) => {
    const id = Date.now() + Math.random();
    setToasts((current) => [...current, { id, message }]);
    setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), 4000);
  }, []);

  /** Recharge le profil (après connexion, création de fiche…) et le renvoie. */
  const refreshUser = useCallback(async () => {
    if (!tokenStore.get()) {
      setUser(null);
      setAuthReady(true);
      return null;
    }
    try {
      const me = await Api.me();
      setUser(me);
      return me;
    } catch {
      setUser(null);
      return null;
    } finally {
      setAuthReady(true);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  // Jeton expiré détecté par le client HTTP : on repasse en visiteur sans recharger la page.
  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      notify('Votre session a expiré. Reconnectez-vous pour continuer.');
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
  }, [notify]);

  // À chaque changement d'écran : retour en haut et focus sur le titre, pour que les lecteurs
  // d'écran annoncent la nouvelle page (une SPA ne le fait pas d'elle-même).
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    window.scrollTo(0, 0);
    requestAnimationFrame(() => document.getElementById('page-title')?.focus());
  }, [route.path]);

  const needsAuth = PROTECTED_ROUTES.includes(route.name);
  useEffect(() => {
    if (needsAuth && authReady && !user) navigate(loginPath(route.path), { replace: true });
  }, [needsAuth, authReady, user, route.path]);

  const logout = () => {
    tokenStore.clear();
    setUser(null);
    notify('Vous êtes déconnecté.');
    navigate('/');
  };

  const renderScreen = () => {
    if (needsAuth && (!authReady || !user)) return <Spinner />;
    switch (route.name) {
      case 'discover':
        return <DiscoverScreen query={route.query} position={position} />;
      case 'provider':
        return <ProviderScreen key={route.params.id} id={route.params.id} user={user} position={position} notify={notify} />;
      case 'auth':
        return <AuthScreen query={route.query} onAuthenticated={refreshUser} />;
      case 'sessions':
        return <SessionsScreen user={user} />;
      case 'session':
        return <SessionDetailScreen key={route.params.id} id={route.params.id} notify={notify} />;
      case 'onboarding':
        return (
          <ProviderOnboarding
            user={user}
            position={position}
            onCreated={async (provider) => {
              await refreshUser();
              notify('Fiche publiée. Les clients peuvent maintenant réserver.');
              navigate(`/praticien/${provider.id}`, { replace: true });
            }}
          />
        );
      default:
        return (
          <div className="space-y-4 pt-2">
            <h1 id="page-title" tabIndex={-1} className="text-4xl font-semibold outline-none">
              Cette page n'existe pas
            </h1>
            <Button onClick={() => navigate('/')}>Revenir à l'accueil</Button>
          </div>
        );
    }
  };

  const navLink = (active) =>
    cx(
      'inline-flex h-10 items-center gap-2 rounded-full px-3 font-bold transition-colors',
      active ? 'bg-white text-nuit' : 'text-nuit-doux hover:bg-white hover:text-nuit'
    );

  return (
    <div className="flex min-h-screen flex-col">
      {/* Lien d'évitement : un bouton, car une ancre "#contenu" serait prise pour une route. */}
      <button
        type="button"
        onClick={() => document.getElementById('contenu')?.focus()}
        className="sr-only z-50 rounded-full bg-nuit px-4 py-2 font-bold text-white focus:not-sr-only focus:fixed focus:left-4 focus:top-3"
      >
        Aller au contenu
      </button>

      <header className="sticky top-0 z-30 border-b border-lilas/60 bg-brume/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between gap-3 px-4 sm:px-6">
          <a href="#/" className="font-display text-[1.7rem] font-semibold tracking-tight text-nuit">
            mazlove
          </a>
          <nav aria-label="Navigation principale" className="flex items-center gap-1">
            {user ? (
              <>
                {user.role === 'PROVIDER' && (
                  <a
                    href={user.providerId ? `#/praticien/${user.providerId}` : '#/profil-praticien'}
                    className={navLink(route.name === 'onboarding' || (route.name === 'provider' && Number(route.params.id) === user.providerId))}
                  >
                    <UserRound className="h-5 w-5" aria-hidden="true" />
                    <span className="sr-only sm:not-sr-only">{user.providerId ? 'Ma fiche' : 'Publier ma fiche'}</span>
                  </a>
                )}
                <a
                  href="#/seances"
                  className={navLink(route.name === 'sessions' || route.name === 'session')}
                  aria-current={route.name === 'sessions' ? 'page' : undefined}
                >
                  <CalendarDays className="h-5 w-5" aria-hidden="true" />
                  <span className="sr-only sm:not-sr-only">Mes séances</span>
                </a>
                <button type="button" onClick={logout} className={navLink(false)}>
                  <LogOut className="h-5 w-5" aria-hidden="true" />
                  <span className="sr-only sm:not-sr-only">Se déconnecter</span>
                </button>
              </>
            ) : (
              route.name !== 'auth' && (
                <a href={`#${loginPath(route.path)}`} className="inline-flex h-10 items-center rounded-full bg-nuit px-5 font-bold text-white hover:bg-nuit-800">
                  Se connecter
                </a>
              )
            )}
          </nav>
        </div>
      </header>

      <main id="contenu" tabIndex={-1} className="mx-auto w-full max-w-5xl flex-1 px-4 pb-16 pt-6 outline-none sm:px-6">
        {renderScreen()}
      </main>

      <footer className="border-t border-lilas/60">
        <p className="mx-auto max-w-5xl px-4 py-6 text-[0.95rem] text-nuit-doux sm:px-6">
          Mazlove propose uniquement des séances non sexuelles. Vous pouvez arrêter une séance à tout moment, sans
          justification.
        </p>
      </footer>

      <div className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4" role="status" aria-live="polite">
        {toasts.map((toast) => (
          <p key={toast.id} className="pointer-events-auto rounded-full bg-nuit px-5 py-2.5 font-bold text-white shadow-lg">
            {toast.message}
          </p>
        ))}
      </div>
    </div>
  );
}
