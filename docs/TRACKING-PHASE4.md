# Phase 4 — continuité et fiabilité du suivi

Livraison du 30 octobre 2026, à partir de `b83090a`. Aucun Docker, service de données ou déploiement lancé. [Production et retour arrière](PRODUCTION.md) · [Prompt pour la suite](NEXT-PROMPT.md).

Ce bilan décrit la livraison initiale. Les corrections GPS du 31 octobre, le comportement lors des pannes REST et les tests désormais exécutés avec PostgreSQL/Redis/STOMP sont détaillés dans [GPS-OPTIMIZATION.md](GPS-OPTIMIZATION.md).

## Comportement livré

`ProviderJourney`, au-dessus des écrans de l’application, retrouve le trajet prestataire via `/api/users/me/provider-trip`. Le GPS continue pendant les changements d’écran et reprend après rechargement d’un trajet EN_ROUTE, sous réserve de la permission du navigateur. L’arrivée, l’annulation, la déconnexion, le changement de compte et l’expiration STOMP libèrent l’émetteur, son watcher, son intervalle, son Wake Lock et sa connexion. Une panne de lecture de l’état serveur suspend le GPS avec un message ; la reprise suit le prochain rafraîchissement réussi.

Un verrou Web Locks par compte prestataire empêche deux onglets du même navigateur d’émettre simultanément. Le second propose une prise de relais explicite ; BroadcastChannel demande au premier de nettoyer son émetteur avant de rendre le verrou. Une fermeture/crash libère le verrou navigateur. Sans Web Locks, pas de GPS et une explication est affichée. `/demo` conserve ses deux jetons indépendants ; la simulation ne lance jamais le vrai GPS. Passer au GPS réel arrête le simulateur.

L’accueil appelle `/api/users/me/active-session`, protégé et limité à une ligne SQL : EN_ROUTE, ARRIVED, IN_PROGRESS, puis la réservation ACCEPTED/PENDING la plus proche dont la plage n’est pas expirée. Les deux rôles sont pris en compte, uniquement pour l’utilisateur authentifié. Deux index partiels soutiennent la sélection. Le trajet dont l’utilisateur est prestataire possède son propre endpoint pour ne pas être masqué par une séance où il serait client.

## Transactions et confidentialité

Les opérations GPS, arrivée, annulation, code de début, fin et changement de rendez-vous verrouillent la séance. Le chargement sous verrou rafraîchit une entité éventuellement déjà présente dans le contexte JPA. L’index unique partiel EN_ROUTE par prestataire reste en place. La simulation et son reset suivent aussi le verrou.

Les positions/événements sont publiés après commit ; une nouvelle transaction verrouille et revérifie EN_ROUTE avant publication live. Les notifications de statut ignorent les transitions dépassées, et le broker conserve l’ordre par connexion. Les liens sont révoqués dans la transaction de fin de trajet. Snapshots et vues publiques vérifient l’état sous verrou avant de lire Redis. Une ancienne position reçue en retard ne réapparaît pas après arrivée, ni après changement de compte.

Le registre WebSocket contrôle l’expiration à l’entrée et à la sortie ; il ferme les connexions expirées (code 1008) au plus tard au prochain passage du job de 1 seconde. La suppression de compte ferme ses sockets, retire sa disponibilité et ses clés de trajet ; les FK SQL suppriment séances, partages, alertes, contacts et file d’emails associés. La disponibilité prend aussi un verrou sur la fiche prestataire afin de ne pas la republier pendant sa suppression.

| Donnée | Durée / nettoyage |
| --- | --- |
| Dernière position | Redis 120 s, écriture + TTL atomiques ; effacement à l’arrivée/annulation/fin |
| Itinéraire de séance | 14 h maximum ; même effacement terminal |
| Compteur proximité | 60 s |
| Déviation / épisode | 300 s |
| Signal perdu | 14 h maximum ; supprimé au retour du signal |
| Limiteur GPS | 1 s |
| Cache itinéraires mutualisé | 10 min, sans identifiant de compte |
| Disponibilité | marqueur 90 s ; nettoyage GEO toutes les 30 s ; clé GEO globale expire après 120 s sans mise à jour |
| Quota partage public | 30/min/adresse vue par le backend ; compteur + TTL atomiques de 60 s ; HTTP 429 conservé |
| Événements SQL | purge horaire après 30 jours par défaut |
| Liens publics | révocation terminale ; purge horaire des liens expirés |
| Résultats d’emails SOS | purge horaire des lignes SENT/FAILED/UNKNOWN après 7 jours |

Les alertes SOS elles-mêmes restent conservées pour l’équipe et sont supprimées avec le compte. Les clés de cache et les messages d’exception ne sont plus inclus dans les logs concernés. Nginx masque les URL secrètes de partage et supprime les referrers ; appliquer les mêmes règles au proxy extérieur (voir PRODUCTION).

## SOS durable

L’alerte et les destinataires sont enregistrés dans la même transaction, avant SMTP. Un second appui pour la même alerte ouverte renvoie celle-ci ; une contrainte unique interdit deux livraisons au même destinataire pour cette alerte. Un worker réserve une ligne avec `FOR UPDATE SKIP LOCKED`, valide cette réservation puis appelle SMTP. Les jobs de suivi/expiration disposent d’un ordonnanceur distinct du heartbeat STOMP, avec quatre threads.

Après un échec certain avant acceptation (connexion refusée, hôte introuvable, authentification SMTP), il reste au maximum deux réessais, espacés de 30 puis 60 secondes. Une préparation invalide échoue immédiatement. Un timeout ou un résultat ambigu devient UNKNOWN, sans réessai automatique. Une réservation SENDING abandonnée après un crash devient UNKNOWN après 10 minutes. L’utilisateur voit les nombres envoyés, en attente, échoués et incertains ; l’administration signale les problèmes.

