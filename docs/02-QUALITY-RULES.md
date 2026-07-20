# Regles De Qualite Et Conventions

Ce fichier est normatif. Toute future session doit respecter ces regles sauf demande explicite contraire.

## Principes Generaux

- Faire les changements minimaux corrects.
- Ne pas introduire d'over-engineering.
- Ne pas ajouter de couche abstraite sans usage concret.
- Toujours proteger les invariants metier au plus pres du domaine.
- Toujours preferer un code testable et explicite.
- Ne pas cacher les erreurs par des `return null` ou des exceptions generiques.

## Backend Spring

### Controllers

Les controllers doivent :

- exposer le contrat HTTP uniquement ;
- recevoir des DTO ;
- deleguer la logique au service ;
- ne pas acceder directement aux repositories ;
- utiliser `@Valid` sur les requetes entrantes validees ;
- ne pas contenir de logique metier.

### Services

Les services doivent :

- porter les cas d'usage applicatifs ;
- orchestrer repositories, securite et generation de reponses ;
- lever des exceptions metier ou techniques precises ;
- ne pas retourner `null` ;
- rester petits et testables.

### Repositories

Les repositories Spring Data doivent etre des interfaces.

Exemple valide :

```java
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
}
```

### DTO

Pour les DTO simples, utiliser des `record` Java.

Exemple :

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String accessCode
) {
}
```

Ne jamais exposer directement les entites JPA en reponse API publique si l'entite contient relations, secrets ou champs internes.

### Exceptions

Ne pas utiliser `throws Exception` dans les services/controllers.

Pour l'authentification :

- utiliser `BadCredentialsException` pour credentials invalides ;
- renvoyer un message generique `Invalid credentials` ;
- ne pas permettre l'enumeration d'emails.

Les exceptions REST doivent etre traduites via `@RestControllerAdvice`.

Pour les conflits metier previsibles dans l'admin, utiliser une exception explicite traduite en `409 Conflict`, pas une erreur SQL brute.

## Securite

### Passwords Et Codes

- Ne jamais stocker un mot de passe ou access code en clair.
- Utiliser `PasswordEncoder.matches(raw, encoded)`.
- Ne jamais comparer un hash BCrypt avec `equals`.
- Le champ persistant s'appelle `passwordHash`, pas `password`.

### JWT

- Signature actuelle : HS256.
- La cle doit etre fournie par configuration.
- Dans tous les environnements hors test, `JWT_SECRET` est une variable d'environnement obligatoire.
- Le secret doit contenir au minimum 32 octets et ne doit jamais etre versionne.
- Ne pas creer un deuxieme utilitaire JWT concurrent. Le service officiel est `JwtService`.
- Le token doit contenir le minimum necessaire : `sub`, `userId`, `roles`, `iat`, `exp`.

### Sessions

L'API JWT est stateless. Toujours garder :

```java
SessionCreationPolicy.STATELESS
```

### Roles

Les roles sont dans le JWT et sont mappes en authorities Spring via `JwtAuthenticationConverter`.

Regle actuelle : le claim custom `roles` doit produire des authorities `ROLE_*`, par exemple `ADMIN` -> `ROLE_ADMIN`.

Ne pas utiliser `hasRole` ou `@PreAuthorize` sur un nouveau role sans verifier que le token emet bien la valeur attendue dans `roles`.

## JPA Et Domaine

### Entites

- Pas de `@Data`.
- Pas de `@ToString` global.
- `@Getter` est acceptable.
- `@Setter` au niveau classe est tolere temporairement, mais a reduire progressivement.
- Les methodes metier doivent remplacer les setters sensibles.

### Relations

- `@ManyToOne(fetch = FetchType.LAZY)` par defaut.
- Ajouter `optional = false` si relation obligatoire.
- Ajouter `@JoinColumn(nullable = false)` si la FK est obligatoire en base.
- Ne pas creer de relations bidirectionnelles sans besoin reel.

### Enums

Toujours :

```java
@Enumerated(EnumType.STRING)
```

Jamais `ORDINAL`.

### Audit

Tous les objets critiques doivent heriter de `AuditableEntity`.

`createdAt` est non updatable.

`updatedAt` est maintenu par Spring Data JPA.

## Tests

Les tests ne sont pas un sujet d'apprentissage pour l'utilisateur dans ce projet, mais ils doivent rester maintenus par l'assistant.

Regles :

- Ajouter des tests pour tout comportement backend non trivial.
- Tester les cas heureux et les cas d'erreur.
- Pour les services : tests unitaires avec Mockito.
- Pour les controllers simples : `MockMvcBuilders.standaloneSetup` est accepte.
- Pour le contexte global : conserver `RacebetsApplicationTests`.
- Lancer `mvnw test` avec JDK 25 apres modifications significatives.

Mockito est configure en `javaagent` via Surefire. Ne pas supprimer cette configuration sans raison.

## Frontend Angular

Regles actuelles pour `frontend/` :

- Angular 21 en standalone components.
- `ChangeDetectionStrategy.OnPush` obligatoire sur les composants.
- Preferer `input()`, `output()`, `viewChild()` et `computed()` aux APIs decorateur historiques.
- Utiliser `@ngrx/signals` pour l'etat local applicatif structure.
- Eviter RxJS lourd dans les composants ; convertir les flux externes en signals dans les services quand c'est pertinent.
- Aucun `any` non justifie.
- Pour Taiga UI v5, consulter la documentation MCP Taiga UI avant d'ajouter ou modifier un composant UI significatif.
- Lancer `npm run build` dans `frontend/` apres une modification frontend non triviale.

## Build Et Environnement

Le projet compile avec Java 25. Un terminal en Java 23 echouera avec :

```text
release version 25 not supported
```

Toujours verifier :

```powershell
java -version
.\mvnw.cmd -version
```

Commande standard :

```powershell
$env:JAVA_HOME='C:\Users\Monsieur PC VERYFAST\.jdks\jbr-25.0.3'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

## Definition Of Done Par Lot

Un lot est valide uniquement si :

- le code compile ;
- les tests passent ;
- les choix sont coherents avec l'architecture ;
- les erreurs bloquantes et majeures ont ete corrigees ;
- une fiche d'entretien est fournie a l'utilisateur.
