import { useSyncExternalStore } from 'react';

// Mode jour / mode nuit. Règle : un choix explicite de la personne (localStorage) l'emporte ;
// sans choix, on suit le réglage du système, en direct. Un clic qui ramène au réglage du système
// efface le choix : l'application se remet à le suivre.
// La logique de premier affichage est dupliquée dans index.html (script en <head>) pour éviter un
// éclair de la mauvaise couleur avant le chargement du bundle ; garder les deux alignés.

export const THEME_KEY = 'mazlove.theme';
export const THEME_EVENT = 'mazlove:theme';
// Couleur de la barre du navigateur mobile = fond de page (jeton --c-brume de theme.css).
export const THEME_COLORS = { light: '#F8F7F3', dark: '#161219' };

const guard = (fn, fallback) => {
  try {
    return fn();
  } catch {
    return fallback; // stockage ou matchMedia indisponibles (navigation privée, vieux navigateur)
  }
};

export function systemTheme(win = window) {
  return guard(() => win.matchMedia('(prefers-color-scheme: dark)').matches, false) ? 'dark' : 'light';
}

export function storedTheme(win = window) {
  const value = guard(() => win.localStorage.getItem(THEME_KEY), null);
  return value === 'dark' || value === 'light' ? value : null;
}

export const resolveTheme = (win = window) => storedTheme(win) || systemTheme(win);

export function currentTheme(doc = document) {
  return doc.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
}

/** Pose le thème sur <html> et prévient les abonnés ; sans effet s'il ne change rien. */
export function applyTheme(theme, win = window) {
  const doc = win.document;
  const changed = doc.documentElement.dataset.theme !== theme;
  doc.documentElement.dataset.theme = theme;
  doc.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[theme]);
  if (changed) win.dispatchEvent(new Event(THEME_EVENT));
  return changed;
}

/** Bascule jour/nuit à la demande de la personne et retourne le nouveau thème. */
export function toggleTheme(win = window) {
  const next = currentTheme(win.document) === 'dark' ? 'light' : 'dark';
  guard(() => {
    if (next === systemTheme(win)) win.localStorage.removeItem(THEME_KEY);
    else win.localStorage.setItem(THEME_KEY, next);
  });
  applyTheme(next, win);
  return next;
}

/** Applique le thème au démarrage et le garde à jour (réglage système, autre onglet). */
export function initTheme(win = window) {
  applyTheme(resolveTheme(win), win);
  const sync = () => applyTheme(resolveTheme(win), win);
  const media = guard(() => win.matchMedia('(prefers-color-scheme: dark)'), null);
  media?.addEventListener?.('change', sync); // sans choix explicite, resolveTheme suit le système
  const onStorage = (event) => (event.key === THEME_KEY || event.key === null) && sync();
  win.addEventListener('storage', onStorage);
  return () => {
    media?.removeEventListener?.('change', sync);
    win.removeEventListener('storage', onStorage);
  };
}

const subscribe = (callback) => {
  window.addEventListener(THEME_EVENT, callback);
  return () => window.removeEventListener(THEME_EVENT, callback);
};

/** Thème affiché (« light » ou « dark »), mis à jour quand il change. */
export const useTheme = () => useSyncExternalStore(subscribe, () => currentTheme(), () => 'light');

// --- Carte : fond OpenFreeMap sombre la nuit, couleurs de tracé lisibles sur chaque fond. ---

export const MAP_STYLE_LIGHT = 'https://tiles.openfreemap.org/styles/liberty';
export const MAP_STYLE_DARK = 'https://tiles.openfreemap.org/styles/dark';

export const mapStyleUrl = (theme, env = import.meta.env) =>
  theme === 'dark'
    ? env.VITE_MAP_STYLE_DARK_URL || MAP_STYLE_DARK
    : env.VITE_MAP_STYLE_URL || MAP_STYLE_LIGHT;

export const MAP_COLORS = {
  light: { meeting: '#C8283E', passed: '#8E9098', remaining: '#4D6655' },
  dark: { meeting: '#E5546B', passed: '#7B7F8C', remaining: '#6FCF9F' },
};
