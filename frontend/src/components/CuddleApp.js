import ProviderJourney, { JourneyNotice } from "./tracking/ProviderJourney";
import React, {
  lazy,
  Suspense,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  CalendarDays,
  LogOut,
  UserRound,
  Heart,
  Compass,
  ArrowUpRight,
  Sparkles,
} from "lucide-react";
import ThemeToggle from "./ThemeToggle";
import { wsClient } from "../lib/ws";
import { SESSION_UPDATED_EVENT } from "../lib/sessionTracking";
import { Api, SESSION_EXPIRED_EVENT, tokenStore } from "../api";
import { loginPath, navigate, useRoute } from "../lib/router";
const AuthScreen = lazy(() => import("./AuthScreen"));
import DiscoverScreen from "./DiscoverScreen";
const ProviderOnboarding = lazy(() => import("./ProviderOnboarding"));
const ProviderScreen = lazy(() => import("./ProviderScreen"));
const SessionsScreen = lazy(() =>
  import("./SessionsScreen").then((m) => ({ default: m.SessionsScreen })),
);
const SessionDetailScreen = lazy(() =>
  import("./SessionsScreen").then((m) => ({ default: m.SessionDetailScreen })),
);
import { Button, Spinner, cx } from "./ui";
import "./shell.css";

/** Point de recherche d'origine (Paris centre), utilisé tel quel pour la sélection des praticiens. */
const DEFAULT_POSITION = { lat: 48.87, lng: 2.35, label: "Paris centre" };

