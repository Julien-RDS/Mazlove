ifneq ($(strip $(CODEX_PROXY_CERT)),)
COMPOSE ?= docker compose -f docker-compose.yml -f docker-compose.proxy.yml
else
COMPOSE ?= docker compose
endif

# Outils de développement (MailHog, PgAdmin) : profil Compose "dev", actif par défaut.
# Déploiement sans eux : make up COMPOSE_PROFILES=
export COMPOSE_PROFILES ?= dev

# Commit embarqué dans les images (étiquette) et dans le frontend (version.json) : voir `make version`.
export GIT_SHA ?= $(shell git rev-parse --short HEAD 2>/dev/null || echo unknown)

.PHONY: up down logs seed test clean status swagger mail db version

up:
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f backend

seed:
	$(COMPOSE) exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"' < backend/src/main/resources/sql/seed.sql

test:
	cd backend && mvn test

clean:
	$(COMPOSE) down -v --rmi local

status:
	$(COMPOSE) ps

version:
	./scripts/check-served-version.sh

swagger:
	@echo 'http://localhost:3001/swagger-ui.html'

mail:
	@echo 'http://localhost:8025'

db:
	@echo 'http://localhost:5050'

.PHONY: demo simulate e2e osrm-prepare
SESSION ?= 4
SCENARIO ?= HAPPY_PATH
SPEED ?= 10
demo:
	$(COMPOSE) up -d --build --wait
	@echo 'Démo : http://localhost:3000/demo — cliquez sur Lancer'
	@echo 'Alice / Sophie : alice@test.com / sophie@test.com — password123'
	@echo 'Emails : http://localhost:8025 — API : http://localhost:3001/swagger-ui.html'
simulate:
	node scripts/simulate.mjs $(SESSION) $(SCENARIO) $(SPEED)
e2e:
	cd frontend && npx playwright test
osrm-prepare:
	mkdir -p osrm-data
	curl -fL https://download.geofabrik.de/europe/france/ile-de-france-latest.osm.pbf -o osrm-data/ile-de-france-latest.osm.pbf
	docker run --rm -v "$(CURDIR)/osrm-data:/data" ghcr.io/project-osrm/osrm-backend:v5.27.1 osrm-extract -p /opt/car.lua /data/ile-de-france-latest.osm.pbf
	docker run --rm -v "$(CURDIR)/osrm-data:/data" ghcr.io/project-osrm/osrm-backend:v5.27.1 osrm-partition /data/ile-de-france-latest.osrm
	docker run --rm -v "$(CURDIR)/osrm-data:/data" ghcr.io/project-osrm/osrm-backend:v5.27.1 osrm-customize /data/ile-de-france-latest.osrm
