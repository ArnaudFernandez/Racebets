# Frontend Racebets

Application Angular 19 standalone utilisant Signals, NgRx Signals et Taiga UI v5. Node.js 22 est la version utilisee par la CI et l'image Docker.

## Routes

- `/` : tableau des courses et paris fictifs, protege par le feature flag `bettingEnabled`.
- `/login` : connexion et inscription.
- `/quiz` : participation aux quiz, protegee par le feature flag `quizEnabled`.
- `/admin` : administration reservee au role `ADMIN`.

L'administration couvre les utilisateurs, les chevaux, les courses, les participations, les quiz et les feature flags.

## Developpement

```bash
npm ci
npm start
```

`npm start` lance Angular sur `http://localhost:4200` avec `proxy.conf.json`. Les appels `/api` sont transmis au backend local sur `http://localhost:8080`.

## Commandes

```bash
npm run build
npm run lint
npm test
npm run format:check
```

Le build de production est genere sous `dist/frontend/browser`. Aucun framework de test end-to-end n'est configure actuellement.

## Architecture fonctionnelle

- `core/auth` : session JWT locale, guard admin et intercepteur bearer.
- `core/features` : chargement des feature flags et guards de routes.
- `core/realtime` : client SSE du tableau de courses avec reconnexion.
- `features/admin` : ecrans et API d'administration.
- `features/betting` : tableau de course et store NgRx Signals.
- `features/quiz` : administration et participation aux sessions de quiz.
- `shared/ui` : composants visuels reutilisables.

Le tableau de courses consomme `/api/realtime/race-betting/stream` avec `EventSource`. Le joueur de quiz actualise son snapshot REST toutes les deux secondes. La session JWT est conservee dans `localStorage`; cette strategie doit etre reevaluee avant une mise en production exposee.

La couverture frontend est encore minimale : seul le composant racine possede actuellement un test unitaire.
