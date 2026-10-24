import { useEffect, useState } from 'react';

// Routage par le hash (#/praticien/12) : aucune configuration serveur, le bouton « précédent »
// fonctionne et chaque écran a une adresse qu'on peut partager ou mettre en favori.

const ROUTES = [
  { name: 'discover', pattern: /^\/$/ },
  { name: 'provider', pattern: /^\/praticien\/(\d+)$/, keys: ['id'] },
  { name: 'auth', pattern: /^\/connexion$/ },
  { name: 'sessions', pattern: /^\/seances$/ },
  { name: 'session', pattern: /^\/seances\/(\d+)$/, keys: ['id'] },
  { name: 'onboarding', pattern: /^\/profil-praticien$/ },
];

/** "#/seances/4?x=1" -> { name: 'session', params: { id: '4' }, query: { x: '1' }, path: '/seances/4?x=1' } */
export function parseLocation(hash) {
  const path = (hash || '').replace(/^#/, '') || '/';
  const [pathname, queryString = ''] = path.split('?');
  const query = Object.fromEntries(new URLSearchParams(queryString));
  for (const route of ROUTES) {
    const match = pathname.match(route.pattern);
    if (match) {
      const params = {};
      (route.keys || []).forEach((key, index) => {
        params[key] = match[index + 1];
      });
      return { name: route.name, params, query, path };
    }
  }
  return { name: 'notFound', params: {}, query, path };
}

export function useRoute() {
  const [route, setRoute] = useState(() => parseLocation(window.location.hash));
  useEffect(() => {
    const onChange = () => setRoute(parseLocation(window.location.hash));
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return route;
}

/**
 * Change d'écran. replace=true remplace l'entrée d'historique (redirections) pour que
 * « précédent » ne ramène pas sur une page qui redirige aussitôt.
 */
export function navigate(to, { replace = false } = {}) {
  if (replace) {
    window.history.replaceState(null, '', `#${to}`);
    window.dispatchEvent(new Event('hashchange'));
  } else {
    window.location.hash = to;
  }
}

/** Lien de connexion qui ramène ensuite à l'écran courant. */
export function loginPath(returnTo) {
  return returnTo && returnTo !== '/' ? `/connexion?retour=${encodeURIComponent(returnTo)}` : '/connexion';
}
