# Validation sur applications, appareils et préproduction — état au 31 octobre 2026

Ce document prépare les contrôles qui exigent des moyens réels : Docker, PostgreSQL, Redis, MailHog, téléphones, préproduction. **Aucun n'a été exécuté par la livraison qui l'accompagne** : elle s'est déroulée dans un environnement sans Docker, sans accès à Maven Central (donc ni `mvn test` ni tests d'intégration), sans service de données et sans appareil. Les résultats ci-dessous sont des cases à remplir, pas des résultats.

| Point | Statut | Ce qui existe |
| --- | --- | --- |
| 1. Version réellement servie | Outil livré, **non exécuté sur Docker** | `make version` / `scripts/check-served-version.sh` (logique vérifiée contre un faux `docker`, six scénarios) |
| 2. Parcours navigateur réel | À exécuter ; **lacunes de couverture identifiées** | `frontend/e2e/tracking.spec.js` (un seul scénario) |
| 3. Safari/iOS, Chrome/Android | À exécuter | Matrice et mesure de cadence ci-dessous |
| 4. SOS avec MailHog | À exécuter | Scénarios et requêtes SQL ci-dessous |
| 5. Préproduction | À exécuter ; **un contrôle prioritaire** (adresse client) | Liste de contrôles et commandes ci-dessous |

Règles communes : base de test **dédiée** avec le seed fictif et des comptes fictifs, jamais de données utilisateur ; ne pas réinitialiser de volumes ni exécuter `FLUSHALL` ; profil `dev` et réglages de démonstration uniquement en test, jamais en production ; fournir les paramètres PostgreSQL, Redis et JWT par l'environnement, sans les écrire dans les commandes ni dans les journaux.

## 1. Version réellement servie

Les conteneurs existants peuvent encore tourner sur une ancienne image. Les images portent désormais l'étiquette `org.opencontainers.image.revision` et le frontend publie `/version.json`, **seulement** quand `GIT_SHA` est fourni au build (un build de production sans `GIT_SHA` n'expose rien).

```sh
git pull && make up            # make exporte GIT_SHA=$(git rev-parse --short HEAD) et construit
make version                   # ou : ./scripts/check-served-version.sh http://localhost:3000
```

Sans `make` : `GIT_SHA=$(git rev-parse --short HEAD) docker compose --profile dev up -d --build`. Le script compare, pour le backend et le frontend, l'image utilisée par le conteneur à l'image actuelle de son nom (sinon : `docker compose up -d --force-recreate <service>`), puis l'étiquette au commit courant, puis `version.json` au commit courant. Sortie 0 seulement si tout concorde.

Limites : un arbre de travail modifié est signalé (le commit ne décrit plus exactement le build) ; l'étiquette prouve le commit passé au build, pas le contenu réel de l'image ; le script n'a pas été lancé contre un vrai Docker.

## 2. Parcours navigateur réel et lacunes de couverture

```sh
cd backend
MAZLOVE_STOMP_IT=true mvn -Dtest=StompSecurityIT,ActiveSessionIT,TrackingReliabilityIT test
MAZLOVE_STOMP_IT=true MAZLOVE_MAIL_IT=true MAILHOG_URL=http://localhost:8025 mvn -Dtest=TrackingReliabilityIT test
cd ../frontend && BASE_URL=http://localhost:3000 npm run test:e2e
```

Conditions : base jetable avec le seed fictif (ces tests remettent la séance de démonstration à zéro), applications reconstruites à partir du commit (point 1).

Ce que la lecture des tests existants montre, à confirmer en les exécutant :

