# Mazlove

Réservation de séances de câlins professionnels, non sexuelles : recherche de praticiens,
réservation sécurisée par code d'arrivée, déroulé de la séance et avis.

## Stack

- **Backend** : Spring Boot 3.1 · Java 17 (compatible 21) · Maven
- **Base de données** : PostgreSQL 14+ (schéma créé automatiquement au démarrage)
- **Cache et codes OTP** : Redis 7+
- **Authentification** : mot de passe BCrypt, puis code à 6 chiffres (double authentification), puis JWT
- **Temps réel** : STOMP sur WebSocket (`/ws`)
- **Frontend** : React 18 · Vite · Tailwind CSS

## Structure

```
Mazlove/
├── backend/
│   ├── .env.example                 # Variables d'environnement à copier en .env
│   └── src/
│       ├── main/java/com/mazlove/backend/
│       │   ├── config/              # Sécurité, JWT, CORS, Redis, WebSocket, Swagger, erreurs
│       │   ├── controller/          # Endpoints REST et STOMP
│       │   ├── dto/                 # Objets échangés avec le frontend
│       │   ├── exception/           # Exceptions métier (400, 401, 403, 404, 409)
│       │   ├── models/              # Entités JPA
│       │   ├── repository/          # Requêtes (dont recherche géographique)
│       │   ├── service/             # Règles métier
│       │   └── util/                # Géographie, pagination, emails…
│       ├── main/resources/
│       │   ├── application*.properties
│       │   └── sql/schema.sql, demo-data.sql
│       └── test/                    # JUnit (java) et Gatling (scala)
└── frontend/
    └── src/
        ├── api.js                   # Client HTTP (JWT, erreurs)
        ├── lib/                     # Routage par hash, formatage FR
        └── components/              # Écrans (CuddleApp = coquille)
```

## Démarrage en local

### 1. Prérequis

PostgreSQL et Redis doivent tourner. La base doit exister :

```bash
createdb mazlove_db
```

### 2. Backend

```bash
cd backend
cp .env.example .env        # puis renseigner JWT_SECRET (openssl rand -base64 48) et DB_PASSWORD
mvn spring-boot:run
```

- API : http://localhost:3001
- Documentation interactive : http://localhost:3001/swagger-ui.html

Le `.env.example` active le profil **demo** (comptes et praticiens fictifs autour de Paris) et
l'affichage du code de connexion dans la console. Retirez ces deux lignes hors développement.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Interface : http://localhost:3000 (les appels `/api` sont relayés vers le port 3001 par Vite).

### Comptes de démonstration (profil demo)

Mot de passe commun : `Mazlove2026!`

| Email                  | Rôle                            |
|------------------------|---------------------------------|
| `client@mazlove.test`  | Client (a déjà des séances)     |
| `lea@mazlove.test`     | Praticienne vérifiée            |
| `samir@mazlove.test`   | Praticien vérifié               |
| `ines@mazlove.test`    | Praticienne (non vérifiée)      |

À la connexion, le code à 6 chiffres s'affiche dans la console du backend, sur une ligne `[DEV] Code de connexion`.

## Variables d'environnement

| Variable                 | Rôle                                               | Défaut                                         |
|--------------------------|----------------------------------------------------|------------------------------------------------|
| `DATABASE_URL`           | URL JDBC PostgreSQL                                | `jdbc:postgresql://localhost:5432/mazlove_db`  |
| `DB_USERNAME`            | Utilisateur PostgreSQL                             | `postgres`                                     |
| `DB_PASSWORD`            | Mot de passe PostgreSQL                            | vide                                           |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Connexion Redis              | `localhost` / `6379` / vide                    |
| `JWT_SECRET`             | Clé de signature, **32 caractères minimum**        | valeur d'exemple (avertissement au démarrage)  |
| `JWT_EXPIRATION`         | Validité du jeton (ms)                             | `604800000` (7 jours)                          |
| `ALLOWED_ORIGINS`        | Origines CORS / WebSocket, séparées par des virgules | `http://localhost:3000,http://localhost:3001` |
| `MFA_DEV_LOG_OTP`        | Affiche le code de connexion dans les logs         | `false`                                        |
| `DB_INIT_MODE`           | `always` rejoue schema.sql (idempotent), `never` sinon | `always`                                   |
| `CACHE_TTL`              | Durée du cache Redis (`10m`, `600000`…)            | `10m`                                          |
| `SPRING_PROFILES_ACTIVE` | `demo` (données fictives) ou `prod`                | aucun                                          |

