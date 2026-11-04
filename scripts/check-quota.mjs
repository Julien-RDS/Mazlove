#!/usr/bin/env node
// Contrôle du quota du partage public : 30 requêtes par minute et par ADRESSE VUE PAR LE BACKEND
// (GET /api/public/trips/{jeton}, TripSafetyService). Sans dépendance, Node 18 ou plus.
//
//   node scripts/check-quota.mjs https://HOST            # deux contrôles, environ 2 minutes
//   node scripts/check-quota.mjs https://HOST --probe    # UNE requête (à lancer depuis une SECONDE adresse)
//   node scripts/check-quota.mjs --selftest              # vérifie la logique du script sur des serveurs simulés
//   options : --test fixed|spoof   un seul contrôle ;  --from <ip locale>   adresse source ;  --no-wait
//
// À utiliser sur la préproduction : chaque contrôle consomme le quota de l'adresse testeuse pendant 60 s.
// Les jetons sont aléatoires et inexistants (réponse attendue : 404, puis 429 au-delà de 30).
import http from 'node:http';
import https from 'node:https';
import { randomBytes } from 'node:crypto';
import { setTimeout as sleep } from 'node:timers/promises';

export const LIMIT = 30;
const TOTAL = 35;
const WINDOW_MS = 61_000;

function hit(base, { headers = {}, from } = {}) {
  const url = new URL(`/api/public/trips/quota-${randomBytes(8).toString('hex')}`, base);
  const lib = url.protocol === 'https:' ? https : http;
  return new Promise((resolve) => {
    const req = lib.request(url, { method: 'GET', headers, localAddress: from, timeout: 10_000, agent: false }, (res) => {
      res.resume();
      res.on('end', () => resolve(res.statusCode));
    });
    req.on('timeout', () => req.destroy(new Error('délai dépassé')));
    req.on('error', (e) => resolve(`ERR(${e.code || e.message})`));
    req.end();
  });
}

async function series(base, makeHeaders, from) {
  const codes = [];
  for (let i = 0; i < TOTAL; i += 1) codes.push(await hit(base, { headers: makeHeaders(i), from }));
  const first429 = codes.indexOf(429) + 1; // 0 = aucun
  const odd = codes.filter((c) => c !== 404 && c !== 429);
  return { codes, first429, odd };
}

const oddNote = (odd) => (odd.length ? ` Réponses inattendues : ${[...new Set(odd)].join(', ')} (le chemin ou le proxy sont-ils les bons ?).` : '');

export async function checkFixed(base, from) {
  const { first429, odd } = await series(base, () => ({}), from);
  if (odd.length === TOTAL) return { ok: false, text: `Aucune réponse exploitable.${oddNote(odd)}` };
  if (first429 === LIMIT + 1) return { ok: true, text: `OK : 429 dès la ${first429}ᵉ requête (limite ${LIMIT}).${oddNote(odd)}` };
  if (first429 === 0) return { ok: false, text: `ÉCHEC : aucun 429 en ${TOTAL} requêtes. Quota inactif ou compteur contourné.${oddNote(odd)}` };
  if (first429 <= LIMIT)
    return { ok: false, text: `SUSPECT : 429 dès la ${first429}ᵉ requête. Le quota était déjà entamé : autre trafic de la même adresse, ou compteur PARTAGÉ avec d'autres visiteurs. Relancer après 60 s sur un réseau calme ; si cela se répète, voir --probe.${oddNote(odd)}` };
  return { ok: false, text: `ANOMALIE : premier 429 à la ${first429}ᵉ requête (attendu : ${LIMIT + 1}). Certaines requêtes n'atteignent pas le compteur (cache, CDN ?).${oddNote(odd)}` };
}

export async function checkSpoof(base, from) {
  const { first429, odd } = await series(base, (i) => ({ 'X-Forwarded-For': `203.0.113.${(i % 250) + 1}` }), from);
  if (first429 === 0)
    return { ok: false, text: `CONTOURNABLE : ${TOTAL} requêtes à adresses X-Forwarded-For différentes, aucun 429. Le quota suit l'en-tête fourni par le client : ne PAS faire confiance à ce proxy tel quel.${oddNote(odd)}` };
  return { ok: true, text: `OK : l'en-tête X-Forwarded-For fourni par le client ne change pas le compteur (429 à la ${first429}ᵉ). Ne prouve PAS que le backend voit la vraie adresse : voir --probe.${oddNote(odd)}` };
}