SMTP ne permet pas de garantir « exactement une fois » en cas de crash ou de réponse perdue après acceptation. Ces cas demandent une vérification humaine. Un email déjà en cours d’envoi ne peut pas être rappelé par une suppression de compte. Aucun appel ni SMS aux secours n’est effectué ; 17/112 restent explicitement indiqués.

## Vérifications exécutées

- `mvn test` : **70 comptabilisés, 66 exécutés et réussis, 4 tests d’intégration préexistants désactivés**, zéro échec/erreur. Compilation des nouvelles classes IT comprise.
- `npm test` : **21 tests Vitest réussis**.
- `npm run test:frontpage` avec Chromium système : **12 tests réussis**, API/STOMP/tuiles simulés. Ils couvrent l’accueil, le chargement différé, le départ avec géolocalisation émulée, la continuité console→accueil→console, le rechargement, le nettoyage à l’arrivée/annulation/expiration, le relais entre onglets, la déconnexion pendant une requête, le changement de compte l’isolation de la démo et le rejet d’une réponse de trajet retardée après l’arrivée.
- `VITE_DEMO=false npm run build` : réussi, aucun module `DemoSplitScreen` dans le résultat. Entrée JavaScript **233,06 kB / 74,34 kB gzip**, contre environ 221 / 70 kB au bilan précédent ; la gestion globale ajoute donc environ 12 kB bruts. MapLibre reste différé : **806,47 kB / 219,65 kB gzip**. L’avertissement Vite >500 kB concerne ce moteur. Aucun score Lighthouse annoncé.
- `git diff --check` : sans erreur.

Les dépendances frontend ont été utilisées dans une copie de travail dédiée, pour ne pas modifier les anciens `node_modules` suivis dans le dépôt. Maven a utilisé Java 17. Les artefacts générés ne font pas partie du commit.

## Vérifications non exécutées avec services

PostgreSQL, Redis, API et SMTP/MailHog locaux étaient inaccessibles. Les **11 tests IT** ont été sélectionnés explicitement sans variable d’activation : tous désactivés, aucun parcours réel exécuté. Le parcours Playwright réel n’a pas été lancé. Aucun test de configuration Nginx sur un serveur n’a été effectué.

Les suites fournies :

- `ActiveSessionIT` (3 cas) : >50 réservations, deux rôles, utilisateur tiers, priorité et exclusions. Lignes dédiées et rollback PostgreSQL.
- `StompSecurityIT` (2 cas) : CONNECT sans jeton, tiers, client émetteur refusé, réception autorisée, nettoyage et absence de nouveaux messages après arrêt, expiration pendant la connexion. Routage local déterministe.
- `TrackingReliabilityIT` (6 cas) : itinéraire GeoJSON versionné, filtres/précision/désordre/point aberrant, ETA, TTL, signal perdu/récupéré, arrivée sur deux pings, course GPS/arrivée, liens expirés/révoqués/quota, SOS durable et dédupliqué après refus SMTP, email dans MailHog et ADMIN, rendez-vous figé et unicité EN_ROUTE. Le cas MailHog demande un opt-in supplémentaire.
- `frontend/e2e/tracking.spec.js` : consentement, départ, navigation vers l’accueil, ETA décroissante, coupure/reconnexion, deux pings, code, séance et fin. PostgreSQL/Redis/backend/frontend requis. Les tests autonomes ne sont pas inclus dans cette commande.

À lancer uniquement sur une base jetable dédiée avec le seed fictif ; ces IT remettent la séance de démonstration à zéro et manipulent des alertes de test. Fournir les paramètres PostgreSQL/Redis/JWT via l’environnement sans les mettre dans les commandes ni les logs :

```sh
cd backend
MAZLOVE_STOMP_IT=true mvn -Dtest=StompSecurityIT,ActiveSessionIT,TrackingReliabilityIT test
# Ajouter le cas de réception SMTP lorsque MailHog est déjà accessible :
MAZLOVE_STOMP_IT=true MAZLOVE_MAIL_IT=true MAILHOG_URL=http://localhost:8025 mvn -Dtest=TrackingReliabilityIT test
cd ../frontend
BASE_URL=http://localhost:3000 npm run test:e2e
```

Les tests HTTP/STOMP ouvrent leur propre port applicatif éphémère, sans démarrer de base ni de conteneur. Le parcours navigateur réel attend les applications existantes. Pour ce dernier, utiliser uniquement l’environnement de test avec les comptes fictifs, le profil dev et le seed ; ces réglages ne doivent jamais être copiés en production.

## Limites restantes

Le navigateur ne garantit ni GPS ni Wake Lock en arrière-plan ou écran verrouillé ; HTTPS et les permissions restent requis. Le verrou est limité aux onglets d’une même origine et d’un même navigateur, pas aux appareils distincts. En cas de coupure réseau, l’arrêt serveur bloque l’ingestion et l’affichage ; l’interface se recale à la reconnexion ou au prochain snapshot. Les changements Redis ne participent pas à la transaction PostgreSQL : l’état SQL bloque les lectures après arrêt et les TTL bornent les résidus éventuels après incident.

Le broker simple et les connexions en mémoire visent une instance. Les services publics de routage/tuiles restent sans SLA. Il reste à exécuter les suites réelles, compléter la validation de détour durable et des courses d’annulation, puis vérifier les mobiles, le proxy HTTPS/adresse réelle, la migration et le rollback sur une copie de données. Le code et les tests livrés ne constituent pas une validation de production.
