// Client HTTP unique de l'application : ajoute le JWT, convertit les erreurs de l'API
// (format { status, message, fields }) en ApiError lisibles, et gère l'expiration de session.

// Vide en développement (le proxy Vite relaie /api) ; en production, URL de l'API si elle
// est servie sur un autre domaine, ex. VITE_API_URL=https://api.mazlove.fr
const API_BASE = import.meta.env.VITE_API_URL ?? '';

const TOKEN_KEY = 'mazlove.token';

/** Émis quand l'API répond 401 à une requête authentifiée : le jeton a expiré. */
export const SESSION_EXPIRED_EVENT = 'mazlove:session-expired';

// Le JWT vit dans le localStorage pour survivre au rechargement de la page.
// Contrepartie : tout script injecté (XSS) pourrait le lire. React échappe le contenu affiché,
// et l'application n'insère jamais de HTML brut ; à revoir si l'on intègre des scripts tiers.
export const tokenStore = {
  get: () => {
    try {
      return window.localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: (token) => {
    try {
      window.localStorage.setItem(TOKEN_KEY, token);
    } catch {
      // Navigation privée stricte : la session ne durera que le temps de l'onglet.
    }
  },
  clear: () => {
    try {
      window.localStorage.removeItem(TOKEN_KEY);
    } catch {
      // Rien à nettoyer.
    }
  },
};

export class ApiError extends Error {
  /**
   * @param {string} message phrase affichable telle quelle à l'utilisateur
   * @param {number} status code HTTP (0 = serveur injoignable)
   * @param {Object<string,string>|null} fields erreurs par champ de formulaire
   */
  constructor(message, status, fields = null) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}

const FALLBACK_MESSAGES = {
  400: 'Certaines informations sont à corriger.',
  401: 'Connectez-vous pour continuer.',
  403: "Vous n'avez pas accès à cette page.",
  404: "Cette page n'existe pas ou n'est plus disponible.",
  409: 'Cette action entre en conflit avec une autre. Rechargez et réessayez.',
};

/**
 * Appel générique.
 * @param {string} path chemin après /api, ex. "/providers?lat=48.87&lng=2.35"
 * @param {{ method?: string, body?: any, auth?: boolean }} options
 *   auth=false pour /auth/* : un 401 y signifie « mauvais identifiants », pas « session expirée ».
 */
export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = tokenStore.get();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE}/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.', 0);
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && auth && token) {
      tokenStore.clear();
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    const message =
      data?.message ||
      FALLBACK_MESSAGES[response.status] ||
      'Une erreur inattendue est survenue. Réessayez dans quelques instants.';
    throw new ApiError(message, response.status, data?.fields || null);
  }
  return data;
}

/** Tous les endpoints utilisés par l'interface, au même endroit. */
export const Api = {
  // Praticiens (publics)
  // Sélection d'origine : 50 km autour du point, du plus proche au plus éloigné (fixé côté serveur).
  // Sans pagination demandée, l'URL reste exactement /api/providers?lat=…&lng=…
  providersNearby: ({ lat, lng, page, size }) => {
    const params = new URLSearchParams({ lat, lng });
    if (page !== undefined) params.set('page', page);
    if (size !== undefined) params.set('size', size);
    return api(`/providers?${params}`);
  },
  provider: (id) => api(`/providers/${id}`),
  providerReviews: (id) => api(`/providers/${id}/reviews`),
  providerServices: (id) => api(`/providers/${id}/services`),
  createProvider: (body) => api('/providers', { method: 'POST', body }),
  addReview: (providerId, body) => api(`/providers/${providerId}/reviews`, { method: 'POST', body }),

  // Authentification en deux étapes
  register: (body) => api('/auth/register', { method: 'POST', body, auth: false }),
  login: (body) => api('/auth/login', { method: 'POST', body, auth: false }),
  verifyOtp: (body) => api('/auth/verify-otp', { method: 'POST', body, auth: false }),

  // Compte connecté
  me: () => api('/users/me'),
  mySessions: (page = 0, size = 50) => api(`/users/me/sessions?page=${page}&size=${size}`),

  // Séances
  session: (id) => api(`/sessions/${id}`),
  createSession: (body) => api('/sessions', { method: 'POST', body }),
  validateArrival: (id, code) => api(`/sessions/${id}/validate-arrival`, { method: 'POST', body: { code } }),
  confirmEnd: (id) => api(`/sessions/${id}/confirm-end`, { method: 'POST' }),
  cancelSession: (id) => api(`/sessions/${id}/cancel`, { method: 'POST' }),
};