| Besoin | Couverture actuelle | À écrire avec les services de test |
| --- | --- | --- |
| Détour durable | `TrackingServiceTest.deviationOnlyOncePerEpisode` (unitaire) | IT sur Redis réel : positions à plus de `TRACKING_DEVIATION_THRESHOLD_M` (400) pendant plus de `TRACKING_DEVIATION_DURATION_S` (60) → **une seule** alerte pour l'épisode ; retour sous le seuil puis nouveau détour → nouvel épisode ; expiration de la clé d'épisode (300 s) |
| Perte et retour du signal | `TrackingReliabilityIT` (signal perdu/récupéré) | Déjà couvert côté serveur ; côté interface voir la matrice du point 3 |
| Annulation concurrente | `SessionCancellationTest` (unitaire, un seul cas) | IT : annulation lancée en même temps qu'une position GPS, qu'une arrivée (deux pings) et qu'un code de début → état final unique, position effacée, liens révoqués, aucune publication live après l'annulation |
| Révocation des liens | `TrackingReliabilityIT` (expiration, révocation, quota) et `TrackingJobsTest` | Parcours navigateur réel : « Révoquer les liens » puis `GET /api/public/trips/{jeton}` → 404 ; ajouter à `tracking.spec.js` qui n'a aucun scénario de révocation |

`tracking.spec.js` ne contient qu'un scénario (consentement, GPS émulé, arrivée, code, fin). Il ne couvre ni la révocation, ni l'annulation, ni le SOS, ni le détour. Je n'ai pas écrit ces tests sans pouvoir les exécuter : un test d'intégration jamais lancé n'apporte aucune preuve.

## 3. Appareils réels : Safari/iOS et Chrome/Android

À faire sur au moins un iPhone (Safari) et un Android (Chrome), en HTTPS (la géolocalisation l'exige hors `localhost`), avec un compte prestataire fictif et un trajet EN_ROUTE. Noter pour chaque appareil : modèle, version du système, version du navigateur.

Messages attendus (ceux du code, `ProviderJourney`, `useProviderBroadcaster`, `providerLease`) :

| Scénario | Résultat attendu | Observé (à remplir) |
| --- | --- | --- |
| Première permission | Invite du navigateur ; « Recherche de votre position… » puis « GPS actif » après la première position émise | |
| Refus | « Autorisez la localisation dans les réglages du navigateur, puis reprenez le suivi. » et bouton « Réessayer la localisation » | |
| Réactivation après refus | Après réglage et clic, retour à « GPS actif » sans recharger | |
| Signal faible (intérieur) | « Signal GPS faible » ou « GPS faible ou position ancienne : recherchez un endroit dégagé. » ; retour à « GPS actif » en extérieur | |
| Rechargement de la page | Reprise du trajet EN_ROUTE sans nouvelle séance ; GPS repris si la permission est mémorisée | |
| Deux onglets (Web Locks) | Le second affiche « GPS actif dans un autre onglet » et « Reprendre le suivi dans cet onglet » ; le premier « Suivi transféré vers un autre onglet » | |
| Navigateur sans Web Locks | Échec **explicite** : « Ce navigateur ne permet pas de sécuriser le partage entre onglets. Utilisez un navigateur récent en HTTPS. » Ne pas introduire de verrou `localStorage` (course possible) | |
| Wake Lock | Écran allumé au premier plan pendant 5 minutes ; noter si `'wakeLock' in navigator` est vrai | |
| Arrière-plan 60 s, retour | « Gardez l'application au premier plan pour maintenir le GPS. » puis « Connexion interrompue. Le suivi reprendra avec une position récente. » ; reprise **sans** rejouer d'historique | |
| Écran verrouillé 60 s, retour | Même résultat que l'arrière-plan | |
| Mode économie d'énergie | Noter le comportement (pas d'attente particulière) | |
| HTTP hors `localhost` | « La géolocalisation nécessite HTTPS et un navigateur compatible. » | |

Le navigateur ne garantit **ni GPS ni Wake Lock en arrière-plan ou écran verrouillé** : ces lignes documentent un comportement, elles ne valident pas une garantie.

### Mesurer la cadence

Valeurs attendues d'après le code (`providerGps.js`) : au plus un envoi toutes les 3 s en déplacement, toutes les 10 s à l'arrêt ; si le navigateur ne rappelle pas le watcher, une position fraîche est demandée après 10 s. Côté REST : recherche du trajet prestataire toutes les 30 s, rafraîchissement immédiat après une action locale et au retour au premier plan, une seule requête à la fois ; snapshot partagé toutes les 30 s par séance observée.

