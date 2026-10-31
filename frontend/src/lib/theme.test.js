import { describe, expect, it } from 'vitest';
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import {
  MAP_COLORS,
  MAP_STYLE_DARK,
  MAP_STYLE_LIGHT,
  THEME_COLORS,
  THEME_EVENT,
  THEME_KEY,
  applyTheme,
  currentTheme,
  initTheme,
  mapStyleUrl,
  resolveTheme,
  storedTheme,
  systemTheme,
  toggleTheme,
} from './theme';

// Fausse fenêtre : l'environnement Vitest est « node » (ni jsdom ni happy-dom).
function fakeWindow({ system = 'light', stored = null, storageBroken = false } = {}) {
  const listeners = {};
  const store = new Map(stored ? [[THEME_KEY, stored]] : []);
  const meta = { content: '', setAttribute(name, value) { if (name === 'content') this.content = value; } };
  const media = {
    matches: system === 'dark',
    listeners: new Set(),
    addEventListener(_, fn) { this.listeners.add(fn); },
    removeEventListener(_, fn) { this.listeners.delete(fn); },
  };
  const denied = () => { throw new Error('stockage refusé'); };
  const win = {
    document: {
      documentElement: { dataset: {} },
      querySelector: (selector) => (selector === 'meta[name="theme-color"]' ? meta : null),
    },
    localStorage: storageBroken
      ? { getItem: denied, setItem: denied, removeItem: denied }
      : {
          getItem: (key) => (store.has(key) ? store.get(key) : null),
          setItem: (key, value) => store.set(key, String(value)),
          removeItem: (key) => store.delete(key),
        },
    matchMedia: () => media,
    addEventListener: (type, fn) => (listeners[type] ||= new Set()).add(fn),
    removeEventListener: (type, fn) => listeners[type]?.delete(fn),
    dispatchEvent: (event) => { (listeners[event.type] || []).forEach((fn) => fn(event)); return true; },
  };
  return {
    win, meta, store, media, listeners,
    setSystem(next) { media.matches = next === 'dark'; media.listeners.forEach((fn) => fn()); },
  };
}

describe('résolution du thème', () => {
  it('un choix enregistré l’emporte sur le réglage du système', () => {
    expect(resolveTheme(fakeWindow({ system: 'dark', stored: 'light' }).win)).toBe('light');
    expect(resolveTheme(fakeWindow({ system: 'light', stored: 'dark' }).win)).toBe('dark');
  });

  it('sans choix, suit le système ; une valeur inconnue est ignorée', () => {
    expect(resolveTheme(fakeWindow({ system: 'dark' }).win)).toBe('dark');
    expect(storedTheme(fakeWindow({ stored: 'violet' }).win)).toBeNull();
    expect(resolveTheme(fakeWindow({ system: 'light', stored: 'violet' }).win)).toBe('light');
  });

  it('retombe sur le thème jour si le stockage ou matchMedia sont inaccessibles', () => {
    const { win } = fakeWindow({ storageBroken: true });
    expect(storedTheme(win)).toBeNull();
    win.matchMedia = () => { throw new Error('indisponible'); };
    expect(systemTheme(win)).toBe('light');
    expect(resolveTheme(win)).toBe('light');
  });
});

describe('application et bascule', () => {
  it('applyTheme pose le thème, la couleur de barre et prévient une seule fois', () => {
    const { win, meta } = fakeWindow();
    let events = 0;
    win.addEventListener(THEME_EVENT, () => { events += 1; });
    expect(applyTheme('dark', win)).toBe(true);
    expect(currentTheme(win.document)).toBe('dark');
    expect(meta.content).toBe(THEME_COLORS.dark);
    expect(applyTheme('dark', win)).toBe(false);
    expect(events).toBe(1);
  });

  it('un clic enregistre un choix, un second clic qui rejoint le système l’efface', () => {
    const { win, store } = fakeWindow({ system: 'light' });
    applyTheme('light', win);
    expect(toggleTheme(win)).toBe('dark');
    expect(store.get(THEME_KEY)).toBe('dark');
    expect(toggleTheme(win)).toBe('light');
    expect(store.has(THEME_KEY)).toBe(false); // l'application suit de nouveau le système
  });

  it('même logique quand le système est sombre', () => {
    const { win, store } = fakeWindow({ system: 'dark' });
    applyTheme('dark', win);
    expect(toggleTheme(win)).toBe('light');
    expect(store.get(THEME_KEY)).toBe('light');
    expect(toggleTheme(win)).toBe('dark');
    expect(store.has(THEME_KEY)).toBe(false);
  });

  it('la bascule fonctionne même si le stockage est refusé', () => {
    const { win } = fakeWindow({ storageBroken: true });
    applyTheme('light', win);
    expect(() => toggleTheme(win)).not.toThrow();
    expect(currentTheme(win.document)).toBe('dark');
  });
});

