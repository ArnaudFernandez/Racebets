# Architecture Logicielle

## Vision Generale

L'application est construite par vertical slicing : chaque lot ajoute une capacite metier exploitable et testable. Les couches doivent rester simples et explicites.

Architecture backend actuelle :

```text
Controller REST
-> Service applicatif
-> Repository Spring Data JPA
-> Entites JPA
```

La securite HTTP est centralisee dans `security.SecurityConfig`. La generation JWT est centralisee dans `security.JwtService`.

## Packages Actuels

```text
com.pixsom.racebets
com.pixsom.racebets.admin
com.pixsom.racebets.admin.horse
com.pixsom.racebets.admin.horse.dto
com.pixsom.racebets.admin.race
com.pixsom.racebets.admin.race.dto
com.pixsom.racebets.admin.raceentry
com.pixsom.racebets.admin.raceentry.dto
com.pixsom.racebets.auth
com.pixsom.racebets.auth.dto
com.pixsom.racebets.config
com.pixsom.racebets.security
com.pixsom.racebets.repositories
com.pixsom.racebets.entities
com.pixsom.racebets.entities.common
com.pixsom.racebets.enums
```

## Modele Domaine Valide

### AppUser

Entite utilisateur applicatif.

Table : `app_user`.

Points importants :

- Le nom Java est singulier et evite la collision avec `org.springframework.security.core.userdetails.User`.
- Les roles sont stockes en `@ElementCollection` avec `EnumType.STRING`.
- Le secret d'acces est stocke sous forme de `passwordHash`, jamais en clair.
- L'authentification actuelle utilise `email + accessCode`, avec verification BCrypt.

### Race

Entite course.

Table : `races`.

Ne doit pas contenir directement `winnerHorseId`. Le resultat appartient a la participation, pas a la course seule.

### Horse

Entite cheval.

Table : `horses`.

Le cheval n'est jamais gagnant dans l'absolu. Il a un classement uniquement via une participation a une course.

### RaceEntry

Entite de jointure forte entre `Race` et `Horse`.

Table : `race_entries`.

Elle porte :

- `race`
- `horse`
- `horseNumber`
- `rank`

Contraintes uniques validees :

```text
(race_id, horse_id)
(race_id, horse_number)
```

Justification : un meme cheval ne peut pas etre inscrit deux fois a la meme course, et deux chevaux ne peuvent pas avoir le meme dossard dans une course.

`rank` est `Integer`, pas `int`, pour representer une absence de classement avant la fin de course.

### Bet

Entite pari.

Table : `bets`.

Un pari reference `RaceEntry`, pas `Race` + `Horse` separement.

Justification : le pari porte sur un cheval dans le contexte d'une course. Referencer `Race` et `Horse` separement autoriserait des incoherences comme parier sur un cheval non inscrit a la course.

## Audit JPA

Classe commune : `entities.common.AuditableEntity`.

Elle contient :

- `createdAt`
- `updatedAt`

