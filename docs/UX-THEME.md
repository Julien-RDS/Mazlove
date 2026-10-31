# Palette rose / rouge et mode nuit — 31 octobre 2026

Livraison à partir de `01f9b07`. Changement visuel uniquement : aucun parcours, route, API ni logique de suivi n'est modifié. Les écrans gardent la même mise en page (comparée avant/après sur la fiche praticien, voir « Vérifications »).

## Ce qui change

- **L'orange disparaît.** Le **rose** porte l'identité : logo, aplat de l'accueil, étoiles, état « Réservée », pastilles. Le **rouge** réserve son poids aux moments qui comptent : ticket du code d'arrivée (texte blanc), bouton SOS, favori. Le vert sauge reste celui de « vérifié / séance en cours ».
- **Mode nuit.** Bouton lune/soleil dans l'en-tête (étiquette « Mode nuit », état annoncé par `aria-pressed`). Sans choix, l'application suit le réglage du système **en direct**. Un clic enregistre un choix (`localStorage`, clé `mazlove.theme`) ; un clic qui ramène au réglage du système efface ce choix, et l'application se remet à le suivre. Un script en `<head>` pose le thème avant le premier rendu (pas d'éclair de la mauvaise couleur) ; la barre du navigateur mobile prend la couleur du fond.
- **Carte.** La nuit, fond OpenFreeMap `dark` (même hébergeur que `liberty`, gratuit, sans clé), contrôles et crédits assortis, tracé vert clair et épingle rouge. `VITE_MAP_STYLE_DARK_URL` permet d'en choisir un autre ; `VITE_MAP_STYLE_URL` reste le fond du jour. Changer de thème carte ouverte **recrée la carte** avec le bon fond (nouvelle demande de style et de tuiles) ; MapLibre reste chargé à la demande.

## Comment c'est construit

Toutes les couleurs vivent dans `frontend/src/theme.css` sous forme de variables (`--c-brume`, `--c-rose-deep`…), en triplets RGB pour garder l'opacité. `tailwind.config.js` les lit (`bg-brume`, `text-rose-deep`, `bg-rouge/40`) et les feuilles de style les utilisent directement : le mode nuit n'exige donc aucune variante `dark:` composant par composant.

Les noms désignent des **rôles**, pas des teintes : en mode nuit, `nuit` (l'encre) devient clair et `brume` (le fond) devient sombre ; `sur-nuit` est le texte posé sur une surface `nuit` (le bouton principal s'inverse). Pour une nouvelle couleur, ajouter un jeton dans les deux blocs de `theme.css`, jamais un hexadécimal dans un composant ou une feuille de style.

Seules exceptions tolérées en dur : le blanc des marqueurs de carte, le gris d'un marqueur périmé, une étiquette sur photo et quelques ombres à très faible opacité.

## Contrastes

Seuils : 4,5 pour le texte (AA), 3 pour les icônes. Ratios calculés à partir de `theme.css` ; `src/lib/theme.test.js` les contrôle pour 45 paires par thème.

| Paire | Jour | Nuit |
| --- | --- | --- |
| Texte principal (nuit / brume) | 14,7 | 16,2 |
| Texte secondaire (nuit-doux / brume) | 5,0 | 8,4 |
| Texte secondaire sur carte | 5,4 | 7,8 |
| Bouton principal (sur-nuit / nuit) | 15,8 | 16,2 |
| Texte rose (rose-deep / brume) | 6,7 | 9,6 |
| Texte sur l'aplat rose (rose-ink / rose-soft) | 7,8 | 9,3 |
| Texte rouge (rouge-deep / carte) | 8,9 | 7,6 |
| Vert (sauge / sauge-wash) | 5,9 | 8,2 |
| Erreur (alerte / alerte-pale) | 5,0 | 7,0 |
| Code d'arrivée (blanc / rouge) | 5,5 | 5,5 |
| Étoiles et icônes (rose / brume), seuil 3 | 4,3 | 4,7 |

Deux corrections de contraste au passage. Le rouge d'erreur `alerte` n'atteignait que **4,37** sur son fond pâle (l'ancien commentaire de la configuration annonçait pourtant des contrastes vérifiés) : il passe de `#B4443A` à `#A83D33` (4,95). Les gris secondaires du CSS (24 valeurs presque identiques) sont regroupés en un seul jeton ; 16 d'entre elles étaient sous 4,5 sur le fond de page, souvent pour de petits textes. Ces textes sont un peu plus foncés qu'avant.

## Vérifications exécutées

| Vérification | Résultat |
| --- | --- |
| `npm test` | 51 tests Vitest réussis, 10 fichiers (31 existants + 20 pour le thème) |
| `npm run test:frontpage` avec Chromium 153 | 21 scénarios réussis (15 existants + 6 pour le thème) ; API, STOMP et tuiles simulés |
| `VITE_DEMO=false npm run build` | Réussi ; MapLibre toujours différé |
| Garde-fous | Chacun a été vérifié en **cassant volontairement** la fonctionnalité : contraste d'origine rétabli, jeton oublié en mode nuit, orange réintroduit dans un CSS, bascule qui n'efface plus le choix, fond sombre jamais choisi, carte non recréée. Chaque défaut fait échouer le test attendu. |
| Rendu | Captures Chromium sans tête (bureau 1280 px, mobile 390 px, jour et nuit) de l'accueil, de la section de suivi, du ticket, du panneau de trajet (carte, ETA, SOS) et de la fiche praticien. La fiche praticien a été rendue **sur l'ancien et le nouveau code avec le même jeu de données** (1280 px, mode jour) : mise en page identique à l'œil, seules les couleurs changent. Comparaison visuelle, pas un test automatisé. |

Le test des contrastes lit `theme.css` ; il ne mesure pas ce qu'un navigateur affiche. Aucun score Lighthouse n'est annoncé.

## Limites

- **Fond sombre réel non chargé.** Les tuiles sont simulées dans tous les tests : l'URL `https://tiles.openfreemap.org/styles/dark` n'a pas été chargée par l'application. À vérifier une fois sur la préproduction (carte ouverte la nuit).
- **Appareils réels.** Aucun essai sur téléphone : couleur de la barre du navigateur, bascule avec le thème du système et rendu sur écran OLED restent à constater. Le comportement de la barre du navigateur n'a pas été vérifié sur iOS.
- **Autres écrans.** Connexion, liste des séances, console prestataire et page de partage public n'ont pas fait l'objet d'une comparaison visuelle ; elles utilisent les mêmes jetons et classes.
- **Politique de sécurité.** Le script de thème dans `index.html` est en ligne. Une future politique `Content-Security-Policy` stricte devra l'autoriser par empreinte ou `nonce`, ou le déplacer dans un fichier.
- **Photos.** Les portraits sont légèrement assombris la nuit (`brightness .9`) ; réglage purement esthétique, à ajuster.
- **Carte et changement de thème.** Le thème n'est lu qu'à la création d'une carte. Un changement, même automatique (réglage système qui bascule au coucher du soleil), recrée la carte ouverte et relance sa caméra : le recentrage automatique est de nouveau actif.
