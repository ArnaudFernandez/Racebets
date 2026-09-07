# Optimiseur d'images des quiz

Cet outil recupere des quiz existants avec l'API d'administration, convertit leurs images PNG, JPEG ou WebP en WebP optimise, puis peut creer de nouveaux quiz sans modifier les originaux.

## Garanties

- simulation sans creation ni modification de quiz par defaut;
- identifiants de quiz obligatoires et explicites;
- creation par `POST` uniquement, sans mise a jour ni suppression;
- refus de creer une copie dont le titre etait deja present au debut de l'execution;
- confirmation supplementaire requise pour `olifan.pixsom.fr`;
- validation du type reel, de la Base64 et du nombre de pixels de chaque image;
- traitement sequentiel pour limiter la consommation memoire;
- objectif de 300 Ko et plafond strict de 500 Ko par image par defaut;
- refus d'une requete superieure a 45 Mo avant son envoi;
- rapport sans JWT, code d'acces ni contenu d'image.

Le titre d'une copie recoit le suffixe ` [Optimise]`. Les questions et reponses obtiennent de nouveaux identifiants; le quiz original reste intact.

## Prerequis

- Node.js 22 ou une version plus recente;
- un compte administrateur local avec code d'acces;
- une sauvegarde PostgreSQL terminee et dont la restauration a deja ete verifiee.

Installer la dependance native depuis ce dossier :

```powershell
npm ci
```

## 1. Simulation

Lancer d'abord un seul quiz. `QUIZ_IDS` accepte ensuite plusieurs identifiants separes par des virgules.

```powershell
$env:BASE_URL = "https://olifan.pixsom.fr"
$env:QUIZ_IDS = "42"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_ACCESS_CODE = Read-Host "Code administrateur"
npm run optimize
Remove-Item Env:ADMIN_ACCESS_CODE
```

La simulation telecharge et compresse les images localement, mais n'envoie aucun `POST` de creation de quiz. La connexion administrateur utilise normalement `POST /api/auth/login` et le rapport est ecrit sur le disque local. Verifier :

- que chaque image optimisee reste sous 500 Ko;
- que la reduction totale est significative;
- que le rapport apparait dans `reports/`;
- qu'aucune erreur de format ou de limite n'est signalee.

## 2. Creation d'une copie

Relancer avec les deux confirmations d'ecriture :

```powershell
$env:APPLY = "true"
$env:PRODUCTION_CONFIRMATION = "DUPLIQUER_QUIZ_PROD"
npm run optimize
Remove-Item Env:APPLY
Remove-Item Env:PRODUCTION_CONFIRMATION
Remove-Item Env:ADMIN_ACCESS_CODE
```

L'outil affiche l'identifiant du nouveau quiz. Ouvrir la copie dans l'administration et controler visuellement toutes les images de question et de correction avant de l'utiliser.

Traiter les autres quiz uniquement apres cette validation :

```powershell
$env:QUIZ_IDS = "43,44"
$env:ADMIN_ACCESS_CODE = Read-Host "Code administrateur"
npm run optimize
```

Effectuer d'abord une nouvelle simulation, puis remettre temporairement `APPLY` et `PRODUCTION_CONFIRMATION` pour creer les copies.

## Configuration

| Variable | Defaut | Description |
| --- | --- | --- |
| `BASE_URL` | obligatoire | Origine HTTPS de l'application |
| `QUIZ_IDS` | obligatoire | Identifiants sources separes par des virgules |
| `ADMIN_EMAIL` | obligatoire sans token | Adresse du compte administrateur |
| `ADMIN_ACCESS_CODE` | obligatoire sans token | Code d'acces administrateur |
| `ADMIN_TOKEN` | vide | JWT alternatif aux identifiants; ne jamais le stocker dans un fichier |
| `APPLY` | `false` | Doit valoir exactement `true` pour creer les copies |
| `PRODUCTION_CONFIRMATION` | vide | Doit valoir `DUPLIQUER_QUIZ_PROD` pour ecrire sur la production |
| `COPY_SUFFIX` | ` [Optimise]` | Suffixe des titres copies |
| `TARGET_KB` | `300` | Taille cible par image |
| `MAX_KB` | `500` | Taille maximale acceptee par image |
| `MAX_DIMENSION` | `1600` | Longueur maximale d'un cote en pixels |
| `MAX_REQUEST_MB` | `45` | Limite locale avant envoi du quiz |
| `MAX_RESPONSE_MB` | `250` | Limite de lecture d'une reponse API |
| `REPORT_DIR` | `reports` | Dossier local des rapports |

Ne pas activer `ALLOW_HTTP=true` hors d'un environnement local isole. L'outil refuse HTTP par defaut afin de ne pas exposer le JWT ou le code d'acces.

## Reprise et nettoyage

L'execution s'arrete a la premiere erreur. Les copies deja creees restent disponibles et figurent dans le rapport. Une nouvelle execution refusera leur titre existant, ce qui evite une duplication silencieuse.

Ne jamais lancer deux instances de l'outil en parallele. Le controle des titres repose sur la liste observee avant la creation et ne remplace pas une contrainte d'unicite atomique cote serveur. Le statut `creation-indeterminate` dans un rapport signifie que la requete a pu atteindre le serveur sans que sa reponse soit exploitable; verifier la liste des quiz avant toute relance.

Conserver les quiz originaux jusqu'a la fin de l'evenement. Supprimer une copie invalide uniquement depuis l'administration, apres avoir confirme qu'aucune session ne l'utilise.
