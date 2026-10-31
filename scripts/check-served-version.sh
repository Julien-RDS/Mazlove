#!/usr/bin/env bash
# Vérifie que la pile Docker en cours d'exécution sert bien le commit courant du dépôt.
#
#   make up                         # construit avec GIT_SHA=$(git rev-parse --short HEAD)
#   ./scripts/check-served-version.sh [URL_DU_FRONTEND]      # défaut : http://localhost:3000
#
# Pour chaque service (backend, frontend) : 1) le conteneur tourne-t-il sur l'image que porte
# actuellement son nom (sinon : ancienne image, `docker compose up -d --force-recreate <service>`) ;
# 2) l'étiquette de cette image correspond-elle au commit courant. Le frontend est en plus interrogé
# (version.json). Code de sortie 0 seulement si tout concorde. Ne modifie rien.
set -u
cd "$(dirname "$0")/.."
URL="${1:-http://localhost:3000}"
HEAD_SHA="$(git rev-parse --short HEAD 2>/dev/null || echo '')"
[ -n "$HEAD_SHA" ] || { echo "Dépôt Git introuvable." >&2; exit 2; }
COMPOSE="${COMPOSE:-docker compose}"
fail=0
ko() { echo "  ✗ $*"; fail=1; }
ok() { echo "  ✓ $*"; }
same() { [ -n "$1" ] && [ -n "$2" ] && { [[ "$1" == "$2"* ]] || [[ "$2" == "$1"* ]]; }; }

echo "Commit du dépôt : $HEAD_SHA"
[ -z "$(git status --porcelain)" ] || echo "  ! arbre de travail modifié : le commit ne décrit pas exactement ce qui a été construit."

for svc in backend frontend; do
  echo "Service $svc"
  cid="$($COMPOSE ps -q "$svc" 2>/dev/null | head -n1)"
  if [ -z "$cid" ]; then ko "aucun conteneur en cours d'exécution"; continue; fi
  name="$(docker inspect -f '{{.Config.Image}}' "$cid")"
  running="$(docker inspect -f '{{.Image}}' "$cid")"
  current="$(docker image inspect -f '{{.Id}}' "$name" 2>/dev/null)"
  label="$(docker image inspect -f '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "$name" 2>/dev/null)"
  if [ -z "$current" ]; then ko "image $name introuvable"
  elif [ "$running" != "$current" ]; then ko "le conteneur tourne sur une ANCIENNE image de $name : $COMPOSE up -d --force-recreate $svc"
  else ok "le conteneur utilise l'image actuelle de $name"; fi
  if [ -z "$label" ] || [ "$label" = "unknown" ] || [ "$label" = "<no value>" ]; then
    ko "image construite sans GIT_SHA : commit inconnu (utiliser make up)"
  elif same "$label" "$HEAD_SHA"; then ok "étiquette de l'image : $label"
  else ko "l'image correspond au commit $label, pas à $HEAD_SHA : reconstruire (make up)"; fi
done

echo "Frontend servi ($URL/version.json)"
body="$(curl -fsS -m 10 "$URL/version.json" 2>/dev/null || true)"
served="$(printf '%s' "$body" | sed -n 's/.*"commit"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
if [ -z "$served" ]; then ko "pas de version.json exploitable (frontend construit sans GIT_SHA, ou URL incorrecte)"
elif same "$served" "$HEAD_SHA"; then ok "le frontend sert le commit $served"
else ko "le frontend sert le commit $served, pas $HEAD_SHA"; fi

echo
if [ "$fail" -eq 0 ]; then echo "OK : la pile sert le commit $HEAD_SHA."; else echo "ÉCART : voir ci-dessus."; fi
exit "$fail"
