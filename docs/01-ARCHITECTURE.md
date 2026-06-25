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
com.pixsom.racebets.auth
com.pixsom.racebets.auth.dto
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

## Tests Actuels

Tests ajoutes :

- `AuthServiceTest`
- `AuthControllerTest`
- `JwtServiceTest`
- `SecurityConfigTest`
- `HorseServiceTest`
- `HorseAdminControllerTest`
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
