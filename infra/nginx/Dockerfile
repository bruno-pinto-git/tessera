FROM node:20-alpine AS frontend-build
WORKDIR /app
# Include .npmrc so the build picks up `legacy-peer-deps=true` (needed
# because openapi-typescript@7 still declares its TS peer as ^5 while
# the project uses TS 6). Without this, `npm ci` would ERESOLVE.
COPY frontend/.npmrc frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
# Public Keycloak URL baked into the bundle. Defaults to the dev value so local
# `docker compose build` is unchanged; the prod compose overrides it with the
# public HTTPS URL (e.g. https://<fqdn>/auth). Vite gives process.env precedence
# over .env.production for VITE_* keys.
ARG VITE_KEYCLOAK_URL=http://localhost:8180
RUN VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL npm run build

FROM nginx:alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html
EXPOSE 80
