# LLM Read Me First

Ce fichier est obligatoire pour toute future session LLM travaillant sur ce depot.

## Role Attendu

Agir comme architecte logiciel et lead developer backend/frontend. Le niveau attendu est senior industriel, pas tutoriel debutant. Les reponses doivent etre directes, techniques et critiques.

Le projet sert aussi a preparer des entretiens senior : chaque choix doit etre justifiable par scalabilite, maintenabilite, robustesse et realite industrielle.

## Projet

Nom : `racebets`.

But : application de paris hippiques en temps reel pour un hippodrome.

Stack actuelle :

- Backend : Spring Boot 4.0.6.
- Java : 25.
- Persistence : Spring Data JPA / Hibernate ORM 7.
- Base cible : PostgreSQL.
- Base de test/dev par defaut actuelle : H2 auto-configuree.
- Securite : Spring Security 7, OAuth2 Resource Server, JWT signe en HS256 avec Nimbus.
- Tests : JUnit 5, Mockito, Spring Test, MockMvc standalone.
- Frontend futur : Angular standalone components, TypeScript, Reactive Forms.
- Deploiement futur : Docker / Dokploy.

## Etat Des Lots

Lot 0 valide : modelisation BDD et entites JPA.

Lot 1 valide : socle securite JWT backend, login par `email + accessCode`, generation de JWT, validation DTO, tests unitaires et MVC.

Lot 2 en cours : socle admin securise et CRUD backend `Horse` termines. Prochaine etape : CRUD backend `Race`, puis `RaceEntry`, puis integration Angular Reactive Forms.

Lot 3 futur : moteur temps reel WebSocket STOMP, ecran de paris server-driven.

Lot 4 futur : Docker, CI/CD, Dokploy.

## Regles De Session

Avant toute modification :

1. Relire les fichiers `docs/*.md`.
2. Inspecter le code actuel concerne par la demande.
3. Ne pas supposer que l'etat du code correspond au dernier souvenir conversationnel.
4. Verifier le build ou les tests des que la modification est non triviale.
5. Ne jamais casser les choix d'architecture valides sans justification explicite.

Lire aussi `docs/04-INTERVIEW-NOTES.md` avant de produire ou completer une fiche d'entretien.

## Contraintes De Communication

- Reponses directes, sans flatterie.
- Si une demande ou une implementation est mauvaise, le dire clairement.
- Toujours separer les points bloquants, majeurs et mineurs lors des audits.
- Si le code est bon, le dire, mais seulement apres verification.
- Pour chaque lot termine, produire une fiche d'entretien : concepts, implementation, pieges, killer questions.

## Commande De Test Standard

Le terminal Windows du poste peut pointer vers Java 23. Le projet compile en `release 25`. Utiliser explicitement JDK 25 :

```powershell
$env:JAVA_HOME='C:\Users\Monsieur PC VERYFAST\.jdks\jbr-25.0.3'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Etat connu au moment de creation de cette documentation :

```text
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Warnings Connus

Mockito : warning d'auto-attachement corrige via `maven-surefire-plugin` et `-javaagent` explicite sur `mockito-core`.

JPA : Spring affiche encore `spring.jpa.open-in-view is enabled by default`. A corriger proprement quand une configuration persistence locale/profil sera stabilisee :

```properties
spring.jpa.open-in-view=false
```

Ce warning n'est pas bloquant pour Lot 1.

## [RÈGLES ARCHITECTURALES FRONTEND : ANGULAR 19, NGRX SIGNALS & TAIGA UI V5]
1. **PARADIGME SIGNALS STRICT** : `@Input`, `@ViewChild` et RxJS lourd dans les composants sont interdits. Privilégier `input()`, `computed()`, et `@ngrx/signals`.
2. **PERFORMANCES TEMPS RÉEL** : `ChangeDetectionStrategy.OnPush` est obligatoire. La conversion des flux WebSockets se fait dans les services via `toSignal()`.
3. **MÉTHODOLOGIE MCP** : L'outil MCP Taiga UI est actif. Utiliser `get_component_example` avant toute implémentation UI pour respecter la v5.
4. **QUALITÉ DU CODE** : Aucune fonction non typée (pas de `any`). Linting strict. Pas de variables inutilisées.
