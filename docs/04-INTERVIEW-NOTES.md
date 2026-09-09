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

## Lot 2 : Admin REST Et Angular Reactive Forms

Statut : valide fonctionnellement. Le socle admin, les CRUD backend `Horse`, `Race`, `RaceEntry`, la page admin Angular Reactive Forms, l'integration auth frontend JWT, le seeder ADMIN local controle, le durcissement UX et la validation end-to-end avec token backend reel sont termines.

### Concepts Deja Couverts

- Conversion d'un claim JWT custom `roles` en authorities Spring `ROLE_*`.
- Difference entre authentification valide et autorisation par role.
- Protection route-prefix `/api/admin/**` avec `hasRole("ADMIN")`.
- CRUD REST admin avec DTO request/response, service, repository.
- Validation d'entree avec `@NotBlank` et `@Size`.
- Traduction d'une absence de ressource en HTTP `404` via `@RestControllerAdvice`.
- Etat metier `RaceState` expose via DTO et conserve lors d'une mise a jour si absent de la requete.
- Frontend Angular 21/Taiga UI v5 initialise avec Signals, NgRx Signals et SSE.
- Invariants `RaceEntry` controles avant sauvegarde et proteges aussi par contraintes SQL.
- Conflits metier admin traduits en HTTP `409 Conflict`.
- Routes Angular principales lazy-loadees pour garder le bundle initial sous budget.
- Page `/admin` connectee aux CRUD admin via service HTTP type et Reactive Forms.
- Login frontend avec guard admin et intercepteur `Authorization: Bearer ...`.
- Seeder ADMIN local desactive par defaut et active seulement via profil `local`.
- Test d'integration complet : login ADMIN local, JWT signe par le backend, acces a un endpoint `/api/admin/**`.
- UX admin : confirmation de suppression, libelles de chargement, erreurs `401/403`, `404` et `409` rendues lisibles.

### Points A Retenir

Le CRUD `Horse` expose des DTO et non l'entite JPA, meme si l'entite est simple. Ce choix evite de coupler le contrat HTTP au modele de persistence et prepare les CRUD plus riches comme `Race` et `RaceEntry`.

Spring Security 7 peut ajouter une authority technique comme `FACTOR_BEARER`. Les tests doivent verifier la presence des authorities metier attendues sans supposer qu'elles sont les seules.

Le CRUD `Race` garde le resultat hors de l'entite `Race`. Le gagnant et les classements restent a modeliser via `RaceEntry`, ce qui preserve l'invariant valide au Lot 0.

Le CRUD `RaceEntry` est le vrai point metier du Lot 2 : il garantit qu'un cheval est inscrit dans une course avec un numero de dossard unique pour cette course, et que le classement reste optionnel jusqu'au resultat.

L'admin Angular est maintenant branche a l'auth frontend. La session JWT est stockee en `localStorage` avec expiration ; c'est pragmatique pour le projet, mais a challenger avant production selon le niveau de risque XSS accepte.

Le seeder ADMIN local est volontairement controle par configuration : il resout le probleme de bootstrap en developpement et en test d'integration sans introduire de compte admin actif par defaut en production.

### Reponse Senior Synthese

J'ai separe le contrat admin REST des entites JPA via des DTO request/response, puis place les invariants metier dans les services applicatifs. Les endpoints `/api/admin/**` sont proteges par role `ADMIN` a partir d'un claim JWT custom converti en authorities Spring. Cote Angular, la page admin utilise Reactive Forms et un service HTTP type, avec guard et intercepteur JWT. La validation finale couvre un vrai flux local : creation de l'admin par seeder, login, recuperation d'un JWT signe, puis acces a un endpoint admin protege.

### Killer Questions Lot 2

Pourquoi ne pas exposer directement les entites JPA dans les controllers admin ?

Reponse : parce que l'entite est un modele de persistence, pas un contrat HTTP. L'exposer couple l'API a Hibernate, risque les graphes relationnels involontaires, et rend les evolutions de champs plus dangereuses.

Pourquoi traduire les violations d'unicite `RaceEntry` en `409 Conflict` ?

Reponse : le client envoie une requete syntaxiquement valide, mais incompatible avec l'etat actuel du domaine. `400` serait trop generique, `500` exposerait un probleme serveur faux. `409` exprime un conflit metier recuperable.

Pourquoi garder les contraintes SQL si le service verifie deja les doublons ?

Reponse : le service donne un message metier propre, mais la base reste le dernier verrou d'integrite, notamment en cas de concurrence ou de bug applicatif.

