import React from 'react';
import { Moon, Sun } from 'lucide-react';
import { toggleTheme, useTheme } from '../lib/theme';

// Bouton jour / nuit de l'en-tête. L'étiquette reste « Mode nuit » : c'est aria-pressed qui annonce l'état.
export default function ThemeToggle() {
  const dark = useTheme() === 'dark';
  const Icon = dark ? Sun : Moon;
  return (
    <button
      type="button"
      onClick={() => toggleTheme()}
      className="shell-theme"
      aria-pressed={dark}
      aria-label="Mode nuit"
      title={dark ? 'Revenir au mode jour' : 'Activer le mode nuit'}
    >
      <Icon className="h-5 w-5" aria-hidden="true" />
    </button>
  );
}
