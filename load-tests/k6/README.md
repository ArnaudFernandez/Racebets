# Tests de charge Olifan

Ces scripts ciblent `https://olifan.pixsom.fr` par défaut.

## Fichiers

- `generate-users.ps1` génère le CSV importable par l'administration.
- `login-storm.js` envoie une connexion simultanée par utilisateur.
- `quiz-load.js` connecte progressivement les comptes, rejoint le quiz, reproduit le polling Angular et répond de façon synchronisée trois secondes après l'ouverture de chaque question.
- `cleanup-users.ps1` supprime uniquement les comptes dont l'email se termine par `@loadtest.invalid`, après confirmation explicite.
- `users.csv` est local et ignoré par Git.

## Préparation

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\load-tests\k6\generate-users.ps1 -Count 150
winget install k6 --source winget
```

Importer ensuite `load-tests/k6/users.csv` depuis `Administration > Utilisateurs` et activer temporairement la connexion participant sans mot de passe depuis `Administration > App branding`.

## Exécution prudente

Lancer les commandes depuis la racine du dépôt.

```powershell
k6 run -e VUS=5 -e DURATION=2m .\load-tests\k6\quiz-load.js
k6 run -e VUS=25 -e DURATION=5m .\load-tests\k6\quiz-load.js
k6 run -e VUS=75 -e DURATION=10m .\load-tests\k6\quiz-load.js
k6 run -e VUS=150 -e DURATION=20m .\load-tests\k6\quiz-load.js
```

Le quiz doit être lancé dans l'administration avant la commande. Démarrer la première question seulement lorsque le compteur de participants a atteint le nombre de VUs attendu.

## Pic de connexion

Ce test est volontairement simultané. Le rate limit Nginx actuel peut le faire échouer, ce qui constitue précisément le résultat recherché.

```powershell
k6 run -e VUS=150 .\load-tests\k6\login-storm.js
```

Pour afficher le tableau de bord local k6 dans le navigateur :

```powershell
$env:K6_WEB_DASHBOARD="true"
$env:K6_WEB_DASHBOARD_OPEN="true"
k6 run -e VUS=5 -e DURATION=2m .\load-tests\k6\quiz-load.js
```

Ne jamais exécuter directement le test à 150 VUs sans avoir validé les paliers 5, 25 et 75.

## Nettoyage

Terminer le quiz de charge puis supprimer ce questionnaire dédié depuis l'administration avant de supprimer les comptes. Les participations et réponses historiques empêchent volontairement la suppression d'un utilisateur encore référencé.

```powershell
.\load-tests\k6\cleanup-users.ps1
```

Le script demande les identifiants administrateur, affiche le nombre de comptes ciblés et ne supprime rien tant que `SUPPRIMER` n'a pas été saisi exactement.
