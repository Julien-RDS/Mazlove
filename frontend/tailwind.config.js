// Jetons de design Mazlove.
// Palette « veilleuse » : fond brume lavande, encre nuit, et un seul accent chaud (abricot)
// réservé aux moments qui comptent (code d'arrivée, étoiles). Contrastes vérifiés AA.
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        nuit: { DEFAULT: '#22231F', 800: '#3C3D34', doux: '#6B6B62' },
        brume: '#F8F7F3',
        lilas: { DEFAULT: '#E2DFD5', pale: '#F0EDE6' },
        abricot: { DEFAULT: '#F26B35', pale: '#FBE7DA' },
        sauge: { DEFAULT: '#2F6650', pale: '#DCEBE3' },                  // vérifié, séance en cours
        alerte: { DEFAULT: '#B4443A', pale: '#F6E1DE' },                 // erreurs, annulation
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
