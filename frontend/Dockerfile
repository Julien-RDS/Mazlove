# syntax=docker/dockerfile:1
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=secret,id=proxy_ca \
    if [ -f /run/secrets/proxy_ca ]; then export NODE_EXTRA_CA_CERTS=/run/secrets/proxy_ca; fi && \
    npm ci
COPY . .
ARG VITE_DEMO=false
ARG VITE_MAP_STYLE_URL=https://tiles.openfreemap.org/styles/liberty
ENV VITE_DEMO=$VITE_DEMO VITE_MAP_STYLE_URL=$VITE_MAP_STYLE_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod -R a+rX /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
