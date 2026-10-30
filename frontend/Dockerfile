# syntax=docker/dockerfile:1
# Node 20 est en fin de vie depuis avril 2026 : Node 22 (LTS).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=secret,id=proxy_ca --mount=type=cache,target=/root/.npm \
    if [ -f /run/secrets/proxy_ca ]; then export NODE_EXTRA_CA_CERTS=/run/secrets/proxy_ca; fi && \
    npm ci
COPY . .
ARG VITE_DEMO=false
ARG VITE_MAP_STYLE_URL=https://tiles.openfreemap.org/styles/liberty
ENV VITE_DEMO=$VITE_DEMO VITE_MAP_STYLE_URL=$VITE_MAP_STYLE_URL
RUN npm run build

# Branche stable 1.30 épinglée : le tag suit ses correctifs de sécurité.
FROM nginx:1.30-alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod -R a+rX /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