- **Émission (Chrome/Android)** : `chrome://inspect` sur un poste, onglet Network, requête WebSocket `/ws`, panneau Messages : filtrer les trames `SEND` vers `/app/tracking/{id}/position` et relever les horodatages.
- **Réception (tout appareil, dont iPhone)** : ouvrir le suivi du même trajet dans Chrome sur un poste de bureau et relever les trames reçues sur `/topic/tracking.{id}`. Cela mesure les positions **acceptées** par le serveur (après limiteur de 1 s et filtres), donc un peu moins que l'émission brute. Utile pour iOS, dont l'inspecteur ne permet pas d'exporter facilement les trames.
- **Requêtes REST** : onglet Network, filtre `provider-trip`, `snapshot`, `active-session`, sur 5 minutes au premier plan puis après retour d'arrière-plan : compter, et vérifier l'absence de rafale ou de requêtes concurrentes.

| Appareil et navigateur | Phase | Durée | Envois ou positions | Médiane des intervalles | Maximum | Requêtes REST |
| --- | --- | --- | --- | --- | --- | --- |
| | Déplacement | | | | | |
| | Arrêt | | | | | |
| | Retour d'arrière-plan | | | | | |

**Aucun gain de batterie n'est annoncé ni mesuré.** Une comparaison de consommation exigerait des conditions identiques et répétées sur plusieurs appareils ; sans elle, ne rien publier.

## 4. SOS avec MailHog

Rappel du comportement documenté (`TRACKING-PHASE4.md`) : alerte et destinataires enregistrés avant SMTP ; un second appui sur la même alerte ouverte renvoie la même alerte ; contrainte `UNIQUE(alert_id, recipient)` ; après un refus certain, au plus deux réessais (30 s puis 60 s), soit **trois tentatives** ; un délai dépassé ou un résultat ambigu devient `UNKNOWN` **sans réessai** ; une réservation `SENDING` abandonnée après un crash devient `UNKNOWN` après 10 minutes. SMTP ne permet pas de garantir « exactement une fois » : un résultat incertain demande une vérification humaine, jamais une relance automatique.

Suivre la file sans jamais afficher `body` (il contient le lien du partage et des données personnelles) :

```sql
SELECT id, alert_id, recipient, status, attempts, next_attempt_at, updated_at
FROM sos_mail_queue ORDER BY id DESC LIMIT 20;
SELECT status, count(*) FROM sos_mail_queue GROUP BY status;
```

| Scénario | Mise en place | Attendu |
| --- | --- | --- |
| Envoi nominal | MailHog démarré (`http://localhost:8025`), un client avec contact de confiance, SOS tenu 1,5 s | Un message par contact et pour l'équipe dans MailHog ; lignes `SENT` ; l'interface indique les nombres envoyés, en attente, échoués, incertains |
| Déduplication | Second appui sur le même SOS | Même alerte ; une seule ligne par destinataire ; aucun doublon dans MailHog |
| Refus certain | Arrêter MailHog (connexion refusée) puis SOS | `attempts` 1, 2, puis 3 ; `PENDING` aux deux premières avec `next_attempt_at` environ +30 s puis +60 s ; `FAILED` à la troisième ; échec visible côté utilisateur et administrateur |
| Résultat incertain | Remplacer le serveur SMTP par une socket qui accepte sans répondre (délai SMTP borné à 5 s) | `UNKNOWN`, `attempts` reste à 1, **aucun réessai** ; visible comme « incertain » |
| Interruption du worker | Avec la socket muette, arrêter brutalement le backend pendant `SENDING`, le relancer | Les lignes `PENDING` reprennent ; la ligne `SENDING` devient `UNKNOWN` au bout de 10 minutes ; aucune relance automatique des incertains |
| Test d'intégration | `MAZLOVE_STOMP_IT=true MAZLOVE_MAIL_IT=true MAILHOG_URL=http://localhost:8025 mvn -Dtest=TrackingReliabilityIT test` | Cas MailHog réussi |

Aucun appel ni SMS aux secours n'est effectué ; 17 et 112 restent affichés. Un message déjà en cours d'envoi ne peut pas être rappelé par une suppression de compte.

## 5. Préproduction

Contrôles à faire sur la préproduction existante ; remplacer `HOST` et l'origine autorisée.

