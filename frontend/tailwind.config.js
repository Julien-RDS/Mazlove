// Jetons de design Mazlove.
// Les couleurs viennent de src/theme.css (variables CSS) : le mode nuit n'a donc pas de variantes
// « dark: » à écrire composant par composant. Palette : fond brume, encre nuit, accents ROSE
// (identité) et ROUGE (code d'arrivée, SOS, favori), vert sauge pour « vérifié / en cours ».
// Les noms désignent des rôles : en mode nuit, `nuit` (encre) devient clair et `brume` (fond) sombre.
// Contrastes contrôlés par src/lib/theme.test.js (AA : 4,5 pour le texte, 3 pour les icônes).
const token = (name) => `rgb(var(--c-${name}) / <alpha-value>)`;

export default {
  darkMode: ['selector', '[data-theme="dark"]'],
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        nuit: { DEFAULT: token('nuit'), 800: token('nuit-800'), doux: token('nuit-doux') },
        'sur-nuit': token('sur-nuit'), // texte sur une surface `nuit`
        brume: token('brume'), // fond de page
        carte: token('carte'), // cartes et champs (blanc le jour)
        lilas: { DEFAULT: token('lilas'), pale: token('lilas-pale'), fort: token('lilas-fort') },
        rose: {
          DEFAULT: token('rose'), // surface sous icône blanche, étoiles
          pale: token('rose-pale'),
          soft: token('rose-soft'),
          line: token('rose-line'),
          deep: token('rose-deep'), // texte rose lisible
          ink: token('rose-ink'), // texte sur aplat rose
        },
        rouge: { DEFAULT: token('rouge'), pale: token('rouge-pale'), deep: token('rouge-deep') },
        sauge: { DEFAULT: token('sauge'), pale: token('sauge-pale') }, // vérifié, séance en cours
        alerte: { DEFAULT: token('alerte'), pale: token('alerte-pale') }, // erreurs, annulation
      },
      fontFamily: {
        // Fraunces : serif aux formes douces pour les titres ; Atkinson Hyperlegible : conçue
        // pour la lisibilité (malvoyance), adaptée à un public parfois fatigué ou anxieux.
        display: ['Fraunces', 'Georgia', 'serif'],
        sans: ['"Atkinson Hyperlegible"', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'],
      },
      keyframes: {
        'sheet-in': {
          from: { opacity: '0', transform: 'translateY(24px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'ticket-in': {
          '0%': { opacity: '0', transform: 'scale(0.96) rotate(-1.5deg)' },
          '100%': { opacity: '1', transform: 'scale(1) rotate(0deg)' },
        },
      },
      animation: {
        'sheet-in': 'sheet-in 220ms ease-out',
        'ticket-in': 'ticket-in 450ms cubic-bezier(0.2, 0.8, 0.2, 1)',
      },
    },
  },
  plugins: [],
};
