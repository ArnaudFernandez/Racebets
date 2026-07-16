# Deploiement Racebets

Racebets est deploye sous forme de trois services prives : PostgreSQL, le backend Spring Boot et le frontend
Angular servi par Nginx. Seul le frontend doit recevoir un domaine public. Il relaie `/api/*` vers le backend sur
le reseau Docker interne.

## Images publiees

Le workflow `.github/workflows/container-build.yml` teste le projet puis publie dans GHCR :

- `ghcr.io/arnaudfernandez/racebets/backend:prod-<sha-complet>` depuis `main` ;
- `ghcr.io/arnaudfernandez/racebets/frontend:prod-<sha-complet>` depuis `main` ;
- les equivalents `staging-<sha-complet>` depuis `staging` ;
- des alias mobiles `prod` et `staging`, pratiques pour l'observation mais a ne pas utiliser pour un deploiement
  reproductible.

Les deux images configurees dans Dokploy doivent toujours porter le meme SHA complet. Un retour arriere consiste a
remettre les deux tags du dernier commit sain puis a redeployer. La base PostgreSQL n'est jamais exposee par un port
public.

## Secrets obligatoires

Ne jamais copier un secret dans Git. Les valeurs sont creees directement dans Dokploy :

```bash
openssl rand -hex 32  # POSTGRES_PASSWORD
openssl rand -hex 64  # JWT_SECRET
openssl rand -base64 24  # code du premier administrateur
```

Le secret JWT versionne dans une ancienne revision du depot doit etre considere compromis et ne doit jamais etre
reutilise. `JWT_SECRET` est obligatoire au demarrage et doit contenir au minimum 32 octets.

## Premier administrateur

Sur une base neuve uniquement, effectuer un bootstrap temporaire :

1. Definir `RACEBETS_DEV_ADMIN_ENABLED=true` dans Dokploy.
2. Definir `RACEBETS_DEV_ADMIN_EMAIL` et `RACEBETS_DEV_ADMIN_ACCESS_CODE` avec des valeurs uniques.
3. Deployer, verifier que la connexion administrateur fonctionne.
4. Remettre immediatement `RACEBETS_DEV_ADMIN_ENABLED=false`.
5. Supprimer les deux variables contenant l'email et le code, puis redeployer.

Le seeder n'ecrase pas le code d'un compte existant. Il ne doit toutefois jamais rester active en production.

## Variables

Les fichiers `production.env.example` et `staging.env.example` sont des listes de controle. Pour le premier
deploiement, `SPRING_JPA_HIBERNATE_DDL_AUTO=update` initialise le schema. L'utilisation d'un outil de migrations
versionnees (Flyway ou Liquibase) reste necessaire avant des evolutions de schema complexes ; ne pas passer
aveuglement a `validate` tant qu'aucune migration initiale ne decrit le schema.

## Dokploy

- Compose production : `deploy/dokploy/docker-compose.production.yml`.
- Compose staging : `deploy/dokploy/docker-compose.staging.yml`.
- Service public : `frontend`.
- Port interne du domaine : `8080`.
- HTTPS : active avec un certificat Let's Encrypt.
- `backend`, `postgres` et leurs ports ne doivent avoir aucun domaine ni publication de port.

Les conteneurs applicatifs s'executent sans privileges, avec un systeme de fichiers en lecture seule, toutes les
capabilities Linux supprimees, un `/tmp` borne, des healthchecks et une rotation des journaux.

## Sauvegardes

Configurer une destination S3 hors du VPS, une sauvegarde PostgreSQL quotidienne, une retention d'au moins sept
jours et lancer un test manuel. Tester aussi une restauration avant l'ouverture publique. Le volume nomme
`postgres_data` persiste entre les deploiements mais ne remplace jamais une sauvegarde externe.