/** Écrans qui exigent d'être connecté. */
const PROTECTED_ROUTES = ["sessions", "session", "onboarding"];

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
    setTimeout(
      () => setToasts((current) => current.filter((t) => t.id !== id)),
      4000,
    );
  }, []);

  /** Recharge le profil (après connexion, création de fiche…) et le renvoie. */
  const refreshUser = useCallback(async () => {
    const requestedToken = tokenStore.get();
    if (!requestedToken) {
      setUser(null);
      setAuthReady(true);
      return null;
    }
    try {
      const me = await Api.me();
      if (tokenStore.get() !== requestedToken) return null;
      setUser(me);
      return me;
    } catch {
      if (tokenStore.get() !== requestedToken) return null;
      setUser(null);
      return null;
    } finally {
      if (tokenStore.get() === requestedToken) setAuthReady(true);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  useEffect(() => {
    const token = tokenStore.get();
    if (!user || !token) return undefined;
    const client = wsClient(token, {
      reconnectDelay: 5000,
      onConnect: () =>
        client.subscribe("/user/queue/notifications", (frame) => {
          try {
            const event = JSON.parse(frame.body);
            if (typeof event.message === "string") notify(event.message);
            if (event.sessionId)
              window.dispatchEvent(new Event(SESSION_UPDATED_EVENT));
          } catch {
            // Ignore malformed events without breaking the connection.
          }
        }),
    });
    client.activate();
    return () => {
      client.deactivate();
    };
  }, [user, notify]);

  // Jeton expiré détecté par le client HTTP : on repasse en visiteur sans recharger la page.
  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      notify("Votre session a expiré. Reconnectez-vous pour continuer.");
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
  }, [notify]);

  useEffect(() => {
    const storage = (e) => {
      if (e.key === "mazlove.token") {
        setUser(null);
        setAuthReady(!e.newValue);
        refreshUser();
      }
    };
    window.addEventListener("storage", storage);
    return () => window.removeEventListener("storage", storage);
  }, [refreshUser]);

  // À chaque changement d'écran : retour en haut et focus sur le titre, pour que les lecteurs
  // d'écran annoncent la nouvelle page (une SPA ne le fait pas d'elle-même).
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    window.scrollTo(0, 0);
    requestAnimationFrame(() => document.getElementById("page-title")?.focus());
  }, [route.path]);

  const needsAuth = PROTECTED_ROUTES.includes(route.name);
  useEffect(() => {
    if (needsAuth && authReady && !user)
      navigate(loginPath(route.path), { replace: true });
  }, [needsAuth, authReady, user, route.path]);

  const logout = () => {
    tokenStore.clear();
    setUser(null);
    notify("Vous êtes déconnecté.");
    navigate("/");
  };

  const renderScreen = () => {
    if (needsAuth && (!authReady || !user)) return <Spinner />;
    switch (route.name) {
      case "discover":
        return (
          <DiscoverScreen
            query={route.query}
            position={position}
            user={user}
            notify={notify}
          />
        );
      case "provider":
        return (
          <ProviderScreen
            key={route.params.id}
            id={route.params.id}
            user={user}
            position={position}
            notify={notify}
          />
        );
      case "auth":
        return <AuthScreen query={route.query} onAuthenticated={refreshUser} />;
      case "sessions":
        return <SessionsScreen user={user} />;
      case "session":
        return (
          <SessionDetailScreen
            key={route.params.id}
            id={route.params.id}
            notify={notify}
          />
        );
      case "onboarding":
        return (
          <ProviderOnboarding
            user={user}
            position={position}
            onCreated={async (provider) => {
              await refreshUser();
              notify("Fiche publiée. Les clients peuvent maintenant réserver.");
              navigate(`/praticien/${provider.id}`, { replace: true });
            }}
          />
        );
      default:
        return (
          <div className="space-y-4 pt-2">
            <h1
              id="page-title"
              tabIndex={-1}
              className="text-4xl font-semibold outline-none"
            >
              Cette page n'existe pas
            </h1>
            <Button onClick={() => navigate("/")}>Revenir à l'accueil</Button>
          </div>
        );
    }
  };

  const favoritesActive =
    route.name === "discover" && route.query.vue === "favoris";
  const navItems = [
    {
      label: "Explorer",
      href: "#/",
      icon: Compass,
      active: route.name === "discover" && !favoritesActive,
    },
    {
      label: "Mes favoris",
      href: "#/?vue=favoris",
      icon: Heart,
      active: favoritesActive,
    },
    {
      label: "Mes séances",
      href: "#/seances",
      icon: CalendarDays,
      active: route.name === "sessions" || route.name === "session",
    },
  ];
  const navLink = (active) => cx("shell-nav-link", active && "is-active");

  return (
    <ProviderJourney token={user ? tokenStore.get() : null} user={user}>
      <div className="app-shell">
        {/* Lien d'évitement : un bouton, car une ancre "#contenu" serait prise pour une route. */}
        <button
          type="button"
          onClick={() => document.getElementById("contenu")?.focus()}
          className="sr-only z-50 rounded-full bg-nuit px-4 py-2 font-bold text-sur-nuit focus:not-sr-only focus:fixed focus:left-4 focus:top-3"
        >
          Aller au contenu
        </button>

        <header className="shell-header">
          <div className="shell-header-inner">
            <a href="#/" className="shell-logo" aria-label="Mazlove, accueil">
              <span className="shell-logo-symbol" aria-hidden="true">
                <Heart size={21} fill="currentColor" strokeWidth={1.5} />
              </span>
              mazlove<span className="shell-logo-dot">.</span>
            </a>
            <nav
              aria-label="Navigation principale"
              className="shell-desktop-nav"
            >
              {navItems.map(({ label, href, icon: Icon, active }) => (
                <a
                  key={label}
                  href={href}
                  className={navLink(active)}
                  aria-current={active ? "page" : undefined}
                >
                  <Icon size={17} aria-hidden="true" />
                  <span>{label}</span>
                </a>
              ))}
            </nav>
            <div className="shell-account">
              <ThemeToggle />
              {user ? (
                <>
                  {user.role === "PROVIDER" && (
                    <a
                      href={
                        user.providerId
                          ? `#/praticien/${user.providerId}`
                          : "#/profil-praticien"
                      }
                      className="shell-provider-link"
                    >
                      <UserRound className="h-5 w-5" aria-hidden="true" />
                      <span className="sr-only sm:not-sr-only">
                        {user.providerId ? "Ma fiche" : "Publier ma fiche"}
                      </span>
                    </a>
                  )}
                  <span
                    className="shell-avatar"
                    aria-label={`Compte de ${user.name || "membre"}`}
                  >
                    {(user.name || "M").slice(0, 1).toUpperCase()}
                  </span>
                  <button
                    type="button"
                    onClick={logout}
                    className="shell-logout"
                    aria-label="Se déconnecter"
                    title="Se déconnecter"
                  >
                    <LogOut className="h-5 w-5" aria-hidden="true" />
                  </button>
                </>
              ) : (
                route.name !== "auth" && (
                  <a href={`#${loginPath(route.path)}`} className="shell-login">
                    Se connecter <ArrowUpRight size={16} aria-hidden="true" />
                  </a>
                )
              )}
            </div>
          </div>
        </header>
        <JourneyNotice
          currentSessionId={route.name === "session" ? route.params.id : null}
        />

        <main id="contenu" tabIndex={-1} className="shell-main">
          <Suspense fallback={<Spinner />}>{renderScreen()}</Suspense>
        </main>

        <footer className="shell-footer">
          <div className="shell-footer-inner">
            <span className="shell-footer-signature">
              <Sparkles size={18} aria-hidden="true" /> Un peu plus de lien. Un
              peu plus de vous.
            </span>
            <p>Séances non sexuelles. Votre consentement, à chaque instant.</p>
          </div>
        </footer>

        <nav className="shell-mobile-nav" aria-label="Navigation mobile">
          {navItems.map(({ label, href, icon: Icon, active }) => (
            <a
              key={label}
              href={href}
              className={cx("shell-mobile-link", active && "is-active")}
              aria-current={active ? "page" : undefined}
            >
              <Icon size={21} aria-hidden="true" />
              <span>{label}</span>
            </a>
          ))}
        </nav>

        <div
          className="shell-toasts pointer-events-none fixed inset-x-0 z-50 flex flex-col items-center gap-2 px-4"
          role="status"
          aria-live="polite"
        >
          {toasts.map((toast) => (
            <p
              key={toast.id}
              className="pointer-events-auto rounded-full bg-nuit px-5 py-2.5 font-bold text-sur-nuit shadow-lg"
            >
              {toast.message}
            </p>
          ))}
        </div>
      </div>
    </ProviderJourney>
  );
}