export async function probe(base, from) {
  const code = await hit(base, { from });
  if (code === 429) return { ok: false, text: '429 : quota déjà atteint pour cette adresse. Si une AUTRE adresse publique vient d\'épuiser le quota, le compteur est PARTAGÉ (le backend voit l\'adresse du proxy).' };
  if (code === 404) return { ok: true, text: '404 : quota non atteint pour cette adresse. Si une autre adresse vient d\'épuiser le sien, les compteurs sont bien distincts.' };
  return { ok: false, text: `Réponse inattendue : ${code}.` };
}

async function selftest() {
  // Serveurs simulés : « bon » (compteur par adresse source), « contournable » (suit X-Forwarded-For),
  // « partagé » (un seul compteur pour tous, comme si le backend ne voyait que l'adresse du proxy).
  const start = (mode) =>
    new Promise((resolve) => {
      const counts = new Map();
      const srv = http.createServer((req, res) => {
        const key = mode === 'partagé' ? 'proxy' : mode === 'contournable' ? req.headers['x-forwarded-for'] || req.socket.remoteAddress : req.socket.remoteAddress;
        const n = (counts.get(key) || 0) + 1;
        counts.set(key, n);
        res.statusCode = n > 30 ? 429 : 404; // seuil réel du backend (TripSafetyService), volontairement indépendant de LIMIT
        res.end('{}');
      });
      srv.listen(0, '127.0.0.1', () => resolve({ base: `http://127.0.0.1:${srv.address().port}`, close: () => srv.close() }));
    });
  const cases = [];
  const expect = (label, got, want) => cases.push({ label, ok: got === want, got, want });
  for (const [mode, fixed, spoof] of [['bon', true, true], ['contournable', true, false], ['partagé', true, true]]) {
    let s = await start(mode); expect(`${mode} / quota à adresse fixe`, (await checkFixed(s.base)).ok, fixed); s.close();
    s = await start(mode); expect(`${mode} / X-Forwarded-For aléatoires`, (await checkSpoof(s.base)).ok, spoof); s.close();
  }
  // Deux « machines » : 127.0.0.1 épuise le quota, 127.0.0.2 envoie une seule requête.
  for (const [mode, expected] of [['bon', true], ['partagé', false]]) {
    const s = await start(mode);
    await series(s.base, () => ({}), '127.0.0.1');
    expect(`${mode} / seconde adresse après épuisement`, (await probe(s.base, '127.0.0.2')).ok, expected);
    s.close();
  }
  { const s = await start('bon'); await series(s.base, () => ({}), '127.0.0.1'); expect('bon / même adresse après épuisement', (await probe(s.base, '127.0.0.1')).ok, false); s.close(); }
  for (const c of cases) console.log(`${c.ok ? '✓' : '✗'} ${c.label}${c.ok ? '' : ` (obtenu ${c.got}, attendu ${c.want})`}`);
  const bad = cases.filter((c) => !c.ok).length;
  console.log(bad ? `\n${bad} cas en échec.` : `\nAuto-test réussi (${cases.length} cas) : la logique du script est cohérente avec les trois situations simulées.`);
  process.exit(bad ? 1 : 0);
}

async function main() {
  const args = process.argv.slice(2);
  if (args.includes('--selftest')) return selftest();
  const base = args.find((a) => /^https?:\/\//.test(a));
  const opt = (n) => (args.includes(n) ? args[args.indexOf(n) + 1] : undefined);
  if (!base) { console.error('Usage : node scripts/check-quota.mjs https://HOST [--probe] [--test fixed|spoof] [--from IP] [--no-wait]'); process.exit(2); }
  const from = opt('--from');
  if (args.includes('--probe')) { const r = await probe(base, from); console.log(r.text); process.exit(r.ok ? 0 : 1); }
  const only = opt('--test');
  const plan = [['fixed', 'Quota à adresse fixe', checkFixed], ['spoof', 'X-Forwarded-For aléatoires', checkSpoof]].filter(([id]) => !only || id === only);
  let failed = false;
  for (const [i, [, label, fn]] of plan.entries()) {
    if (i > 0 && !args.includes('--no-wait')) { console.log(`(attente de ${WINDOW_MS / 1000} s : fenêtre du quota)`); await sleep(WINDOW_MS); }
    const r = await fn(base, from);
    console.log(`${label} : ${r.text}`);
    failed ||= !r.ok;
  }
  process.exit(failed ? 1 : 0);
}

if (import.meta.url === `file://${process.argv[1]}`) main();