Annotations :

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
```

`@MappedSuperclass` ne cree pas de table. Les colonnes sont integrees directement dans les tables filles.

`@EntityListeners(AuditingEntityListener.class)` branche l'audit Spring Data JPA sur les evenements de persistence.

`@EnableJpaAuditing` est active une seule fois dans `RacebetsApplication`.

## Regles JPA Validees

- Toujours `@Enumerated(EnumType.STRING)` pour les enums persistantes.
- Jamais `EnumType.ORDINAL`.
- Relations `@ManyToOne` en `FetchType.LAZY`.
- Relations obligatoires avec `optional = false` et `@JoinColumn(nullable = false)`.
- Noms de tables explicites avec `@Table`.
- Contraintes metier importantes au niveau SQL via `@Column`, `@UniqueConstraint`, `nullable = false`.
- Pas de `@Data` sur les entites.
- Pas de `@ToString` global sur les entites.
- Eviter les relations bidirectionnelles tant qu'elles ne sont pas necessaires.

## Securite Actuelle

### Flux Login

Endpoint :

```http
POST /api/auth/login
```

Request :

```json
{
  "email": "bettor@example.com",
  "accessCode": "DUPJEA"
}
```

Validation :

- `email` : `@NotBlank`, `@Email`.
- `accessCode` : `@NotBlank`.

Service : `AuthService`.

Etapes :

1. Recherche utilisateur par email.
2. Si absent : `BadCredentialsException("Invalid credentials")`.
3. Verification `PasswordEncoder.matches(accessCode, passwordHash)`.
4. Si invalide : meme exception generique.
5. Generation JWT via `JwtService`.
6. Retour `LoginResponse`.

### JWT

Service : `JwtService`.

Signature : HS256.

Beans JWT : `SecurityConfig`.

Claims generes :

- `sub` : email utilisateur.
- `roles` : liste des roles enum en string.
- `userId` : identifiant technique utilisateur.
- `iat` : issued at.
- `exp` : expiration.

Le serveur est stateless :

```java
SessionCreationPolicy.STATELESS
```

### SecurityConfig

Regles actuelles :

- `/api/auth/login` : public.
- `/api/admin/**` : authentifie avec role `ADMIN`.
- Tout le reste : authentifie.
- CSRF desactive pour API stateless.
- Resource Server JWT active.
- Le claim JWT custom `roles` est converti en authorities Spring `ROLE_*` via `JwtAuthenticationConverter`.

Note Spring Security 7 : une authority additionnelle `FACTOR_BEARER` peut etre presente sur les authentifications bearer. Les tests ne doivent pas supposer que les roles metier sont les seules authorities.

### Seeder ADMIN Local

Pour permettre une validation locale sans manipuler manuellement la base, `DevAdminUserSeeder` cree ou met a jour un utilisateur `ADMIN` seulement si `racebets.dev-admin.enabled=true`.

Configuration par defaut :

```properties
racebets.dev-admin.enabled=false
```

Profil local :

```properties
racebets.dev-admin.enabled=true
racebets.dev-admin.email=admin@racebets.local
racebets.dev-admin.access-code=ADMIN-LOCAL-2026
```

Le seeder encode toujours le code d'acces avec BCrypt et ne doit pas etre active en production.

## Lot 2 Admin REST

### CRUD Horse

Endpoints admin :

```http
GET    /api/admin/horses
GET    /api/admin/horses/{id}
POST   /api/admin/horses
PUT    /api/admin/horses/{id}
DELETE /api/admin/horses/{id}
```

Package : `admin.horse`.

Structure :

- `HorseAdminController` expose le contrat HTTP.
- `HorseService` porte le cas d'usage CRUD.
- `HorseRepository` etend `JpaRepository<Horse, Long>`.
- `HorseRequest` valide `name` avec `@NotBlank` et `@Size(max = 100)`.
- `HorseResponse` expose seulement `id` et `name`.
- `NotFoundException` est traduite en `404` via `AdminExceptionHandler`.

Choix important : l'API admin n'expose pas directement l'entite JPA `Horse`, meme si elle est simple aujourd'hui. Cela garde un contrat HTTP stable pour les prochains champs et evite de normaliser une mauvaise habitude pour `Race` et `RaceEntry`.

### CRUD Race

Endpoints admin :

```http
GET    /api/admin/races
GET    /api/admin/races/{id}
POST   /api/admin/races
PUT    /api/admin/races/{id}
DELETE /api/admin/races/{id}
```

Package : `admin.race`.

Structure :

- `RaceAdminController` expose le contrat HTTP.
- `RaceService` porte le cas d'usage CRUD.
- `RaceRepository` etend `JpaRepository<Race, Long>`.
- `RaceRequest` valide `name` avec `@NotBlank` et `@Size(max = 120)`, `raceImgUrl` avec `@Size(max = 500)` et accepte `state` optionnel.
- `RaceResponse` expose `id`, `name`, `raceImgUrl`, `state`.
- Creation : si `state` est absent, l'etat par defaut est `CREATED`.
- Mise a jour : si `state` est absent, l'etat existant est conserve.

Choix important : `Race` ne contient toujours pas de gagnant. Le resultat et le classement restent portes par `RaceEntry`.

### CRUD RaceEntry

Endpoints admin :

```http
GET    /api/admin/race-entries
GET    /api/admin/race-entries?raceId={raceId}
GET    /api/admin/race-entries/{id}
POST   /api/admin/race-entries
PUT    /api/admin/race-entries/{id}
DELETE /api/admin/race-entries/{id}
```

Package : `admin.raceentry`.

Structure :

- `RaceEntryAdminController` expose le contrat HTTP.
- `RaceEntryService` porte les cas d'usage et les invariants metier.
- `RaceEntryRepository` etend `JpaRepository<RaceEntry, Long>` et expose les recherches d'unicite.
- `RaceEntryRequest` exige `raceId`, `horseId`, `horseNumber > 0`; `rank` est optionnel mais positif si present.
- `RaceEntryResponse` expose `id`, `raceId`, `raceName`, `horseId`, `horseName`, `horseNumber`, `rank`.
- `ConflictException` est traduite en HTTP `409 Conflict` quand un invariant d'inscription est viole.

Invariants verifies avant sauvegarde :

- un meme cheval ne peut pas etre inscrit deux fois a la meme course ;
- deux chevaux ne peuvent pas partager le meme numero de dossard dans une course.

Les contraintes SQL uniques restent presentes sur `race_entries` comme filet de securite : `(race_id, horse_id)` et `(race_id, horse_number)`.

## Frontend Actuel

Le dossier `frontend/` contient une application Angular 19 standalone avec Taiga UI v5 et NgRx Signals.

Elements presents :

- Route racine lazy-loadee vers `LiveBettingBoardComponent`.
- Route `/admin` lazy-loadee vers `AdminDashboardComponent`.
- Route `/login` lazy-loadee vers `LoginPageComponent`.
- `BettingSignalStore` avec `@ngrx/signals` pour les partants par course et le pari utilisateur local.
- `SseService` conserve son nom historique mais charge maintenant un snapshot JSON one-shot depuis `/api/realtime/race-betting` en attendant le vrai WebSocket Lot 3.
- `RealtimeCardComponent` partage avec `input()` et `ChangeDetectionStrategy.OnPush`.
- `AuthService` gere le login frontend, la session JWT locale et l'expiration.
- `authTokenInterceptor` ajoute `Authorization: Bearer ...` aux appels `/api/**` hors `/api/auth/login`.
- `adminGuard` protege la route `/admin` en exigeant le role `ADMIN` cote frontend.
- `AdminApiService` fournit les appels HTTP typés vers `/api/admin/horses`, `/api/admin/races`, `/api/admin/race-entries`.
- `AdminDashboardComponent` utilise Reactive Forms pour gerer `Horse`, `Race` et `RaceEntry`.
- L'UX admin affiche des erreurs backend differenciees (`401/403`, `404`, `409`), des libelles de chargement et une confirmation explicite avant suppression.
- Modeles front actuels sous `features/races/models` et `features/betting/models`.

Point important : la session frontend est actuellement persistee en `localStorage` avec expiration calculee depuis `expiresIn`. C'est acceptable pour cette etape projet, mais le choix devra etre revalide avant production selon le modele de menace.

Regle : ne pas toucher aux composants Taiga UI sans consulter la documentation MCP Taiga UI (`get_component_example`) pour la v5.

## Tests Actuels

Tests ajoutes :

- `AuthServiceTest`
- `AuthControllerTest`
- `JwtServiceTest`
- `SecurityConfigTest`
- `HorseServiceTest`
- `HorseAdminControllerTest`
- `RaceServiceTest`
- `RaceAdminControllerTest`
- `RaceEntryServiceTest`
- `RaceEntryAdminControllerTest`
- `DevAdminUserSeederTest`
- `AdminEndToEndTest`
- `RacebetsApplicationTests`

Couverture comportementale :

- Login valide.
- Email inconnu.
- Code invalide.
- Validation DTO invalide.
- Mauvais credentials en HTTP 401.
- Token JWT signe avec HS256 et claims attendus.
- Mapping JWT `roles` vers authorities `ROLE_*`.
- Protection `/api/admin/**` : anonyme refuse, utilisateur non-admin refuse, admin accepte.
- CRUD backend `Horse` : liste, creation, mise a jour, suppression, validation payload, 404.
- CRUD backend `Race` : liste, creation, etat par defaut, conservation/changement d'etat, mise a jour, suppression, validation payload, 404.
- CRUD backend `RaceEntry` : liste globale, liste filtree par course, creation, mise a jour, suppression, validation payload, 404, conflits metier en 409.
- Seeder ADMIN local : desactive si non configure, creation admin si active, hash BCrypt, ajout du role `ADMIN` si l'utilisateur existe deja.
- Integration admin : login avec l'utilisateur local seede, recuperation d'un JWT signe par le backend, acces autorise a `/api/admin/horses`.
- Frontend admin/auth : build Angular et lint passent avec routes lazy-loadees, login, guard admin et interceptor JWT.
