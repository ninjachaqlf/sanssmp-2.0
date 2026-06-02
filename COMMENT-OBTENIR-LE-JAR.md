# 📦 Obtenir le fichier SansSMP.jar (sans rien installer)

Java doit être "compilé" avant de fonctionner. On va laisser **GitHub le faire
gratuitement pour toi**, puis tu télécharges le `.jar` final. Aucun logiciel à
installer sur ton PC.

## Étapes

1. **Crée un compte gratuit** sur https://github.com (si tu n'en as pas).

2. Clique sur le **+** en haut à droite → **New repository**.
   - Donne un nom (ex : `sans-smp-plugin`)
   - Laisse en **Public** (ou Private, peu importe)
   - Clique **Create repository**

3. Sur la page du dépôt, clique sur **"uploading an existing file"**
   (ou : Add file → Upload files).

4. **Glisse-dépose TOUT le contenu de ce dossier** (le dossier `src`, le dossier
   `.github`, le fichier `pom.xml`...) dans la zone d'upload.
   👉 Important : garde la même structure de dossiers. Le plus simple est de
   sélectionner tous les fichiers/dossiers d'un coup et de les déposer.
   Puis clique **Commit changes** en bas.

5. GitHub lance automatiquement la compilation. Va dans l'onglet **Actions**
   (en haut du dépôt). Attends que le rond devienne **vert ✅** (1 à 2 minutes).

6. Clique sur la ligne verte → en bas, section **Artifacts** → clique sur
   **SansSMP** pour télécharger.

7. Tu obtiens un fichier `SansSMP.zip` → **décompresse-le** : à l'intérieur se
   trouve **`SansSMP.jar`**. 🎉

## Installer sur ton serveur

1. Serveur en **Paper 1.21.x** (Java 21).
2. Mets **`SansSMP.jar`** dans le dossier **`plugins/`** du serveur.
3. Redémarre le serveur.
4. En jeu : `/epee dash` pour tester, `/rituel` pour lancer un rituel.

---

## (Optionnel) Compiler sur ton PC

Si tu préfères, avec **Java 21 (JDK)** + **Maven** installés, ouvre un terminal
dans le dossier du projet et lance :

```
mvn clean package
```

Le `.jar` apparaît dans le dossier `target/`.
