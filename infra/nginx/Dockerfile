FROM node:20-alpine AS frontend-build
WORKDIR /app
# Include .npmrc so the build picks up `legacy-peer-deps=true` (needed
# because openapi-typescript@7 still declares its TS peer as ^5 while
# the project uses TS 6). Without this, `npm ci` would ERESOLVE.
COPY frontend/.npmrc frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html
EXPOSE 80