## Parcours dans l'interface

1. **Sélection** : taille, traits physiques, recherche, résultat. Ces critères sont repris à
   l'identique de la version d'origine ; ils seront redéfinis par l'équipe produit et ne filtrent
   pas encore la liste (l'API ne possède pas ces champs). La liste réelle vient de
   `GET /api/providers?lat=48.87&lng=2.35` (50 km autour de Paris centre, du plus proche au plus éloigné).
2. **Fiche praticien** : présentation, disponibilités, avis, déroulé d'une séance.
3. **Réservation** : jour, durée, heure (uniquement dans les créneaux du praticien), consignes.
4. **Code d'arrivée** : le client le montre au praticien, qui le saisit pour démarrer la séance.
5. **Fin de séance**, puis **avis** du client.

Un compte praticien sans fiche est guidé vers « Publier ma fiche ».

## Endpoints

| Méthode | Chemin                                  | Accès     | Rôle |
|---------|-----------------------------------------|-----------|------|
| POST    | `/api/auth/register`                    | public    | Créer un compte (CLIENT ou PROVIDER) |
| POST    | `/api/auth/login`                       | public    | Vérifier les identifiants et envoyer le code |
| POST    | `/api/auth/verify-otp`                  | public    | Échanger le code contre un JWT |
| GET     | `/api/providers?lat=&lng=`              | public    | Praticiens à moins de 50 km |
| GET     | `/api/providers/{id}`                   | public    | Fiche praticien |
| GET     | `/api/providers/{id}/reviews`           | public    | Avis d'un praticien |
| GET     | `/api/providers/availability?lat=&lng=&date=` | public | Praticiens disponibles un jour donné |
| POST    | `/api/providers`                        | PROVIDER  | Publier sa fiche |
| POST    | `/api/providers/{id}/reviews`           | client    | Avis sur une séance terminée |
| POST    | `/api/sessions`                         | connecté  | Réserver |
| GET     | `/api/sessions/{id}`                    | participant | Détail d'une séance |
| POST    | `/api/sessions/{id}/validate-arrival`   | praticien | Saisir le code d'arrivée |
| POST    | `/api/sessions/{id}/confirm-end`        | participant | Terminer la séance |
| POST    | `/api/sessions/{id}/cancel`             | participant | Annuler (avant le début) |
| GET     | `/api/users/me`, `/me/sessions`, `/me/reviews` | connecté | Espace personnel |

Toutes les erreurs ont le même format : `{ "status": 409, "message": "…", "fields": { "champ": "…" } }`.

### Exemple avec curl

```bash
curl -X POST localhost:3001/api/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"client@mazlove.test","password":"Mazlove2026!"}'
# lire le code dans la console du backend, puis :
curl -X POST localhost:3001/api/auth/verify-otp -H 'Content-Type: application/json' \
     -d '{"email":"client@mazlove.test","code":"123456"}'
```

## Tests

```bash
cd backend
mvn test                       # tests unitaires, sans base ni Redis
MAZLOVE_IT=true mvn test       # + tests d'intégration (PostgreSQL et Redis démarrés)
mvn gatling:test               # test de charge (API démarrée ; MAZLOVE_JWT pour inclure la réservation)
```

## Production

- `SPRING_PROFILES_ACTIVE=prod` : schéma non rejoué, Swagger désactivé, logs dans `LOG_PATH`.
- `JWT_SECRET` fort et propre à l'environnement ; `MFA_DEV_LOG_OTP` est forcé à `false`.
- Brancher l'envoi réel du code de connexion (email ou SMS) dans `MfaService.sendOtp`.
- Frontend : `npm run build`, puis servir `frontend/dist`. Si l'API est sur un autre domaine,
  définir `VITE_API_URL` au moment du build et l'ajouter à `ALLOWED_ORIGINS`.

## Licence

Apache 2.0
