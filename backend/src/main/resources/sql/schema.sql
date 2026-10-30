-- ═════════════════════════════════════════════════════════════════════════════
-- Schéma PostgreSQL de Mazlove
--
-- Exécuté à chaque démarrage par Spring Boot (spring.sql.init), AVANT Hibernate.
-- Tout est idempotent (IF NOT EXISTS) : le rejouer sur une base existante ne fait rien.
-- La base elle-même doit déjà exister : createdb mazlove_db
-- (pas de CREATE DATABASE ici : il échouerait, Spring étant déjà connecté à la base).
--
-- Chaque colonne correspond à un champ d'entité JPA ; Hibernate (ddl-auto=validate)
-- refuse de démarrer si une colonne manque ou n'a pas le bon type.
-- ═════════════════════════════════════════════════════════════════════════════

-- Comptes : clients, praticiens et admins (entité User)
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,      -- stocké en minuscules par l'application
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,             -- hash BCrypt (60 caractères)
    phone       VARCHAR(20)  NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('CLIENT', 'PROVIDER', 'ADMIN')),
    rating      REAL         NOT NULL DEFAULT 0,   -- REAL = float Java ; NOT NULL car type primitif
    avatar      TEXT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Fiches praticiens (entité Provider). latitude/longitude = @Embedded Location.
