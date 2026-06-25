# Roadmap Et Prochaines Etapes

## Lots Projet

### Lot 0 : Modelisation BDD Et Entites JPA

Statut : valide.

Livrables :

- Entites JPA : `AppUser`, `Race`, `Horse`, `RaceEntry`, `Bet`.
- Enums : `Role`, `RaceState`, `BetState`.
- Audit commun : `AuditableEntity`.
- Contraintes metier principales.

Points d'entretien deja couverts :

- Pourquoi `Bet` reference `RaceEntry`.
- Pourquoi `RaceEntry` est une entite forte.
- Pourquoi `EnumType.STRING`.
- Pourquoi eviter `@Data` sur JPA.
- Pourquoi `rank` est `Integer`.
- Difference `optional = false` vs `nullable = false`.
- Role de `@MappedSuperclass`.
- Role de `AuditingEntityListener`.
- Pourquoi `FetchType.LAZY`.

### Lot 1 : Securite Et Auth JWT

Statut : valide au niveau socle backend.

Livrables :

- `SecurityConfig`.
- `JwtService`.
- `AuthController`.
- `AuthService`.
- `AuthExceptionHandler`.
- DTO `LoginRequest`, `LoginResponse`.
- `AppUserRepository`.
- Tests : `AuthServiceTest`, `AuthControllerTest`, `JwtServiceTest`.

Fonctionnalites :

- Login public `POST /api/auth/login`.
- Validation payload login.
- Verification BCrypt du code d'acces.
- JWT HS256 signe avec Nimbus.
- API stateless.
- Credentials invalides en `401`.

Travail restant dans Lot 1 avant production :

- Ajouter mapping roles -> authorities via `JwtAuthenticationConverter` avant toute authorization par role.
- Externaliser `JWT_SECRET` via variable d'environnement.
- Ajouter un seed ou mecanisme de creation initiale d'utilisateurs avec `passwordHash` BCrypt.
- Eventuellement convertir `expiresIn` en secondes ou renommer en `expiresInMs`.

### Lot 2 : CRUD Admin REST Et Angular Reactive Forms

Statut : en cours.

Objectif : creer les APIs admin puis les ecrans Angular permettant de gerer les donnees necessaires au pari.

Backend REST a prevoir :

- CRUD `Horse` : termine cote backend.
- CRUD `Race`.
- Gestion des participations `RaceEntry`.
- Endpoints de consultation utiles aux ecrans admin.
- Protection des routes admin par role `ADMIN` : terminee.

Ordre recommande :

1. Ajouter mapping roles JWT -> authorities Spring : fait.
2. Proteger `/api/admin/**` par `ADMIN` : fait.
3. Creer DTO request/response pour `Horse` : fait.
4. Creer `HorseRepository` : fait.
5. Creer `HorseService` : fait.
6. Creer `HorseAdminController` : fait.
7. Tester controller/service : fait.
8. Repeter pour `Race` : prochaine etape.
9. Implementer `RaceEntry` avec contraintes metier fortes.
10. Ajouter Angular admin avec reactive forms.

Regle importante : ne pas exposer directement les entites JPA dans les reponses admin si cela cree des graphes ou fuites de champs internes.

### Lot 3 : Moteur Temps Reel WebSocket STOMP

Statut : futur.

Objectif : ecran de paris server-driven sans rechargement.

Principes valides :

- WebSocket STOMP obligatoire pour l'ecran de paris.
- Les transitions d'etat de course sont poussees par le serveur.
- Le frontend ne decide pas seul de l'etat de la course.
- Les evenements metier doivent etre explicites : ouverture paris, fermeture paris, resultat, mise a jour classement, etc.

Sujets futurs :

- Authentification du handshake WebSocket.
- Topics STOMP par course.
- Gestion des etats `RaceState`.
- Idempotence des messages.
- Synchronisation initiale REST + flux temps reel.

### Lot 4 : Docker / CI-CD / Dokploy

Statut : futur.

Objectif : packaging et deploiement.

Sujets :

- Dockerfile backend.
- Configuration PostgreSQL.
- Variables d'environnement : `JWT_SECRET`, datasource, profils.
- CI tests.
- Deploiement Dokploy.

## Prochaine Session Recommandee

Continuer Lot 2 :

1. Ajouter `spring.jpa.open-in-view=false` dans une configuration adaptee.
2. Creer CRUD admin backend `Race` avec DTO, repository, service, controller et tests.
3. Implementer `RaceEntry` avec contraintes metier fortes et tests.
4. Ajouter Angular admin avec reactive forms.
5. Produire la fiche d'entretien Lot 2 seulement quand le lot complet est valide.

## Checklist Avant Toute Nouvelle Feature

Avant de coder :

- Lire `docs/00-LLM-READ-ME-FIRST.md`.
- Lire `docs/01-ARCHITECTURE.md`.
- Lire `docs/02-QUALITY-RULES.md`.
- Lire ce fichier.
- Lire `docs/04-INTERVIEW-NOTES.md` si la session touche a une validation de lot ou une preparation entretien.
- Inspecter le code actuel, ne pas se fier uniquement a ces documents.
- Verifier si des tests doivent etre ajoutes ou mis a jour.

Apres avoir code :

- Lancer les tests avec JDK 25.
- Corriger warnings bloquants ou dettes apparues.
- Expliquer les changements et l'impact architectural.
