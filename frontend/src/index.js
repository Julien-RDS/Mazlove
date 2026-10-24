import React from 'react';
import ReactDOM from 'react-dom/client';
// Polices auto-hébergées (pas d'appel à Google Fonts : rien ne part chez un tiers, RGPD oblige).
import '@fontsource/fraunces/500.css';
import '@fontsource/fraunces/600.css';
import '@fontsource/atkinson-hyperlegible/400.css';
import '@fontsource/atkinson-hyperlegible/700.css';
import './index.css';
import CuddleApp from './components/CuddleApp';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <CuddleApp />
  </React.StrictMode>
);
