# Notes D'Entretien Senior

Ce fichier conserve les fiches de revision et les questions d'entretien liees aux lots valides.

## Lot 0 : JPA, Domaine Et Persistence

### Concepts A Maitriser

- Entite JPA, table, identite technique.
- Relation forte d'association via entite explicite.
- `@ManyToOne(fetch = FetchType.LAZY)`.
- `optional = false` vs `nullable = false`.
- `EnumType.STRING` vs `EnumType.ORDINAL`.
- Audit JPA avec `@MappedSuperclass`, `@CreatedDate`, `@LastModifiedDate`.
- Risques Lombok sur entites JPA.

### Reponse Senior Synthese

J'ai modelise la participation a une course comme une entite metier `RaceEntry`, car les donnees comme le numero de dossard et le classement ne decrivent ni le cheval seul, ni la course seule, mais leur association. Le pari reference donc cette participation pour eviter les incoherences metier. Les enums sont persistees en string, les relations sont lazy, les contraintes sont explicitees cote JPA et SQL, et l'audit est factorise via une `@MappedSuperclass`.

### Killer Questions Lot 0

Pourquoi `Bet` reference `RaceEntry` plutot que `Race` et `Horse` ?

Reponse : un pari porte sur un cheval dans une course donnee. `RaceEntry` garantit que le cheval est bien inscrit dans cette course. Referencer `Race` et `Horse` separement autoriserait des incoherences.

Pourquoi `RaceEntry` est une entite et pas une simple table de jointure ?

Reponse : la relation porte des attributs metier propres, comme `horseNumber` et `rank`. Des qu'une association porte ses propres donnees, elle devient une entite d'association forte.

Pourquoi `EnumType.STRING` est preferable a `ORDINAL` ?

Reponse : `ORDINAL` couple la base a l'ordre Java des valeurs enum. Ajouter ou reordonner une valeur peut corrompre le sens des donnees. `STRING` stocke la valeur metier lisible et stable.

Pourquoi eviter `@Data` sur une entite JPA ?

Reponse : `@Data` genere `equals`, `hashCode` et `toString` sur tous les champs, ce qui interagit mal avec les proxies Hibernate, les relations lazy et les associations bidirectionnelles.

Pourquoi `rank` est en `Integer` et pas en `int` ?

Reponse : avant la fin de course, le classement peut etre inconnu. `int` impose `0`, qui n'a pas de sens metier valide. `Integer` permet `null`.

Pourquoi `@EnableJpaAuditing` n'est pas dans les entites ?

Reponse : c'est une annotation de configuration Spring globale. Les entites exposent seulement les champs auditables et le listener.

Quelle difference entre `nullable = false` et `optional = false` ?

Reponse : `nullable = false` contraint la colonne SQL. `optional = false` contraint le modele JPA et informe Hibernate que l'association est obligatoire.

Pourquoi `@MappedSuperclass` ne cree pas de table ?

Reponse : elle factorise du mapping commun. Les colonnes sont heritees par les entites concretes, mais la classe mere n'est pas une entite persistable.

Pourquoi `createdAt` est `updatable = false` ?

Reponse : la date de creation est immuable. Hibernate ne doit jamais l'inclure dans les requetes `UPDATE`.

Pourquoi `FetchType.LAZY` sur les `@ManyToOne` ?

Reponse : pour eviter les chargements implicites de graphe objet. En production, on controle les donnees chargees via DTO, projections ou fetch joins cibles.

## Lot 1 : Spring Security Et JWT

### Concepts A Maitriser

- Spring Security filter chain.
- API stateless.
- CSRF et APIs REST.
- `PasswordEncoder` et BCrypt.
- JWT signe en HS256.
- `JwtEncoder` vs `JwtDecoder`.
- Resource Server JWT.
- Validation DTO avec Jakarta Bean Validation.
- Gestion d'erreurs REST via `@RestControllerAdvice`.

### Reponse Senior Synthese

Le backend expose un endpoint public de login qui verifie `email + accessCode` contre un `passwordHash` BCrypt. En cas de succes, il genere un JWT signe en HS256 avec Nimbus, contenant le subject, l'identifiant utilisateur, les roles, l'issued-at et l'expiration. L'API est stateless, le reste des endpoints est protege par Spring Security Resource Server, et les credentials invalides retournent une erreur generique pour eviter l'enumeration d'utilisateurs.

### Killer Questions Lot 1

Pourquoi utiliser `PasswordEncoder.matches` et pas `equals` ?

Reponse : BCrypt genere un hash sale. Le hash ne se compare jamais directement au secret brut. `matches` applique l'algorithme correctement avec le sel encode.

Pourquoi renvoyer le meme message pour email inconnu et code invalide ?

Reponse : pour eviter l'enumeration d'utilisateurs. L'attaquant ne doit pas savoir si l'email existe.

Pourquoi `SessionCreationPolicy.STATELESS` ?

Reponse : avec JWT, l'etat d'authentification est porte par le token. Le serveur ne doit pas creer de session HTTP.

Pourquoi CSRF est desactive ?

Reponse : l'API est stateless et authentifiee par bearer token. La protection CSRF cible surtout les sessions navigateur avec cookies automatiquement envoyes.

Difference entre `JwtEncoder` et `JwtDecoder` ?

Reponse : `JwtEncoder` signe et produit les tokens au login. `JwtDecoder` verifie les tokens recus sur les endpoints proteges.

Pourquoi HS256 ?

Reponse : c'est une signature symetrique simple et adaptee au prototype monolithique. La meme cle signe et verifie. En architecture distribuee, on pourrait preferer RS256 avec cle privee/publique.

Pourquoi ajouter `@Valid` sur le controller ?

Reponse : les annotations `@NotBlank` et `@Email` sur le DTO ne sont executees par Spring MVC que si le parametre est annote avec `@Valid`.

Pourquoi `@RestControllerAdvice` ?

Reponse : pour centraliser la traduction des exceptions techniques/metier en reponses HTTP coherentes, par exemple `BadCredentialsException` vers `401 Unauthorized`.

Pourquoi les roles dans le JWT ne suffisent pas pour `hasRole` ?

Reponse : Spring Security ne sait pas automatiquement convertir un claim custom `roles` en authorities `ROLE_*`. Il faut un `JwtAuthenticationConverter`.

## Lot 2 : Admin REST, Etat Intermediaire

Statut : non valide comme lot complet. Le socle admin et le CRUD backend `Horse` sont termines, mais `Race`, `RaceEntry` et Angular restent a faire.

### Concepts Deja Couverts

- Conversion d'un claim JWT custom `roles` en authorities Spring `ROLE_*`.
- Difference entre authentification valide et autorisation par role.
- Protection route-prefix `/api/admin/**` avec `hasRole("ADMIN")`.
- CRUD REST admin avec DTO request/response, service, repository.
- Validation d'entree avec `@NotBlank` et `@Size`.
- Traduction d'une absence de ressource en HTTP `404` via `@RestControllerAdvice`.

### Points A Retenir

Le CRUD `Horse` expose des DTO et non l'entite JPA, meme si l'entite est simple. Ce choix evite de coupler le contrat HTTP au modele de persistence et prepare les CRUD plus riches comme `Race` et `RaceEntry`.

Spring Security 7 peut ajouter une authority technique comme `FACTOR_BEARER`. Les tests doivent verifier la presence des authorities metier attendues sans supposer qu'elles sont les seules.
