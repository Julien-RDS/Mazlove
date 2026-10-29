ifneq ($(strip $(CODEX_PROXY_CERT)),)
COMPOSE ?= docker compose -f docker-compose.yml -f docker-compose.proxy.yml
else
COMPOSE ?= docker compose
endif

.PHONY: up down logs seed test clean status swagger mail db

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

swagger:
	@echo 'http://localhost:3001/swagger-ui.html'

mail:
	@echo 'http://localhost:8025'

db:
	@echo 'http://localhost:5050'
