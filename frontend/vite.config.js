import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Les composants gardent l'extension .js (convention du projet) tout en contenant du JSX :
// par défaut Vite ne transforme le JSX que dans les .jsx, d'où les trois réglages "loader" ci-dessous.
export default defineConfig({
  plugins: [react({ include: /\.(js|jsx)$/ })],
  esbuild: {
    loader: 'jsx',
    include: /src\/.*\.jsx?$/,
    exclude: [],
  },
  optimizeDeps: {
    esbuildOptions: { loader: { '.js': 'jsx' } },
  },
  server: {
    port: 3000,
    // En développement, /api est relayé vers Spring Boot : le navigateur reste sur la même
    // origine (localhost:3000), donc aucune requête CORS ni cookie tiers.
    proxy: {
      '/api': 'http://localhost:3001',
      '/uploads': 'http://localhost:3001',
      '/ws': { target: 'http://localhost:3001', ws: true },
    },
  },
});
