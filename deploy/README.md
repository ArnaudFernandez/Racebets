# Deploiement Racebets

Racebets est deploye sous forme de trois services prives : PostgreSQL, le backend Spring Boot et le frontend
Angular servi par Nginx. Seul le frontend doit recevoir un domaine public. Il relaie `/api/*` vers le backend sur
le reseau Docker interne.

## Images publiees

Le workflow `.github/workflows/container-build.yml` teste le projet puis publie dans GHCR. Une image n'est publiee
qu'apres :

- les tests backend executes sur PostgreSQL 17 ;
- l'application de toutes les migrations Flyway et la validation du schema par Hibernate ;
- le build, le lint et l'audit des dependances frontend de production ;
- la validation des deux fichiers Compose ;
- le build et le scan Trivy des images backend et frontend (vulnerabilites corrigeables `HIGH`/`CRITICAL`).

Les images produites sont :

- `ghcr.io/arnaudfernandez/racebets/backend:prod-<sha-complet>` depuis `main` ;
- `ghcr.io/arnaudfernandez/racebets/frontend:prod-<sha-complet>` depuis `main` ;
- les equivalents `staging-<sha-complet>` depuis `staging` ;
- les equivalents `olifan-<sha-complet>` et l'alias mobile `olifan` depuis `olifan-group` ;
- des alias mobiles `prod` et `staging`, pratiques pour l'observation mais a ne pas utiliser pour un deploiement
  reproductible.

Les deux images configurees dans Dokploy doivent toujours porter le meme SHA complet. Un retour arriere applicatif
consiste a remettre les deux tags du dernier commit sain puis a redeployer. Une migration de base n'est jamais
annulee automatiquement : verifier sa compatibilite avant tout retour arriere. La base PostgreSQL n'est jamais
exposee par un port public.

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

Les fichiers `production.env.example` et `staging.env.example` sont des listes de controle. Le schema est gere par
Flyway dans `src/main/resources/db/migration`. Hibernate reste en `validate` et ne modifie jamais la base.

### Adoption Flyway sur la staging existante

La staging a deja ete creee par Hibernate. Son premier deploiement avec Flyway doit donc etre fait ainsi :

1. Creer et verifier une sauvegarde PostgreSQL hors du VPS.
2. Conserver `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.
3. Definir temporairement `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` et
   `SPRING_FLYWAY_BASELINE_VERSION=4`.
4. Deployer les deux images `staging-<meme-sha>` et verifier dans les logs backend la creation de
   `flyway_schema_history`, puis le demarrage complet de l'application.
5. Tester connexion, administration, pari, historique, quiz et partenaires.
6. Remettre `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false` et `SPRING_FLYWAY_BASELINE_VERSION=1`, puis redeployer une
   derniere fois.

Le baseline enregistre la base existante en version 4 sans rejouer les migrations historiques. La migration V5
reconcilie ensuite le schema de maniere additive et ne modifie aucune valeur metier existante. Si Hibernate refuse le
demarrage en mode `validate`, ne jamais revenir a `update` : restaurer si necessaire et comparer le schema staging a
la migration.

### Base de production neuve

Garder `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false`. Flyway execute `V1__initial_schema.sql` sur la base vide, puis
Hibernate en valide la conformite. Toute evolution future du schema prend un nouveau fichier immuable (`V2__...sql`,
`V3__...sql`, etc.) ; un fichier deja applique ne doit jamais etre modifie.

## Dokploy

- Compose production : `deploy/dokploy/docker-compose.production.yml`.
- Compose staging : `deploy/dokploy/docker-compose.staging.yml`.
- Service public : `frontend`.
- Port interne du domaine : `8080`.
- HTTPS : active avec un certificat Let's Encrypt.
- `backend`, `postgres` et leurs ports ne doivent avoir aucun domaine ni publication de port.

Definir `TRUSTED_PROXY_CIDR` avec le sous-reseau Docker exact utilise exclusivement par Traefik pour joindre le
frontend. Le relever sur le serveur avec `docker network inspect <reseau-traefik>` ; ne jamais utiliser l'ensemble des
plages privees (`10.0.0.0/8`, `172.16.0.0/12` ou `192.168.0.0/16`). La valeur de repli `127.0.0.1/32` ignore les
adresses transmises et reste sure, mais tous les clients apparaissent alors comme le proxy pour la limite par client.

Le pool configure 20 connexions PostgreSQL par replique backend. Avant d'augmenter le nombre de repliques, verifier
que leur total, les connexions d'administration et la marge de maintenance restent sous `max_connections`.

### QR code permanent Olifan

Le QR code imprime doit pointer vers `https://go.pixsom.fr/olifan`, et non directement vers l'application. Le frontend
repond sur ce domaine par une redirection HTTP 302 non mise en cache vers `OLIFAN_REDIRECT_URL`, dont la valeur par
defaut est `https://olifan.pixsom.fr`. La destination peut ainsi changer sans reimprimer le QR code.

