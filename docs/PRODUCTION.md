# Production et retour arrière — phase 4

Ce guide n’a pas été exécuté sur le serveur. Aucun conteneur ni déploiement n’a été lancé pendant la livraison. Effectuer d’abord les tests d’intégration de [TRACKING-PHASE4.md](TRACKING-PHASE4.md) sur une base de test dédiée.

## Préparer la version

Conserver le commit déployé, son JAR, son frontend et sa configuration avant de remplacer quoi que ce soit. Construire avec Java 17/Maven et Node 20 ou supérieur :

```sh
cd backend
mvn test package
cd ../frontend
npm ci
npm test
VITE_DEMO=false npm run build
```

Le JAR est `backend/target/backend-0.1.0.jar` ; les fichiers web sont dans `frontend/dist`. Vérifier le nom réel du JAR produit. Ne pas republier les anciens fichiers `target`/`node_modules` suivis historiquement par Git : reconstruire les artefacts. Le build production exclut le module de démonstration. La configuration Compose principale reste une configuration de **développement** (seed, MailHog, profil dev et démo frontend) : ne pas l’utiliser telle quelle en production.

Fournir au processus backend, par le gestionnaire de secrets/service existant :

- `SPRING_PROFILES_ACTIVE=prod` uniquement ; ni `dev`, ni OTP fixe.
- `JWT_SECRET` privé, aléatoire, au moins 32 octets ; les valeurs connues du dépôt sont refusées.
- `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` propres à la production.
- `PUBLIC_BASE_URL=https://votre-domaine` et `ALLOWED_ORIGINS=https://votre-domaine` ; pour plusieurs origines, liste séparée par des virgules, sans wildcard, chemin ou barre finale. Le même réglage protège le WebSocket.
- `PORT=3001` si le proxy fourni est utilisé : le profil prod seul choisit sinon 8080, alors que `frontend/nginx.conf` cible 3001.
- SMTP opérationnel : `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` selon le serveur ; `MAIL_FROM` via la propriété Spring `mail.from` si nécessaire. Les délais SMTP sont bornés à 5 secondes par opération.
- Répertoires persistants et accessibles au compte du service pour les uploads et les logs. Routage/tuiles selon l’infrastructure existante ; pas de nouvelle clé payante nécessaire. L’ETA peut être estimée si OSRM échoue.

Les propriétés du profil prod désactivent l’initialisation SQL automatique, les logs OTP et Swagger. Le garde de configuration refuse les origines HTTP et le cumul dev/prod. Les contrôleurs de démonstration ne sont enregistrés qu’avec `dev & !prod`.

## Migration

Prévoir une courte maintenance, arrêter les nouvelles réservations/départs, laisser finir ou annuler les trajets et arrêter l’ancien processus applicatif. Sauvegarder PostgreSQL avec les outils habituels (`pg_dump` vers un fichier protégé, secrets via `.pgpass` ou le gestionnaire existant), conserver les uploads et vérifier que la sauvegarde est restaurable.

Sur une base **déjà en phase 3**, appliquer avec `psql`, connecté à la bonne base :

```sh
psql -v ON_ERROR_STOP=1 -f backend/src/main/resources/sql/migrations/004-tracking-reliability.sql
```

Cette migration transactionnelle et rejouable ajoute les index de sélection et la file d’emails SOS ; elle ne transforme pas les séances existantes. Les index classiques peuvent prendre des verrous : les créer pendant la maintenance. L’index unique `uq_provider_en_route` de phase 3 reste la garantie d’un seul trajet actif par prestataire.

Pour une base antérieure à la phase 3 ou neuve, appliquer d’abord `backend/src/main/resources/sql/schema.sql` avec `ON_ERROR_STOP=1`, application arrêtée, après sauvegarde. Ne pas appliquer les seeds. Ce schéma convertit **une seule fois**, lorsque la colonne `started_at` n’existe pas, les anciens `ACCEPTED` en `IN_PROGRESS`. Vérifier cette hypothèse sur les anciennes données avant application ; ne pas supprimer cette colonne pour rejouer la migration. Contrôler les éventuels doublons EN_ROUTE avant création de l’index unique. Le schéma complet inclut déjà les ajouts de phase 4.

## Démarrer et vérifier

Démarrer le JAR construit avec le gestionnaire de service existant, l’environnement ci-dessus et la configuration HTTPS du serveur. Remplacer le frontend par le répertoire `dist` du même commit. Le proxy doit servir `/api`, `/uploads` et `/ws` vers le backend, conserver `Origin`, autoriser l’upgrade WebSocket et rediriger HTTP vers HTTPS. Le backend/Redis/PostgreSQL ne doivent pas être exposés directement au public.

Le quota public est de 30 requêtes/minute par **adresse vue par le backend**. Derrière un proxy, configurer la transmission de l’adresse réelle avec une liste explicite de proxies de confiance. Le Nginx fourni transmet `$remote_addr` en `X-Forwarded-For`. Si un autre proxy TLS le précède, configurer `set_real_ip_from` uniquement pour ce proxy et `real_ip_header` ; côté Tomcat, activer `server.forward-headers-strategy=native` et restreindre `server.tomcat.remoteip.internal-proxies` aux adresses effectives de confiance. Ne pas accepter un `X-Forwarded-For` arbitraire du client. Sans ce réglage, plusieurs utilisateurs derrière le même proxy partagent le quota.

Ne pas journaliser les URL `/t/*`, `/api/public/trips/*`, les en-têtes Authorization, les corps GPS ni les requêtes SQL avec paramètres. La configuration Nginx livrée désactive les logs des deux chemins de partage et utilise `Referrer-Policy: no-referrer` ; reprendre ces règles dans le proxy TLS/CDN éventuel. Aucun test `nginx -t` n’a été exécuté ici : valider la configuration sur le serveur avant rechargement.

Vérifications après démarrage : health 200 ; `/api/dev/login-as` absent ; frontend sans lien/module de démo ; WebSocket accepté depuis l’origine autorisée et refusé depuis une autre ; login, départ et retour à l’accueil en HTTPS ; arrivée qui efface la position ; partage révoqué ; SOS réel vers un contact de test et visibilité administrateur. Vérifier un navigateur mobile au premier plan, puis ses limites lors d’une mise en veille. Un résultat SMTP incertain impose une vérification humaine ; l’application ne contacte pas les secours.

## Retour arrière

Arrêter les nouveaux départs, terminer/annuler les trajets et arrêter le processus phase 4. Revenir au **backend et frontend ensemble** du commit phase 3 précédent (`b83090a`), avec `prod`, les mêmes secrets et `VITE_DEMO=false`. Les ajouts SQL de phase 4 sont compatibles avec cette version : laisser les tables/index en place, ne pas lancer de migration destructive.

Avant ce retour, traiter la file SOS : vérifier `PENDING`, `SENDING`, `FAILED` et `UNKNOWN` avec l’équipe, sans relancer aveuglément les messages incertains. L’ancien backend ne dépile pas cette file. Geler les envois ou prévenir directement les contacts concernés ; conserver les lignes pour le diagnostic. Le rollback réintroduit les limites de phase 3 (GPS attaché à la console, sélection sur 50 séances, pas de fermeture STOMP à l’expiration).

Ne pas revenir à une version antérieure à la phase 3 en conservant les nouveaux statuts : elle ne sait pas les lire. Un tel retour nécessite une restauration contrôlée de la sauvegarde correspondante et une décision explicite sur les données créées depuis. Ne jamais réinitialiser les volumes ou faire un `FLUSHALL` Redis pour un rollback.
