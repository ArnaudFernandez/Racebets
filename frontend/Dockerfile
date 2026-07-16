# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npm run build

FROM nginx:1.29.8-alpine AS runtime

ENV BACKEND_URL=http://backend:8080 \
    NGINX_ENVSUBST_OUTPUT_DIR=/tmp

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/templates/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

RUN chown -R 101:101 /usr/share/nginx/html

EXPOSE 8080
USER 101

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/ || exit 1
