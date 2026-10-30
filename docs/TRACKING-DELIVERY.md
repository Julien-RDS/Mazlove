# Livraison Phase 3

Ce document conserve le bilan historique de phase 3. Le bilan courant est [TRACKING-PHASE4.md](TRACKING-PHASE4.md).


Code livré : cycle de séance, migration SQL, consentement, contrôle des destinations STOMP, ingestion GPS filtrée et éphémère, routage OSRM/repli, arrivée automatique, déviation, signal perdu/récupéré, snapshots, carte animée, console prestataire, rendez-vous, partage public, contacts, SOS et liste administrateur, disponibilité approximative, simulation, scripts et collection Bruno.

La simulation emploie les comptes fictifs du seed et le même service que les positions réelles. Les six scénarios incluent les rejets GPS et les interruptions. Les fixtures GeoJSON proviennent de réponses réelles d’OSRM. Le profil de développement est interdit avec `prod`.

## Vérifications exécutées sans Docker

- Backend : 62 tests comptabilisés, 58 exécutés, 4 tests d’intégration existants ignorés ; zéro erreur ou échec.
- Frontend : 8 tests Vitest réussis ; build Vite avec `VITE_DEMO=true` réussi.
- Fichiers de dépendances cohérents, syntaxe des scripts Node et `git diff --check` vérifiés.
- Compilation du test STOMP d’intégration ; pas d’exécution sans PostgreSQL/Redis.
- Aucun conteneur, déploiement ni test navigateur avec services n’a été lancé.

## Adaptations et limites à vérifier après le pull

- Spring Boot 3.1 / Spring 6.0 utilisent `RestTemplate` avec délais, car `RestClient` n’existe pas dans cette version.
- Les validations ont été regroupées après préparation du JDK/Maven hors dépôt, plutôt qu’exécutées séparément à chacun des huit checkpoints.
- Le test `StompSecurityIT` utilise PostgreSQL/Redis déjà démarrés, sans Testcontainers. Activer `MAZLOVE_STOMP_IT=true` et le sélectionner explicitement avec Maven. Les tests Playwright sont fournis et restent à exécuter avec les services et Chromium installés.
- L’arrêt du partage à l’arrivée prime sur la durée théorique du lien après fin. Le SOS après arrivée prévient les contacts sans réexposer la dernière position live. Aucun SMS ni appel aux secours automatique.
- Le test navigateur vérifie le nettoyage via le snapshot ; le test STOMP contrôle directement les clés Redis. La fluidité sur appareils réels et les scénarios réseau doivent être validés sur l’environnement déployé.
- Le serveur utilise une grille stable pour les prestataires disponibles ; la page publique arrondit à environ 20 m. Les événements sont conservés 30 jours, la position Redis expire après 120 s.
- Le build signale un bundle supérieur à 500 ko à cause de MapLibre. Prévoir un découpage supplémentaire si nécessaire ; aucune erreur de compilation.
- En production : HTTPS, `SPRING_PROFILES_ACTIVE=prod`, `VITE_DEMO=false`, secrets propres, migration SQL avant validation Hibernate, OSRM hébergé et tuiles avec SLA. La PWA ne garantit pas le GPS en arrière-plan ; le broker simple vise une instance.

Les fonctionnalités bonus de la section 19 et le check-in facultatif ne sont pas inclus.
