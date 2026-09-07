# syntax=docker/dockerfile:1.7

FROM node:22-alpine@sha256:16e22a550f3863206a3f701448c45f7912c6896a62de43add43bb9c86130c3e2 AS build
WORKDIR /app

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npm run build

FROM nginx:1.29.8-alpine@sha256:5616878291a2eed594aee8db4dade5878cf7edcb475e59193904b198d9b830de AS runtime

ENV BACKEND_URL=http://backend:8080 \
    OLIFAN_REDIRECT_URL=https://olifan.pixsom.fr \
    TRUSTED_PROXY_CIDR=127.0.0.1/32 \
    NGINX_ENVSUBST_OUTPUT_DIR=/tmp

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/templates/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

RUN apk upgrade --no-cache \
    && chown -R 101:101 /usr/share/nginx/html

EXPOSE 8080
USER 101

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/ || exit 1