Pourquoi tester un login ADMIN reel en integration ?

Reponse : les tests unitaires prouvent chaque brique, mais l'integration verifie le chainage critique : seeding, BCrypt, login, signature JWT, filtre Resource Server, conversion des roles et autorisation admin.

Pourquoi le guard Angular ne suffit pas a proteger `/admin` ?

Reponse : le frontend est seulement une barriere UX. La securite reelle est cote backend avec Spring Security sur `/api/admin/**`; un client peut toujours appeler l'API directement.

Pourquoi le stockage JWT en `localStorage` est a challenger avant production ?

Reponse : il est simple et persistant, mais expose le token en cas de XSS. Selon le modele de menace, des cookies `HttpOnly`, une duree de vie plus courte ou un mecanisme de refresh mieux encadre peuvent etre preferables.

## Capacites Ajoutees Apres Le Lot 2

Ces capacites sont fonctionnelles mais ne sont pas encore considerees comme validees au meme niveau que les Lots 0 a 2, principalement faute de couverture de tests dediee.

### Inscription Et Administration Utilisateur

L'inscription publique normalise l'email, encode le code d'acces avec BCrypt, attribue le role `USER` et retourne un JWT. L'administration gere les profils et les roles `USER`, `VIP`, `ADMIN`, tout en interdisant la suppression ou la retrogradation du dernier administrateur.

Question : pourquoi proteger le dernier administrateur dans le service et pas seulement dans l'interface ?

Reponse : l'interface n'est pas une frontiere de securite et peut etre contournee. L'invariant doit etre applique dans le service pour tous les clients. Pour une forte concurrence, le comptage actuel devrait encore etre renforce par verrouillage transactionnel ou par une politique de bootstrap externe.

### Feature Flags Persistants

Un singleton `AppFeatureSettings` active les sections pari et quiz. La lecture est publique pour permettre le routage initial Angular, la modification est reservee aux administrateurs et le service garantit qu'au moins une fonctionnalite reste active.

Question : un guard Angular suffit-il a desactiver une fonctionnalite ?

Reponse : non. Le guard controle uniquement la navigation et l'UX. Si la desactivation doit aussi interdire les API, le backend doit appliquer le flag dans les cas d'usage concernes. Ce verrouillage serveur n'est pas generalise actuellement.

### Quiz Et Machine D'Etats

Le quiz separe le questionnaire reutilisable (`QuizSet`) de son execution (`QuizSession`). Le serveur pilote les phases `OPENING`, `QUESTION_OPEN`, `QUESTION_LOCKED`, `ANSWER_REVEALED`, `SCOREBOARD` et `FINISHED`. Les joueurs doivent rejoindre la session et ne peuvent soumettre qu'une reponse par question.

Question : pourquoi modeliser les phases explicitement ?

Reponse : une machine d'etats rend les transitions autorisees visibles et testables. Elle empeche par exemple une reponse apres verrouillage ou une revelation avant fermeture. Dans une version plus concurrente, les transitions devront aussi etre protegees par versionnement optimiste ou verrouillage.

Question : pourquoi ne pas envoyer la bonne reponse des l'ouverture ?

Reponse : le DTO joueur masque l'indicateur `correct` et l'identifiant de la bonne reponse jusqu'a la phase de revelation. Le contrat HTTP doit eviter de transmettre un secret fonctionnel que le frontend se contenterait de cacher visuellement.

### SSE Et Temps Reel

Le tableau de courses utilise actuellement Server-Sent Events : Spring MVC emet un snapshot toutes les deux secondes et Angular le consomme avec `EventSource`. Le quiz utilise de son cote un polling REST toutes les deux secondes.

Question : quand preferer SSE a WebSocket ?

Reponse : SSE convient a un flux descendant serveur vers navigateur, fonctionne sur HTTP et gere nativement la reconnexion. WebSocket devient pertinent pour des echanges bidirectionnels frequents ou un protocole temps reel plus riche. Pour des commandes ponctuelles, REST plus SSE reste souvent plus simple.

Limite actuelle : un scheduler est cree par connexion SSE et le flux diffuse des snapshots complets. Avant une forte charge, il faudra mutualiser la production d'evenements, gerer le backpressure et mesurer le cout des requetes JPA repetees.

### Docker, CI Et Dokploy

Le backend et le frontend utilisent des images multi-stage. La production execute un JRE 25 non-root pour Spring Boot et Nginx pour Angular. PostgreSQL, backend et frontend sont relies par un reseau prive ; seul Nginx doit etre expose et transmet `/api` au backend. GitHub Actions teste Maven, construit Angular et publie les images sur GHCR.