1. Creer un enregistrement DNS `go.pixsom.fr` pointant vers le VPS Dokploy.
2. Dans les domaines du Compose Dokploy, associer `go.pixsom.fr` au service `frontend`, port `8080`, avec HTTPS et
   un certificat Let's Encrypt.
3. Definir `OLIFAN_REDIRECT_URL=https://olifan.pixsom.fr` dans l'environnement du Compose.
4. Apres deploiement, verifier `https://go.pixsom.fr/olifan` depuis plusieurs telephones avant de generer les fichiers
   d'impression.

Pour changer la destination, modifier uniquement `OLIFAN_REDIRECT_URL` dans Dokploy puis redeployer le Compose. Ne
jamais changer l'URL encodee dans le QR deja imprime.

Pour Olifan, configurer une seule fois les images suivantes dans l'environnement du Compose Dokploy :

```dotenv
BACKEND_IMAGE=ghcr.io/arnaudfernandez/racebets/backend:olifan
FRONTEND_IMAGE=ghcr.io/arnaudfernandez/racebets/frontend:olifan
```

Le fichier Compose impose `pull_policy: always`. Chaque push sur `olifan-group` publie d'abord les deux alias
`olifan`, puis appelle Dokploy : le deploiement ne peut donc pas commencer avec une seule des deux nouvelles images.
Les tags immuables `olifan-<sha-complet>` restent disponibles pour un retour arriere.

Desactiver le declenchement Dokploy `On Push` pour eviter un deploiement concurrent avant la fin des builds. Creer
dans GitHub un environnement nomme `olifan`, puis y definir :

- variable `DOKPLOY_URL` : URL HTTPS de l'instance Dokploy, sans chemin d'API ;
- variable `DOKPLOY_OLIFAN_COMPOSE_ID` : identifiant du Compose Olifan ;
- secret `DOKPLOY_API_TOKEN` : token Dokploy dedie a la CI.

Le job appelle `POST /api/compose.deploy` apres les tests, scans et publications. Il emet un avertissement et ignore
le deploiement tant que cette configuration est incomplete. Ne jamais exposer le token dans une variable non secrete
ou dans le depot.

Les conteneurs applicatifs s'executent sans privileges, avec un systeme de fichiers en lecture seule, toutes les
capabilities Linux supprimees, un `/tmp` borne, des healthchecks et une rotation des journaux.

## Promotion vers la production

1. Proteger `main` et `staging` dans GitHub : pull request obligatoire, workflow `Build and publish containers`
   obligatoire, branche a jour et au moins une approbation. Interdire les pushes forces et la suppression.
2. Valider la version candidate sur staging, y compris le workflow de course complet sur mobile et ordinateur.
3. Creer une sauvegarde PostgreSQL staging et verifier qu'elle est restaurable.
4. Ouvrir une pull request de `staging` vers `main`, attendre tous les controles et fusionner sans push direct.
5. Dans l'execution GitHub Actions du commit `main`, relever le SHA complet et verifier que le job de publication est
   vert.
6. Dans le Compose Dokploy de production, renseigner `BACKEND_IMAGE` et `FRONTEND_IMAGE` avec les deux tags
   `prod-<meme-sha>`. Ne pas utiliser l'alias mobile `prod`.
7. Renseigner les secrets de production distincts de la staging, garder le bootstrap administrateur desactive et
   associer uniquement le domaine HTTPS au service `frontend` sur le port 8080.
8. Lancer le deploiement et surveiller successivement PostgreSQL `healthy`, backend `healthy`, puis frontend
   `healthy`. Verifier les logs Flyway avant d'ouvrir le trafic.
9. Effectuer les tests de fumee : accueil, inscription/connexion, affichage live, vote, administration et chargement
   d'un logo partenaire. Verifier aussi les en-tetes de securite dans le navigateur.
10. Conserver le SHA precedent et la sauvegarde associee jusqu'a la fin de la periode d'observation.

## Sauvegardes

Configurer une destination S3 hors du VPS, une sauvegarde PostgreSQL quotidienne, une retention d'au moins sept
jours et lancer un test manuel. Tester aussi une restauration avant l'ouverture publique. Le volume nomme
`postgres_data` persiste entre les deploiements mais ne remplace jamais une sauvegarde externe.

Avant chaque migration, produire en plus un dump logique avec `pg_dump` au format custom. Une sauvegarde n'est
consideree valide qu'apres un test de restauration dans une base distincte.