**Priorité : adresse cliente derrière le proxy.** Le quota du partage public (30 requêtes par minute) est calculé sur `request.getRemoteAddr()` (`TripSafetyController`). Le dépôt ne contient aucun réglage `server.forward-headers-strategy` ni `server.tomcat.remoteip.internal-proxies` (recherche faite dans les `.properties`, `.yml`, `.conf`, Dockerfile et documents) : le Nginx fourni envoie `X-Forwarded-For`, mais rien dans le dépôt n'amène le backend à le lire. Si la préproduction ne les définit pas non plus (variables d'environnement `SERVER_FORWARD_HEADERS_STRATEGY` et `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES`, ou propriétés), **tous les visiteurs partagent un seul quota** et une seule personne peut l'épuiser pour tous. C'est une lecture du code, pas une observation à l'exécution ; le réglage est décrit dans `PRODUCTION.md`, la valeur de `internal-proxies` doit se limiter aux adresses réelles du proxy.

1. **HTTPS et redirection.** `curl -sSI https://HOST/` sans `-k` : 200, certificat valide. `curl -sSI http://HOST/` : 301 ou 308 vers HTTPS.
2. **En-têtes de confidentialité.** `curl -sSI https://HOST/ | grep -i referrer-policy` et idem pour `/t/exemple` : `no-referrer`.
3. **Absence de `/api/dev/*`.** `curl -s -o /dev/null -w '%{http_code}\n' https://HOST/api/dev/login-as` : attendu **404**. Un 401 ou 403 prouve que le chemin est protégé, pas qu'il est absent.
4. **Origines WebSocket.** Vérifié contre un serveur local :
   ```sh
   cd frontend && node -e "const WebSocket=require('ws');for(const o of ['https://HOST','https://autre.example']){const w=new WebSocket('wss://HOST/ws',{headers:{Origin:o}});w.on('open',()=>{console.log(o,'→ ouvert');w.close()});w.on('unexpected-response',(q,r)=>{console.log(o,'→ HTTP',r.statusCode)});w.on('error',e=>console.log(o,'→ erreur',e.message))}"
   ```
   Attendu : l'origine autorisée « ouvert », l'autre « HTTP 403 ».
5. **Expiration du JWT en connexion ouverte.** Se connecter en STOMP avec un jeton proche de son expiration (durée courte en préproduction) : la socket doit être fermée avec le code 1008 au plus tard au prochain passage du job d'une seconde après `exp`. Relever l'écart entre `exp` et la fermeture.
6. **Quota public et adresse cliente.** Depuis une première adresse : `for i in $(seq 1 35); do curl -s -o /dev/null -w '%{http_code} ' https://HOST/api/public/trips/jeton-inexistant; done` : 404 puis **429** à partir de la 31ᵉ. Aussitôt depuis une **seconde adresse publique** (autre réseau) : une seule requête doit répondre 404, pas 429 ; un 429 immédiat prouve que le backend voit l'adresse du proxy (quota partagé). Répéter la première série avec un `X-Forwarded-For` aléatoire par requête : le 429 doit toujours apparaître (le Nginx fourni remplace l'en-tête par `$remote_addr`).
7. **Journaux sans jeton ni position.** Compter sans afficher les lignes : `grep -cE '/t/[A-Za-z0-9_-]{16,}|/api/public/trips/[A-Za-z0-9_-]{8,}|Bearer |Authorization|eyJ[A-Za-z0-9_-]{10,}\.' <journaux nginx et backend>` : attendu 0 ; contrôler aussi l'absence de corps GPS (`"lat"`, `"lng"`) dans les journaux applicatifs.
8. **Migration et retour arrière sur une copie.** Copier la base (`pg_dump` vers un fichier protégé, restauration dans une base `…_copie`), appliquer `psql -v ON_ERROR_STOP=1 -f backend/src/main/resources/sql/migrations/004-tracking-reliability.sql`, démarrer le backend du commit courant sur la copie, puis démarrer le backend et le frontend **ensemble** du commit précédent (`b83090a`) sur la même copie : il doit démarrer, les ajouts SQL étant compatibles. Traiter la file SOS avant ce retour (`PENDING`, `SENDING`, `FAILED`, `UNKNOWN`) sans relancer les incertains.

La configuration Compose de développement n'est pas une configuration de production. Ces contrôles ne valident pas la production.