Question : pourquoi ne pas exposer directement le backend ?

Reponse : un point d'entree unique simplifie TLS, CORS, les domaines et le routage. Le backend reste accessible uniquement sur le reseau interne des conteneurs.

Limites actuelles : la CI ne lance ni lint ni tests frontend, `ddl-auto=update` doit etre remplace par des migrations controlees, et la publication GHCR ne declenche pas automatiquement Dokploy.

## Lot 3 : Workflow De Course Et Paris Server-Driven

### Reponse Senior Synthese

J'ai modelise le deroule d'une course comme une machine d'etats lineaire controlee par le backend. Le frontend ne
peut ni ouvrir les paris ni publier un resultat directement par un simple CRUD. Le pari est horodate par le serveur,
serialise par verrou pessimiste sur l'utilisateur et remplace atomiquement en cas de changement. La publication du
resultat exige tous les partants exactement une fois, classe les `RaceEntry`, settle chaque pari et termine la course
dans une seule transaction. Le polling REST authentifie est un transport temporaire ; le futur STOMP reutilisera le
meme snapshot et les memes invariants.

### Killer Questions Lot 3

Pourquoi ne pas laisser le CRUD `Race` changer librement l'etat ?

Reponse : un enum valide ne garantit pas une transition valide. Sans cas d'usage dedie, on pourrait passer de
`CREATED` a `FINISHED`, rouvrir des paris apres leur fermeture ou publier un resultat incomplet.

Pourquoi verrouiller l'utilisateur pendant la prise de pari ?

Reponse : l'invariant est un pari par utilisateur et par course, alors que la course est indirectement referencee
via `RaceEntry`. Le verrou utilisateur serialise deux votes concurrents du meme participant sans dupliquer la course
dans `Bet` et sans creer une incoherence de mapping.

Pourquoi l'horodatage est-il genere cote serveur ?

Reponse : le classement de rapidite est une donnee metier. Une date client serait falsifiable et dependrait de
l'horloge du terminal. Le serveur fournit une base commune ; un changement de cheval renouvelle volontairement cette
date.

Pourquoi garder REST + polling avant STOMP ?

Reponse : le snapshot REST valide deja les contrats, la securite et tout le workflow. STOMP devient ensuite un
changement de transport et de latence, pas une reecriture du domaine. `EventSource` natif ne permettait pas d'ajouter
le bearer JWT, ce qui avait conduit l'ancien SSE a etre public.

Pourquoi separer `visibleOnLive` de `RaceState.FINISHED` ?

Reponse : `FINISHED` decrit un fait metier permanent, alors que l'affichage du resultat est une decision de
publication temporaire. Reutiliser l'etat pour remettre l'ecran a zero detruirait le sens de l'historique ou
ajouterait un etat technique. Un indicateur de visibilite permet de conserver le resultat tout en retirant la
course de l'ecran public.

Pourquoi ne pas laisser l'admin saisir le rang dans le CRUD des participations ?

Reponse : avant la fin, aucun rang n'existe. Le seul cas d'usage legitime est la publication d'un ordre d'arrivee
complet, qui garantit l'unicite et la continuite des places dans une transaction. Un champ optionnel editable
permettait des classements partiels ou contradictoires.

Pourquoi ajouter `finishedAt` alors que l'entite possede deja `updatedAt` ?

Reponse : `finishedAt` represente l'instant metier ou le resultat devient officiel. `updatedAt` est une information
technique qui peut changer plus tard, par exemple lors du retrait du resultat de l'ecran live. Trier l'historique sur
`updatedAt` rendrait donc son ordre instable.

Comment garantir un classement reproductible si deux gagnants ont le meme horodatage ?

Reponse : le tri utilise d'abord l'horodatage serveur du vote, puis l'identifiant persiste du pari comme critere de
departage. L'ordre reste ainsi total et deterministe, sans inventer une precision temporelle absente de la donnee.

Pourquoi l'API d'historique personnel ne prend-elle pas de `userId` en parametre ?

Reponse : l'identite est deja portee par le JWT signe. Accepter un identifiant fourni par le client introduirait un
risque d'acces horizontal aux paris d'un autre utilisateur. Le controller extrait donc le claim `userId` et le
service ne travaille qu'avec cette identite authentifiee.

Pourquoi separer la disponibilite de l'historique de son contenu complet ?

Reponse : le menu a seulement besoin de savoir si au moins une participation terminee existe. Un `exists` en base
est moins couteux que charger les courses, leurs gagnants et les rangs a chaque initialisation de l'application. Le
detail complet reste charge a la demande sur la page dediee.

