# Prompt Phase 4 — Mazlove : fiabiliser le suivi avant usage réel

## 0. Comment travailler

Tu interviens sur le dépôt `Julien-RDS/Mazlove`, à partir du dernier `master`. Lis le code et les documents `docs/TRACKING.md`, `docs/TRACKING-DELIVERY.md` et `docs/FRONTPAGE-TRACKING.md` avant de modifier les fichiers concernés.

L’accueil intègre désormais le suivi : séance active, ETA, accès à la console, carte à la demande, disponibilités approximatives et accès à la démonstration. React/Vite et Spring Boot 3.1 sont déjà en place. Préserve la direction visuelle actuelle et les parcours existants.

Règles :
- Ne lance pas Docker et ne déploie rien. Utilise les services déjà disponibles pour les tests d’intégration ; s’ils manquent, livre les tests et précise lesquels n’ont pas été exécutés.
- Avance par étapes, sans audit général ni installation hors sujet. Donne une courte mise à jour à chaque étape importante.
- Aucun service payant ni nouvelle clé d’API. Tous les textes utilisateur sont en français.
- Adapte-toi aux classes et aux versions réelles. Ne remplace pas toute l’architecture.
- Les données GPS restent réservées aux personnes autorisées ; aucune coordonnée ni aucun jeton dans les logs.
- Les modifications attendues doivent être livrées dans le code, avec des tests utiles et une documentation concise. N’annonce pas un parcours validé si tu ne l’as pas exécuté.
- Termine par un commit et un push sur `master`, sans forcer l’historique. Signale le commit et les limites restantes.

## 1. Continuité du GPS dans l’application — priorité haute

Aujourd’hui `useProviderBroadcaster` est monté dans `ProviderConsole`. Vérifie ce qui se passe lorsque le prestataire appuie sur « Je pars », puis revient à l’accueil ou ouvre une autre page : quitter la console ne doit pas arrêter silencieusement le suivi d’une séance encore `EN_ROUTE`.

Implémente une gestion du suivi au niveau approprié de l’application :
- Une seule session suivie et un seul émetteur GPS actif par prestataire.
- Arrêt immédiat à l’arrivée, à l’annulation et à la déconnexion.
- Reprise après rechargement à partir de l’état serveur ; ne pas redemander un consentement encore valide, mais respecter les permissions du navigateur.
- Empêcher les doubles émissions entre onglets, ou afficher une procédure explicite de prise de relais.
- Garder les deux comptes de `/demo` indépendants. La simulation ne doit pas déclencher le vrai GPS.
- Conserver le Wake Lock et expliquer clairement les limites du navigateur en arrière-plan.

Critère : dans un navigateur de test avec géolocalisation émulée, naviguer de la console vers l’accueil puis revenir conserve les mises à jour ; terminer le trajet libère tous les watchers, timers et connexions associés.

## 2. Une séance active fiable sur l’accueil

`useHomeSession` choisit actuellement une séance parmi les 50 résultats de `/api/users/me/sessions`. Une séance active peut être absente de cette page.

Ajoute un endpoint dédié, protégé, qui sélectionne côté serveur la séance pertinente : `EN_ROUTE`, puis `ARRIVED`, `IN_PROGRESS`, puis la prochaine réservation acceptée/en attente. Exclure les réservations anciennes non actives et ne jamais renvoyer celles d’un autre utilisateur. Utiliser une requête bornée et des index si nécessaires, sans charger tout l’historique.

Branche l’accueil dessus. Conserve les états chargement, vide, erreur et le lien vers les séances. Teste plus de 50 réservations, les deux rôles, les changements d’état et la déconnexion pendant une requête.

## 3. Tester réellement la chaîne de suivi

Les tests de l’accueil utilisent une API et des WebSockets simulés. Ils ne valident pas le backend, Redis, PostgreSQL ou l’envoi d’emails.

Exécute et complète, lorsque les services sont accessibles :
- `StompSecurityIT` : CONNECT sans jeton, abonnement d’un tiers, envoi par le client refusé, réception des positions par le participant autorisé.
- Le parcours Playwright : consentement → départ → diminution de l’ETA → arrivée après deux pings → code → séance en cours → fin.
- Coupure/reconnexion, positions désordonnées, précision insuffisante, point aberrant, détour, perte puis retour du signal.
- Partage expiré/révoqué, position effacée après arrivée, absence de nouveaux messages live après arrêt.
- SOS, contrôle du rôle ADMIN, réception d’email dans MailHog et retour explicite lorsqu’un email échoue.

Les tests doivent être déterministes et utiliser les fixtures d’itinéraire versionnées. Réparer les défauts trouvés avant de déclarer le parcours prêt.

## 4. Concurrence, confidentialité et erreurs

Revoir les chemins concernés par les tests, en particulier les courses entre ingestion GPS, arrivée, annulation et modification du point de rendez-vous. Vérifier les transactions, les verrous et la publication après commit. Confirmer l’unicité d’une session `EN_ROUTE` par prestataire.

Contrôler l’expiration de toutes les clés Redis, la purge des événements, la suppression de compte et la limitation des requêtes publiques. Vérifier les jetons expirés pendant une connexion STOMP et les changements d’utilisateur côté navigateur : aucune ancienne position ne doit réapparaître.

L’alerte SOS doit rester enregistrée si l’email échoue. Proposer puis implémenter une reprise bornée et testée si l’absence de réessai présente un risque opérationnel ; éviter les envois dupliqués. Ne jamais suggérer que les secours sont contactés automatiquement.

## 5. Performance et préparation de production

Préserver le chargement différé de MapLibre, l’absence de carte sur l’accueil tant qu’elle n’est pas ouverte, les appels sans chevauchement et leur pause dans les onglets masqués. Mesurer les changements sur le build ; ne pas inventer de score Lighthouse.

Vérifier le profil `prod`, l’absence des endpoints de démonstration, `VITE_DEMO=false`, les origines WebSocket, HTTPS et la migration des anciennes séances. Documenter un démarrage de production et un retour arrière réalistes. Ne pas activer le développement par inadvertance dans les instructions destinées à la production.

## 6. Validation et livrables

- `cd frontend && npm test && npm run build`.
- `cd frontend && npm run test:frontpage` : tests navigateur autonomes, avec Chromium installé, sans backend ni Docker.
- `cd backend && mvn test`.
- `MAZLOVE_STOMP_IT=true mvn -Dtest=StompSecurityIT test` et les tests E2E complets uniquement avec les services nécessaires.
- Code, migrations éventuelles, tests et documentation à jour.
- Bilan final : changements, résultats exacts, tests non exécutés, risques restants et commit poussé.

Ne commence pas Stripe, une application native, un nouveau design complet ou une infrastructure multi-instance dans cette phase. La priorité est un suivi fiable, sécurisé et vérifié de bout en bout.
