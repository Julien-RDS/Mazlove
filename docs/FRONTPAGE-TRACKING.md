# Suivi intégré à l’accueil

L’accueil présente le suivi après son introduction : explication du parcours pour les visiteurs, séance prioritaire pour les personnes connectées, ETA, accès au détail ou à la console du prestataire. La carte du trajet s’ouvre à la demande. Les disponibilités reprennent le centre de la recherche, affichent un état vide/erreur explicite et permettent d’ouvrir les profils depuis les marqueurs. La mise en ligne exige une action du prestataire, sans demande GPS au chargement de l’accueil.

La démonstration reste visible uniquement avec `VITE_DEMO=true`. Les favoris conservent leur propre affichage.

## Optimisations mesurées

Avant : fichier JavaScript principal d’environ 1 077 Ko, 305 Ko gzip. Après : environ 221 Ko, 70 Ko gzip (build Vite avec démo). Cette comparaison porte sur le fichier principal, pas sur un score de performance ou le poids total téléchargé pendant une visite complète.

MapLibre est dans un fichier séparé d’environ 807 Ko chargé à l’ouverture d’une carte. Les écrans de réservation, connexion, séances et administration sont également chargés à la demande. Le build conserve un avertissement de taille pour le moteur de carte différé.

Les rafraîchissements REST n’empiètent plus les uns sur les autres, se mettent en pause quand l’onglet est masqué et sont annulés à la sortie. Les marqueurs de disponibilité sont réutilisés. Le suivi évite les rendus inutiles de son état de fraîcheur, ferme la connexion à la fin/annulation et n’affiche pas les données d’un compte précédent.

## Validation

18 tests Vitest ; build Vite ; 5 tests Playwright de l’accueil : visiteur, trajet client, disponibilités, prestataire mobile et erreur réseau. Les tests navigateur interceptent les API, WebSockets et fonds de carte : ils valident l’interface et le chargement différé, sans certifier les échanges avec les services réels. Vérification visuelle à 1 440 px et 390 px, sans débordement mobile.

Commandes : `npm test`, `npm run build`, `npm run test:frontpage` depuis `frontend/`. Pour le dernier : installer Chromium avec `npx playwright install chromium`, ou fournir `PLAYWRIGHT_CHROMIUM_EXECUTABLE` vers une installation existante. Aucun Docker n’est nécessaire ; le test lance puis ferme son serveur frontend.

## Phase 4

L’accueil utilise maintenant `/api/users/me/active-session` : la sélection n’est plus limitée à 50 résultats. Le GPS est géré au-dessus des écrans et continue pendant la navigation d’une séance EN_ROUTE. La disponibilité facultative reste indépendante ; ouvrir l’accueil d’un compte sans trajet actif ne demande pas le GPS.

Voir [TRACKING-PHASE4.md](TRACKING-PHASE4.md) pour les nouvelles mesures, les tests et leurs limites, [PRODUCTION.md](PRODUCTION.md) pour la migration, et [NEXT-PROMPT.md](NEXT-PROMPT.md) pour la suite.