describe('initTheme', () => {
  it('applique le thème résolu au démarrage', () => {
    const { win } = fakeWindow({ system: 'dark' });
    initTheme(win);
    expect(currentTheme(win.document)).toBe('dark');
  });

  it('suit le système en direct tant qu’aucun choix n’est enregistré', () => {
    const ctx = fakeWindow({ system: 'light' });
    initTheme(ctx.win);
    ctx.setSystem('dark');
    expect(currentTheme(ctx.win.document)).toBe('dark');
    ctx.setSystem('light');
    expect(currentTheme(ctx.win.document)).toBe('light');
  });

  it('ignore le système quand la personne a choisi', () => {
    const ctx = fakeWindow({ system: 'light', stored: 'light' });
    initTheme(ctx.win);
    ctx.setSystem('dark');
    expect(currentTheme(ctx.win.document)).toBe('light');
  });

  it('se synchronise avec un autre onglet et se désabonne proprement', () => {
    const ctx = fakeWindow({ system: 'light' });
    const stop = initTheme(ctx.win);
    ctx.store.set(THEME_KEY, 'dark');
    ctx.win.dispatchEvent(Object.assign(new Event('storage'), { key: THEME_KEY }));
    expect(currentTheme(ctx.win.document)).toBe('dark');
    ctx.win.dispatchEvent(Object.assign(new Event('storage'), { key: 'autre.cle' })); // sans effet
    stop();
    expect(ctx.media.listeners.size).toBe(0);
    expect(ctx.listeners.storage.size).toBe(0);
  });
});

describe('fond de carte', () => {
  it('clair par défaut, sombre la nuit, surchargeable par variable d’environnement', () => {
    expect(mapStyleUrl('light', {})).toBe(MAP_STYLE_LIGHT);
    expect(mapStyleUrl('dark', {})).toBe(MAP_STYLE_DARK);
    expect(MAP_STYLE_DARK).toBe('https://tiles.openfreemap.org/styles/dark');
    expect(mapStyleUrl('light', { VITE_MAP_STYLE_URL: 'https://exemple.test/jour' })).toBe('https://exemple.test/jour');
    expect(mapStyleUrl('dark', { VITE_MAP_STYLE_URL: 'https://exemple.test/jour' })).toBe(MAP_STYLE_DARK);
    expect(mapStyleUrl('dark', { VITE_MAP_STYLE_DARK_URL: 'https://exemple.test/nuit' })).toBe('https://exemple.test/nuit');
  });

  it('chaque thème définit les trois couleurs de la carte', () => {
    for (const theme of ['light', 'dark'])
      expect(Object.keys(MAP_COLORS[theme]).sort()).toEqual(['meeting', 'passed', 'remaining']);
  });
});

// --- Jetons de couleur (src/theme.css) ---------------------------------------------------------

const SRC = new URL('..', import.meta.url).pathname;
const css = readFileSync(join(SRC, 'theme.css'), 'utf8');

function tokensOf(selectorStart) {
  const start = css.indexOf(selectorStart);
  const block = css.slice(start, css.indexOf('}', start));
  const map = {};
  for (const [, name, r, g, b] of block.matchAll(/--c-([a-z0-9-]+):\s*(\d+)\s+(\d+)\s+(\d+);/g)) map[name] = [+r, +g, +b];
  return map;
}
const LIGHT = tokensOf(':root {');
const DARK = tokensOf(":root[data-theme='dark'] {");