CREATE TABLE IF NOT EXISTS providers (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    bio         TEXT         NOT NULL,
    category    VARCHAR(100) NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    verified    BOOLEAN      NOT NULL DEFAULT FALSE, -- attribué par un admin uniquement
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Mise à niveau des bases créées avec l'ancien schéma (sans effet sur une base neuve).
ALTER TABLE providers ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION;
ALTER TABLE providers ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE providers ADD COLUMN IF NOT EXISTS verified  BOOLEAN NOT NULL DEFAULT FALSE;

-- Photos d'un praticien (@ElementCollection Provider.images)
CREATE TABLE IF NOT EXISTS provider_images (
    provider_id BIGINT NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    image_url   TEXT   NOT NULL
);

-- Créneaux hebdomadaires (@ElementCollection Provider.availability).
-- Heures en VARCHAR(5) "HH:mm" et non TIME : le modèle Java les manipule en String.
CREATE TABLE IF NOT EXISTS provider_availability (
    provider_id BIGINT      NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN
                    ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time  VARCHAR(5)  NOT NULL,
    end_time    VARCHAR(5)  NOT NULL,
    CHECK (start_time < end_time)  -- valable car toujours sur 5 caractères "HH:mm"
);

-- Prestations tarifées (entité ServiceItem, pas encore exposée par l'API)
CREATE TABLE IF NOT EXISTS service_items (
    id          BIGSERIAL PRIMARY KEY,
    provider_id BIGINT       NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    price       REAL         NOT NULL CHECK (price >= 0),
    duration    INTEGER      NOT NULL DEFAULT 1 CHECK (duration BETWEEN 1 AND 12),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE service_items ADD COLUMN IF NOT EXISTS duration INTEGER NOT NULL DEFAULT 1;

-- Séances réservées (entité Session)
CREATE TABLE IF NOT EXISTS sessions (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider_id     BIGINT      NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    datetime        TIMESTAMP   NOT NULL,
    duration        INTEGER     NOT NULL DEFAULT 1 CHECK (duration BETWEEN 1 AND 12), -- en heures
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DONE', 'CANCELLED')),
    code_validation VARCHAR(6)  NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sessions ADD COLUMN IF NOT EXISTS duration INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS service_item_id BIGINT REFERENCES service_items(id) ON DELETE SET NULL;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS price_snapshot REAL;

-- Avis (entité Review). provider_id est dénormalisé depuis sessions.provider_id
-- pour lister les avis et calculer la note moyenne d'un praticien sans double jointure.
CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_id  BIGINT    NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    provider_id BIGINT    NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    rating      INTEGER   NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ajout sans NOT NULL pour ne pas échouer sur d'anciennes lignes ; l'application le remplit toujours.
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS provider_id BIGINT REFERENCES providers (id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS favorites (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider_id BIGINT NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, provider_id)
);

-- ─── Index ───────────────────────────────────────────────────────────────────
-- (users.email a déjà un index implicite via UNIQUE : inutile d'en créer un second.)

-- Règles métier garanties par la base, même en cas de requêtes simultanées :
CREATE UNIQUE INDEX IF NOT EXISTS uq_providers_user   ON providers (user_id);   -- une fiche par compte
CREATE UNIQUE INDEX IF NOT EXISTS uq_reviews_session  ON reviews (session_id);  -- un avis par séance

-- Recherche géographique : pré-filtre "boîte englobante" (BETWEEN sur lat/lng) de
-- ProviderRepository.findNearby. Un index B-tree composite suffit à ce volume ;
-- passer à PostGIS (GiST) si l'on dépasse quelques centaines de milliers de praticiens.
CREATE INDEX IF NOT EXISTS idx_providers_location ON providers (latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_users_phone                ON users (phone);
CREATE INDEX IF NOT EXISTS idx_provider_availability_day  ON provider_availability (provider_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_provider_images_provider   ON provider_images (provider_id);
CREATE INDEX IF NOT EXISTS idx_sessions_client            ON sessions (client_id);
-- Détection des chevauchements à la réservation (praticien + plage horaire).
CREATE INDEX IF NOT EXISTS idx_sessions_provider_datetime ON sessions (provider_id, datetime);
CREATE INDEX IF NOT EXISTS idx_reviews_user               ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_provider           ON reviews (provider_id, created_at DESC);

-- Phase 3. Existing ACCEPTED sessions meant IN_PROGRESS: migrate ONCE before adding milestones.
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sessions' AND column_name='started_at') THEN
  ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_status_check;
  UPDATE sessions SET status='IN_PROGRESS' WHERE status='ACCEPTED';
 END IF;
END $$;
ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_status_check;
ALTER TABLE sessions ADD CONSTRAINT sessions_status_check CHECK(status IN ('PENDING','ACCEPTED','EN_ROUTE','ARRIVED','IN_PROGRESS','DONE','CANCELLED'));
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS meeting_latitude DOUBLE PRECISION;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS meeting_longitude DOUBLE PRECISION;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS meeting_label VARCHAR(255);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS travel_mode VARCHAR(16);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS en_route_at TIMESTAMP;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS arrived_at TIMESTAMP;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS uq_provider_en_route ON sessions(provider_id) WHERE status='EN_ROUTE';
CREATE TABLE IF NOT EXISTS tracking_events(id BIGSERIAL PRIMARY KEY,session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,event_type VARCHAR(32) NOT NULL,latitude DOUBLE PRECISION,longitude DOUBLE PRECISION,details VARCHAR(500),created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX IF NOT EXISTS idx_tracking_events_session ON tracking_events(session_id,created_at);
CREATE TABLE IF NOT EXISTS trip_shares(token VARCHAR(64) PRIMARY KEY,session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,expires_at TIMESTAMP NOT NULL,revoked BOOLEAN NOT NULL DEFAULT FALSE);
CREATE TABLE IF NOT EXISTS trusted_contacts(id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,name VARCHAR(100) NOT NULL,email VARCHAR(255),phone VARCHAR(30));
CREATE TABLE IF NOT EXISTS sos_alerts(id BIGSERIAL PRIMARY KEY,session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,raised_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,latitude DOUBLE PRECISION,longitude DOUBLE PRECISION,status VARCHAR(16) NOT NULL DEFAULT 'OPEN',created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,resolved_at TIMESTAMP);
CREATE TABLE IF NOT EXISTS consents(id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,scope VARCHAR(40) NOT NULL,policy_version VARCHAR(16) NOT NULL,granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,revoked_at TIMESTAMP);

ALTER TABLE sessions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
