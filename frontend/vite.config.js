import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Écrit dist/version.json ({"commit": …}) seulement si GIT_SHA est fourni au build : `make up` le fait,
// un build de production sans GIT_SHA n'expose rien. Lu par scripts/check-served-version.sh.
const versionFile = () => ({
  name: 'mazlove-version-file',
  generateBundle() {
    const commit = process.env.GIT_SHA;
    if (!commit || commit === 'unknown') return;
    this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ commit }) + '\n' });
  },
});

// Les composants gardent l'extension .js (convention du projet) tout en contenant du JSX :
// par défaut Vite ne transforme le JSX que dans les .jsx, d'où les trois réglages "loader" ci-dessous.
export default defineConfig({
  plugins: [react({ include: /\.(js|jsx)$/ }), versionFile()],
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
