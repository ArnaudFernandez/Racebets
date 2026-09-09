# Executer Racebets en local sur macOS

Ce guide installe et lance le projet sans modifier son code fonctionnel. Le mode local utilise une base H2 embarquee : PostgreSQL et Docker ne sont pas necessaires pour le developpement courant.

## 1. Architecture locale

- Backend : Java 25, Spring Boot 4, Maven Wrapper, port `8080`.
- Frontend : Angular 21, Node.js, npm, port `4200`.
- Base locale : H2 embarquee, creee par Spring au demarrage.
- Proxy : Angular transmet `/api` vers `http://localhost:8080`.
- URL a ouvrir : `http://localhost:4200`.

Toujours lancer le backend depuis la racine du depot. Le stockage local des images utilise un chemin relatif sous `uploads/race-images`.

## 2. Etat actuel de ce Mac

L'audit du 7 septembre 2026 a confirme :

- Mac Apple Silicon (`arm64`).
- Homebrew installe dans `/opt/homebrew`.
- JDK Zulu 25.0.3 deja installe.
- Le terminal selectionne encore Java 17 par defaut.
- Node 25.2.1 et npm 11.6.2 installes.
- Docker Desktop et Docker Compose installes, mais facultatifs pour le mode local.

Le projet et la CI utilisent Node 22 comme version de reference. Node 25 peut fonctionner, mais Node 22 LTS est recommande pour reproduire l'environnement de build.

## 3. Preparer le terminal

Ouvrir Terminal et executer :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./mvnw -version
```

Les deux dernieres commandes doivent afficher Java 25. Maven sera telecharge automatiquement par le wrapper lors de sa premiere execution.

Pour selectionner Java 25 dans tous les nouveaux terminaux zsh :

```bash
printf '\nexport JAVA_HOME=$(/usr/libexec/java_home -v 25)\nexport PATH="$JAVA_HOME/bin:$PATH"\n' >> ~/.zshrc
source ~/.zshrc
```

Cette modification est facultative. Elle change le Java par defaut du compte macOS, pas le projet.

## 4. Utiliser Node 22 LTS

La methode recommandee est `nvm`, car elle permet de changer de version sans remplacer le Node global. Installer `nvm` avec la procedure officielle de son depot, rouvrir Terminal, puis executer :

```bash
nvm install 22
nvm use 22
node --version
npm --version
```

La version Node doit commencer par `v22`. Si Node 25 est conserve, tenter d'abord les commandes ci-dessous ; en cas d'erreur Angular ou npm, revenir a Node 22 avant tout autre diagnostic.

## 5. Installer les dependances

Backend : aucune installation Maven globale n'est necessaire. Le wrapper `./mvnw` utilise Maven 3.9.16.

Frontend :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets/frontend
npm ci
```

`npm ci` respecte exactement `package-lock.json`. Ne pas reutiliser un repertoire `node_modules` copie depuis Windows : les dependances peuvent contenir des binaires propres au systeme et a l'architecture.

## 6. Verifier le projet avant le premier lancement

Dans un premier terminal :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw test
```

Dans un second terminal :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets/frontend
npm run lint
npm run build
```

Pour lancer les tests frontend, Google Chrome doit etre installe :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets/frontend
npm test
```

Le mode `npm test` reste interactif tant qu'il n'est pas arrete avec `Ctrl+C`.

## 7. Lancer depuis Terminal

Terminal 1, backend :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Attendre que Spring indique que l'application a demarre sur le port `8080`.

Terminal 2, frontend :

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets/frontend
nvm use 22
npm start
```

Attendre la fin de la compilation puis ouvrir :

```text
http://localhost:4200
```

Compte administrateur local initialise automatiquement :

```text
E-mail : admin@racebets.local
Code d'acces : ADMIN-LOCAL-2026
```

Ces identifiants sont uniquement destines au profil `local`.

Pour arreter les serveurs, appuyer sur `Ctrl+C` dans chacun des deux terminaux.

## 8. Configurer IntelliJ IDEA

### 8.1 Importer le projet

1. Ouvrir IntelliJ IDEA.
2. Choisir `Open` et selectionner `/Users/arnaudfernandez/IdeaProjects/Racebets`.
3. Accepter l'import Maven du fichier `pom.xml`.
4. Attendre la fin de l'indexation et du telechargement des dependances.

### 8.2 Selectionner Java 25

1. Ouvrir `File > Project Structure > Project`.
2. Dans `SDK`, choisir le JDK Zulu 25 existant.
3. S'il n'apparait pas, choisir `Add SDK > JDK` et selectionner `/Users/arnaudfernandez/Library/Java/JavaVirtualMachines/azul-25.0.3/Contents/Home`.
4. Regler `Language level` sur `SDK default` ou `25`.
5. Ouvrir `Settings > Build, Execution, Deployment > Build Tools > Maven`.
6. Choisir le JDK 25 pour `JDK for importer` et `Runner > JRE`.

