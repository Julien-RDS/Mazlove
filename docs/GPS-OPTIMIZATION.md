# GPS et intégration — 31 octobre 2026

Correctifs réalisés depuis `41506d5`. [Prompt pour la suite](NEXT-PROMPT.md).

## Défauts corrigés

- **Séance active inaccessible avec PostgreSQL.** La requête native sélectionnait une `Session` depuis `UserRepository`, ce qui provoquait une erreur de conversion `Object[] → Session`. Elle réside désormais dans `SessionRepository`. Les endpoints `/users/me/active-session` et `/users/me/provider-trip` sont vérifiés par HTTP avec les rôles client et prestataire.
- **Carte recréée au départ.** `ProviderJourney` conserve le même arbre React quand le trajet démarre, se termine ou rencontre une panne REST. Le marqueur du rendez-vous reçoit ses coordonnées avant son ajout à MapLibre, ce qui supprime une autre cause de carte en erreur.
- **Abonnements dupliqués.** L’accueil, la console et le suivi global partagent désormais un observateur par séance et jeton : une connexion d’observation et un rafraîchissement de snapshot toutes les 30 secondes, avec nettoyage après le dernier abonné. L’émetteur GPS garde sa propre connexion. Un snapshot ancien ne remplace plus une position reçue entre-temps.
- **Émission inutilement réveillée chaque seconde.** Le GPS programme le prochain envoi utile : au plus un envoi toutes les 3 secondes en déplacement, toutes les 10 secondes à l’arrêt. Si le navigateur ne rappelle pas le watcher, une position fraîche est demandée après 10 secondes. Les horodatages proviennent toujours du navigateur, notamment pour les deux points de confirmation d’arrivée. Une reconnexion demande un nouveau point ; aucun historique hors ligne n’est rejoué.
- **Premier point perdu.** Une position fraîche reçue avant la première connexion STOMP est conservée pour cette connexion. Les points trop anciens ou imprécis sont écartés.
- **Intégration confuse.** Un bandeau compact sous l’en-tête donne accès au trajet ; dans la console de ce trajet, l’état GPS est affiché directement sans bandeau doublonné. « GPS actif » apparaît après l’émission d’une position, avec des états distincts pour localisation, permission, perte de connexion et relais entre onglets. Un échec initial de chargement propose une reprise depuis l’accueil. Une panne REST temporaire conserve un suivi déjà démarré ; les refus d’authentification continuent de l’arrêter.
- **Caméra intrusive.** Déplacer ou zoomer manuellement suspend le recentrage automatique jusqu’au bouton « Recentrer ». Une mise à jour stationnaire n’anime plus la caméra et conserve l’orientation du marqueur. Le centre initial utilise le trajet disponible.

La recherche du trajet prestataire passe de 15 à 30 secondes, avec rafraîchissement immédiat après une action locale et au retour au premier plan. Les requêtes ne se chevauchent pas. Aucun gain de batterie chiffré n’a été mesuré.

## Vérifications exécutées

| Vérification | Résultat |
| --- | --- |
| `npm test` | 31 tests Vitest réussis, 9 fichiers |
| `npm run test:frontpage` avec Chromium système | 15 scénarios réussis ; API, STOMP et tuiles simulés, géolocalisation émulée |
| `mvn test`, Java 17 | 67 tests exécutés et réussis, 4 tests préexistants désactivés, aucune erreur |
| `ActiveSessionIT`, `StompSecurityIT`, `TrackingReliabilityIT` | 10 tests réussis avec PostgreSQL, Redis et serveur HTTP/STOMP réels ; 1 cas SMTP non activé |
| Régression HTTP des deux endpoints de séance active | Méthode d’intégration relancée avec les nouvelles assertions, réussie ; déjà comprise dans les 10 cas ci-dessus |
| `VITE_DEMO=false npm run build` | Build de production réussi, MapLibre différé, démo exclue |
| `git diff --check` | Sans erreur |

Build de production : entrée JavaScript **237,05 kB / 75,75 kB gzip** ; MapLibre chargé à la demande **806,89 kB / 219,77 kB gzip**. L’avertissement Vite de chunk supérieur à 500 kB concerne ce moteur différé. Aucun score Lighthouse n’a été mesuré.

Les tests d’intégration ont utilisé une base PostgreSQL dédiée et Redis DB 15, sur les services déjà disponibles. Leur serveur Spring était lancé sur un port éphémère. Les conteneurs applicatifs existants n’ont pas été reconstruits ni déployés pour cette livraison.

Les scénarios navigateur couvrent notamment la conservation du canvas au départ, l’unique abonnement d’observation lors de la navigation, la continuité lors d’un HTTP 503, la reprise après un échec initial, les états avant le premier point, le relais entre onglets et l’arrêt après arrivée, annulation, déconnexion ou expiration. Le viewport mobile est contrôlé pour les débordements ; cela ne constitue pas un essai GPS sur téléphone.

## Suite et mise en service

Le frontend **et** le backend doivent être reconstruits après récupération du commit pour bénéficier de tous les correctifs. Le déploiement reste à la charge de l’utilisateur.

Restent à exécuter : le parcours navigateur contre les applications réelles (`frontend/e2e/tracking.spec.js`), les essais Safari/iOS et Chrome/Android, la réception SOS dans MailHog et les contrôles de préproduction listés dans [NEXT-PROMPT.md](NEXT-PROMPT.md). Le navigateur ne garantit toujours pas le GPS en arrière-plan ou écran verrouillé ; Web Locks reste limité aux onglets d’une même origine et d’un même navigateur.
