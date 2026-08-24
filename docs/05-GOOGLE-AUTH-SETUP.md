# Configuration de l'authentification Google

## Fonctionnement applicatif

- Google authentifie l'utilisateur avec OpenID Connect Authorization Code et PKCE.
- RaceBets n'utilise jamais l'email comme identifiant Google durable. La liaison repose sur `issuer + subject` dans `oauth_identity`.
- Un nouvel utilisateur Google cree un `app_user` RaceBets classique avec le role `USER`. Ses paris, quiz, roles et donnees restent donc geres comme pour tout autre compte.
- Si l'email Google correspond a un compte local existant qui n'est pas encore lie, RaceBets demande une seule fois le code d'acces local. Cela empeche qu'un tiers lie un compte precree avec l'email d'une autre personne.
- Les connexions Google suivantes retrouvent directement le meme `app_user`, meme si l'adresse Google change ensuite.
- Le callback ne transmet jamais le JWT RaceBets dans l'URL. Il remet un code aleatoire de 256 bits, stocke uniquement sous forme hachee, valable une minute et consommable une seule fois.

## 1. Creer le projet Google Cloud

1. Ouvrir `https://console.cloud.google.com/` et selectionner ou creer le projet Google Cloud de RaceBets.
2. Ouvrir `Google Auth Platform`. Selon l'interface Google affichee, ces ecrans peuvent aussi se trouver sous `API et services` puis `Ecran de consentement OAuth` et `Identifiants`.
3. Dans `Branding`, renseigner le nom de l'application `RaceBets`, l'adresse de support et l'adresse de contact developpeur.
4. Dans `Audience`, choisir `External` pour autoriser les comptes Google hors de votre organisation. Choisir `Internal` uniquement si tous les utilisateurs appartiennent au meme Google Workspace.
5. Tant que l'application est en mode test, ajouter dans `Test users` chaque adresse Google autorisee a tester.
6. Dans `Data Access`, conserver uniquement les scopes standards `openid`, `.../auth/userinfo.email` et `.../auth/userinfo.profile`. Aucun scope sensible n'est necessaire.

## 2. Configuration locale

### Creer le client local

1. Dans `Google Auth Platform` puis `Clients`, cliquer sur `Create client`.
2. Choisir `Web application`.
3. Nommer le client `RaceBets local`.
4. Dans `Authorized redirect URIs`, ajouter exactement :

```text
http://localhost:4200/api/auth/google/callback/google
```

5. Il n'est pas necessaire d'ajouter une origine JavaScript : le flux est traite par le backend, sans SDK Google dans Angular.
6. Copier le `Client ID` et le `Client secret` affiches par Google.

### Lancer RaceBets en local

Utiliser Java 25, puis definir les secrets uniquement dans le terminal qui lance le backend :

```powershell
$env:GOOGLE_AUTH_ENABLED='true'
$env:GOOGLE_CLIENT_ID='<client-id-local>.apps.googleusercontent.com'
$env:GOOGLE_CLIENT_SECRET='<client-secret-local>'
$env:GOOGLE_LOGIN_CODE_TTL='PT1M'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Dans un second terminal :

```powershell
npm --prefix frontend start
```

Ouvrir exclusivement `http://localhost:4200/login`. Le proxy Angular transmet les routes `/api` au backend et permet a Spring de calculer le callback Google avec le port `4200`.

Verifier avant de tester que `http://localhost:4200/api/auth/providers` renvoie :

```json
{"google":true}
```

## 3. Configuration de production

### Creer un client de production separe

1. Creer un nouveau client OAuth `Web application`, nomme par exemple `RaceBets production`.
2. Dans `Authorized redirect URIs`, ajouter l'URL exacte du domaine public, sans slash final :

```text
https://<domaine-racebets>/api/auth/google/callback/google
```

Exemple :

```text
https://racebets.example.com/api/auth/google/callback/google
```

3. Ne jamais utiliser le client local en production. Pour le staging, creer idealement un troisieme client et enregistrer `https://<domaine-staging>/api/auth/google/callback/google`.
4. Ajouter les domaines publics dans `Branding` si Google le demande. Le domaine racine peut devoir etre verifie dans Google Search Console.
5. Quand les tests sont termines, passer l'application de `Testing` a `In production` dans `Audience`. Les scopes `openid`, `email` et `profile` ne demandent normalement pas de validation de scopes sensibles, mais Google peut demander de completer la marque et les domaines.

### Variables Dokploy

Ajouter ces variables dans l'environnement du compose de production :

```dotenv
GOOGLE_AUTH_ENABLED=true
GOOGLE_CLIENT_ID=<client-id-production>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<client-secret-production>
GOOGLE_LOGIN_CODE_TTL=PT1M
```

Puis redeployer le backend et le frontend. Flyway appliquera automatiquement `V3__google_authentication.sql`. Ne pas lancer la migration manuellement et conserver `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

Le reverse proxy doit continuer a transmettre `X-Forwarded-Host` et `X-Forwarded-Proto`; la configuration Nginx du depot le fait deja. Le site de production doit obligatoirement etre servi en HTTPS.

## 4. Verification fonctionnelle

1. Tester une adresse Google absente de RaceBets : un nouvel utilisateur `USER` doit etre cree et connecte.
2. Se deconnecter puis reutiliser Google : le meme identifiant RaceBets et le meme historique doivent etre retrouves.
3. Tester une adresse deja inscrite localement : RaceBets doit demander une fois le code d'acces local, puis reutiliser ce compte et son historique.
4. Verifier qu'un mauvais code local refuse la liaison.
5. Verifier que la connexion locale par email et code continue de fonctionner pour les comptes locaux.
6. Verifier qu'une annulation sur l'ecran Google revient a `/login` avec un message generique.

## 5. Exploitation et secrets

- Ne jamais committer ni exposer `GOOGLE_CLIENT_SECRET` au frontend.
- Utiliser un client et un secret differents pour local, staging et production.
- En cas de fuite, creer un nouveau secret dans Google Cloud, le deployer, puis supprimer l'ancien.
- Supprimer les URI de callback inutilisees et limiter les utilisateurs de test tant que l'application n'est pas publiee.
- Laisser `GOOGLE_LOGIN_CODE_TTL=PT1M`; l'application refuse les durees inferieures a 30 secondes ou superieures a 5 minutes.