Lombok est une dependance Maven du projet. Si IntelliJ signale les annotations, verifier que `Annotation Processing` est active dans `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`.

### 8.3 Configurer Node

1. Ouvrir `Settings > Languages & Frameworks > JavaScript Runtime`.
2. Selectionner l'interpreteur Node 22 installe par `nvm`.
3. Verifier que le gestionnaire de paquets pointe vers le `npm` associe a ce Node.
4. Dans le terminal integre IntelliJ, executer `cd frontend && npm ci` si ce n'est pas deja fait.

### 8.4 Lancer les configurations fournies

Le depot contient des configurations partagees dans `.run/` :

- `Racebets Backend` : lance Spring Boot avec Java 25 et le profil `local`.
- `Racebets Frontend` : lance `npm start` dans `frontend`.
- `Racebets Local` : lance les deux configurations ensemble.

Ces configurations utilisent `/bin/zsh` plutot que les types Spring Boot et npm. Elles restent ainsi utilisables meme si les plugins Ultimate correspondants sont desactives. Selectionner `Racebets Local` dans la liste en haut a droite, puis cliquer sur Run.

Si IntelliJ affiche encore une ancienne configuration comme `Unknown`, fermer puis rouvrir le projet afin de recharger les fichiers `.run` modifies. Supprimer uniquement les anciennes entrees `Unknown` dans `Run > Edit Configurations` si elles restent affichees.

Sur l'installation IntelliJ auditee, le module global Ultimate est desactive dans `Settings > Plugins`. Pour retrouver les integrations natives Spring Boot et npm, reactiver `IntelliJ IDEA Ultimate` puis redemarrer l'IDE. Cette activation est facultative pour les trois configurations fournies, mais elle necessite une licence Ultimate valide.

## 9. Google OAuth local, facultatif

Le login local par e-mail et code fonctionne sans Google. Pour activer Google, definir les variables dans la configuration backend IntelliJ ou dans le terminal avant le lancement :

```bash
export GOOGLE_AUTH_ENABLED=true
export GOOGLE_CLIENT_ID='votre-client-id'
export GOOGLE_CLIENT_SECRET='votre-secret'
```

Configurer dans Google Cloud la redirection exacte :

```text
http://localhost:4200/api/auth/google/callback/google
```

Ne jamais enregistrer le secret Google dans Git. Voir `docs/05-GOOGLE-AUTH-SETUP.md` pour la procedure complete.

## 10. Donnees locales

- Le profil `local` utilise H2 et `spring.jpa.hibernate.ddl-auto=update`.
- Flyway est desactive en local.
- PostgreSQL n'a donc pas besoin d'etre lance.
- Le secret JWT est regenere au demarrage ; une ancienne session peut devoir se reconnecter.
- Les fichiers envoyes sont crees sous `uploads/`, ignore par Git.

Le comportement de production utilise PostgreSQL et Flyway. Un lancement H2 reussi ne remplace donc pas les tests de migrations et de conteneurs effectues par la CI.

## 11. Depannage

### `Unable to locate a Java Runtime` ou mauvaise version

```bash
/usr/libexec/java_home -V
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

### `Permission denied: ./mvnw`

```bash
chmod +x /Users/arnaudfernandez/IdeaProjects/Racebets/mvnw
```

Le droit d'execution est maintenant aussi enregistre dans Git pour les prochains clones macOS/Linux.

### Le port 8080 ou 4200 est deja utilise

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:4200 -sTCP:LISTEN
```

Arreter l'ancien processus proprement avec `Ctrl+C`, ou fermer son ancienne configuration IntelliJ.

### Le frontend retourne une erreur sur `/api`

Verifier que le backend est demarre sur `8080`, puis ouvrir :

```text
http://localhost:4200/api/app/features
```

Ne pas utiliser directement `http://localhost:8080` comme URL de l'interface.

### Installation npm incoherente apres Windows ou changement de Node

```bash
cd /Users/arnaudfernandez/IdeaProjects/Racebets/frontend
rm -rf node_modules
npm ci
```

Cette suppression ne touche ni au code ni au lockfile.

### IntelliJ ne voit pas les classes generees par Lombok

Recharger le projet Maven, verifier le JDK 25 et activer l'annotation processing. Redemarrer l'IDE seulement si la reindexation ne suffit pas.

## 12. Checklist finale

- `java -version` affiche 25.
- `./mvnw -version` fonctionne.
- Node affiche idealement 22 LTS.
- `npm ci` a termine dans `frontend`.
- `./mvnw test` reussit.
- `npm run lint` et `npm run build` reussissent.
- Backend disponible sur `http://localhost:8080`.
- Frontend disponible sur `http://localhost:4200`.
- Connexion locale possible avec le compte administrateur de developpement.
