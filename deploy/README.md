# Deployment

This project is deployed as three containers per environment:

- PostgreSQL database
- Spring Boot backend
- Angular/Nginx frontend

GitHub Actions builds the backend and frontend images, then publishes them to GHCR. Dokploy pulls those images and runs the compose files in `deploy/dokploy`.

## Images

For repository `<owner>/<repo>`, GitHub Actions publishes:

- `ghcr.io/<owner>/<repo>/backend:staging` from branch `staging`
- `ghcr.io/<owner>/<repo>/frontend:staging` from branch `staging`
- `ghcr.io/<owner>/<repo>/backend:prod` from branch `main`
- `ghcr.io/<owner>/<repo>/frontend:prod` from branch `main`

Each build also publishes immutable short-SHA tags:

- `staging-<sha>`
- `prod-<sha>`

## Dokploy Compose Files

- Staging: `deploy/dokploy/docker-compose.staging.yml`
- Production: `deploy/dokploy/docker-compose.production.yml`

Use the matching `.env.example` file as a checklist for Dokploy environment variables. Do not commit real `.env` files or secrets.

## Required Variables

- `BACKEND_IMAGE`: backend image tag to deploy.
- `FRONTEND_IMAGE`: frontend image tag to deploy.
- `POSTGRES_DB`: database name.
- `POSTGRES_USER`: database user.
- `POSTGRES_PASSWORD`: database password.
- `JWT_SECRET`: HS256 signing secret. Use a long random value.
- `JWT_EXPIRATION`: token lifetime in milliseconds.
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: defaults to `update` for first deployments.

## Routing

Expose the `frontend` service on port `80` in Dokploy. The frontend Nginx container proxies `/api/*` to the internal backend service, so the public application can use a single domain.

The backend service listens on internal port `8080`. It does not need a public domain unless you explicitly want one.