const luminance = ([r, g, b]) => {
  const f = (c) => ((c /= 255) <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
};
const contrast = (a, b) => {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
};
const WHITE = [255, 255, 255];

describe('jetons de couleur', () => {
  it('le mode nuit définit exactement les mêmes jetons que le mode jour', () => {
    expect(Object.keys(DARK).sort()).toEqual(Object.keys(LIGHT).sort());
    expect(Object.keys(LIGHT).length).toBeGreaterThan(25);
  });

  it('chaque jeton utilisé par Tailwind existe dans theme.css', () => {
    const config = readFileSync(join(SRC, '..', 'tailwind.config.js'), 'utf8');
    const used = [...config.matchAll(/token\('([a-z0-9-]+)'\)/g)].map((m) => m[1]);
    expect(used.length).toBeGreaterThan(15);
    for (const name of used) expect(LIGHT, `jeton --c-${name}`).toHaveProperty(name);
  });

  // [premier plan, arrière-plan, seuil, thème facultatif]. Texte : 4,5 (AA) ; icône ou grand glyphe : 3.
  const TEXT = 4.5;
  const UI = 3;
  const PAIRS = [
    ['nuit', 'brume', TEXT], ['nuit', 'carte', TEXT], ['nuit', 'lilas', TEXT], ['nuit', 'lilas-pale', TEXT],
    ['nuit-doux', 'brume', TEXT], ['nuit-doux', 'carte', TEXT], ['nuit-doux', 'lilas-pale', TEXT],
    ['sur-nuit', 'nuit', TEXT],
    ['nuit', 'rose-soft', TEXT], ['rose-ink', 'rose-soft', TEXT], ['nuit', 'rose-pale', TEXT],
    ['nuit-800', 'rose-pale', TEXT], ['rose-ink', 'rose-pale', TEXT], ['rose-ink', 'carte', TEXT],
    ['rose-deep', 'brume', TEXT], ['rose-deep', 'carte', TEXT], ['rose-deep', 'rose-pale', TEXT],
    ['rose-deep', 'rose-soft', UI],
    ['rouge-deep', 'brume', TEXT], ['rouge-deep', 'carte', TEXT], ['rouge-deep', 'rouge-pale', TEXT],
    ['rouge-deep', 'rose-pale', TEXT],
    ['sauge', 'brume', TEXT], ['sauge', 'carte', TEXT], ['sauge', 'sauge-pale', TEXT],
    ['sauge', 'sauge-wash', TEXT], ['sauge', 'lilas-pale', TEXT], ['nuit', 'sauge-wash', TEXT],
    ['alerte', 'brume', TEXT], ['alerte', 'carte', TEXT], ['alerte', 'alerte-pale', TEXT],
    ['alerte', 'sauge-wash', TEXT],
    ['nuit', 'attention-pale', TEXT],
    ['sur-profond', 'profond', TEXT], ['sur-profond-doux', 'profond', TEXT], ['sur-profond-accent', 'profond', UI],
    // Icônes et pastilles
    ['rose', 'brume', UI], ['rose', 'carte', UI], ['rose', 'rose-pale', UI], ['rose', 'lilas-pale', UI],
    ['rouge', 'carte', UI],
    ['rouge', 'rose-soft', UI, 'light'], // astérisque du hero, le jour
    ['rouge-deep', 'rose-soft', UI, 'dark'], // astérisque du hero, la nuit (surcharge dans theme.css)
  ];
  // Blanc littéral posé sur une surface colorée (identique dans les deux thèmes).
  const WHITE_ON = [['rouge', TEXT], ['rose', UI], ['sauge-fill', UI], ['violet', TEXT]];

  for (const [name, tokens] of [['jour', LIGHT], ['nuit', DARK]]) {
    const theme = name === 'jour' ? 'light' : 'dark';
    it(`contrastes WCAG AA du mode ${name}`, () => {
      const failures = [];
      for (const [fg, bg, min, only] of PAIRS) {
        if (only && only !== theme) continue;
        const ratio = contrast(tokens[fg], tokens[bg]);
        if (ratio < min) failures.push(`${fg} sur ${bg} : ${ratio.toFixed(2)} < ${min}`);
      }
      for (const [bg, min] of WHITE_ON) {
        const ratio = contrast(WHITE, tokens[bg]);
        if (ratio < min) failures.push(`blanc sur ${bg} : ${ratio.toFixed(2)} < ${min}`);
      }
      expect(failures).toEqual([]);
    });
  }

  it('la couleur de la barre du navigateur correspond au fond de page', () => {
    const hex = (rgb) => '#' + rgb.map((c) => c.toString(16).padStart(2, '0')).join('').toUpperCase();
    expect(THEME_COLORS.light).toBe(hex(LIGHT.brume));
    expect(THEME_COLORS.dark).toBe(hex(DARK.brume));
    const html = readFileSync(join(SRC, '..', 'index.html'), 'utf8');
    expect(html).toContain(THEME_COLORS.light);
    expect(html).toContain(THEME_COLORS.dark);
    expect(html).toContain(THEME_KEY); // script de premier affichage aligné sur lib/theme.js
  });
});

describe('plus de couleur en dur', () => {
  const walk = (dir) =>
    readdirSync(dir).flatMap((name) => {
      const path = join(dir, name);
      return statSync(path).isDirectory() ? walk(path) : [path];
    });
  const files = walk(SRC);

  it('les feuilles de style n’utilisent que des jetons (hors blanc des marqueurs et ombres très légères)', () => {
    const allowed = new Set(['#fff', '#777', '#2227', '#28231605', '#56382013', '#30283d33', '#283b2406', '#3337190c']);
    const offenders = [];
    for (const file of files.filter((f) => f.endsWith('.css') && !f.endsWith('theme.css'))) {
      for (const [hex] of readFileSync(file, 'utf8').matchAll(/#[0-9a-fA-F]{3,8}\b/g))
        if (!allowed.has(hex.toLowerCase())) offenders.push(`${file.replace(SRC, 'src/')} : ${hex}`);
    }
    expect(offenders).toEqual([]);
  });

  it('les composants n’utilisent ni l’ancien orange, ni bg-white, ni couleur arbitraire', () => {
    const offenders = [];
    for (const file of files.filter((f) => /\.(js|jsx)$/.test(f) && !f.endsWith('.test.js'))) {
      const text = readFileSync(file, 'utf8');
      for (const pattern of [/abricot/, /\bbg-white\b/, /\b(?:bg|text|border|ring|fill|stroke)-\[#/])
        if (pattern.test(text)) offenders.push(`${file.replace(SRC, 'src/')} : ${pattern}`);
    }
    expect(offenders).toEqual([]);
  });
});