Pourquoi rendre la lecture des logos publique alors que leur administration est protegee ?

Reponse : une balise HTML `<img>` ne passe pas par l'intercepteur Angular qui ajoute le bearer JWT. Le logo est un
media public non sensible ; sa lecture peut donc etre anonyme, tandis que la liste admin et toutes les mutations
restent protegees par `ROLE_ADMIN`. La liste publique filtre en plus les partenaires non coches.

Pourquoi valider le type et la taille du logo cote serveur ?

Reponse : l'attribut `accept` du champ fichier n'est qu'une aide UX et peut etre contourne. Le service impose donc
une liste blanche de types raster et une limite de 2 Mo avant toute persistence, ce qui protege le stockage et evite
les contenus actifs comme les SVG arbitraires.

### Tutoriel Du Premier Pari

Le tutoriel est une simulation frontend isolee du domaine reel. Un store NgRx Signals porte sa machine d'etapes et
des fixtures statiques representent la course, le pari et l'historique. Aucun endpoint de pari n'est appele par la
simulation : les invariants et l'horodatage des vraies courses restent donc exclusivement controles par le serveur.
La completion est un marqueur monotone persiste sur `AppUser` par un endpoint authentifie. L'identite vient du claim
JWT `userId` et non du corps de requete. Le tutoriel ne reapparait donc pas apres deconnexion, effacement du stockage
navigateur ou changement d'appareil.

Pourquoi ne pas reutiliser directement `BettingApiService` pour la course d'entrainement ?

Reponse : cela melangerait donnees pedagogiques et domaine persiste, imposerait des exceptions serveur aux invariants
de course et pourrait polluer l'historique. Une fixture locale rend l'isolation explicite et testable.

Pourquoi persister la completion cote serveur plutot que dans `localStorage` ?

Reponse : le besoin est attache au compte et exige que le tutoriel ne reapparaisse jamais. Un stockage navigateur peut
etre efface et n'est pas partage entre appareils. Le booléen serveur fournit une source de verite durable ; sa mise a
jour idempotente rend sans danger les doubles clics et nouvelles tentatives reseau.

## Lot 5 : Mode Exclusif Et Nuage De Mots

### Reponse Senior Synthese

J'ai remplace les indicateurs fonctionnels independants par un enum persiste qui rend l'exclusivite structurelle. Le
nuage de mots est une vertical slice avec une machine d'etats serveur, un slot live unique protege en SQL et des
transitions serialisees par verrou pessimiste. Chaque reponse est rattachee a l'identite JWT, unique par question et
modifiable uniquement pendant l'ouverture. Les participants ne recoivent les agregats qu'apres la revelation et
aucune identite n'est exposee. Le polling REST reste un transport interchangeable avec un futur flux STOMP.

### Killer Questions Lot 5

Pourquoi remplacer trois booleens par un enum `activeMode` ?

Reponse : l'invariant exige exactement un mode. Avec des booleens, plusieurs combinaisons invalides restent
representables et doivent etre refusees a chaque ecriture. Un enum rend ces etats impossibles dans le modele Java, le
contrat HTTP et la base.

Pourquoi une contrainte unique sur un slot nullable ?

Reponse : la verification applicative produit un message clair mais deux transactions peuvent la franchir ensemble.
La valeur `TRUE` reserve l'unique slot live et `NULL` permet de conserver un nombre illimite de questions closes. La
base reste ainsi le dernier arbitre en concurrence.

Pourquoi verrouiller la question lors d'une soumission ?

Reponse : la fermeture et une derniere reponse peuvent arriver simultanement. Le verrou impose un ordre total : soit
la reponse est persistee avant la fermeture, soit elle observe l'etat ferme et est refusee. Une simple verification
sans verrou laisserait une fenetre de course.

Pourquoi stocker un texte affiche et une cle normalisee ?

Reponse : la cle sans casse ni accents regroupe les occurrences attendues, tandis que le texte affiche conserve une
forme humaine stable. Calculer uniquement au frontend dupliquerait la regle, exposerait des resultats divergents et
obligerait a transmettre toutes les reponses brutes.

Pourquoi masquer les agregats avant la revelation cote backend ?

Reponse : cacher les mots uniquement dans Angular ne protege rien ; un participant pourrait lire la reponse HTTP. Le
snapshot joueur retourne donc structurellement une liste vide avant `REVEALED`, alors que la vue admin peut suivre les
agregats necessaires au pilotage.
