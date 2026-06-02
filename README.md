# Sans SMP — Plugin d'épées custom

Plugin Minecraft (Paper / Spigot) pour le serveur **Sans SMP**.
Il ajoute 6 épées légendaires avec capacités, et une commande de **rituel**.

## ⚔️ Les épées

| Épée | Activation | Effet |
|------|-----------|-------|
| **Dash Sword** | Clic droit | Te propulse en avant en un éclair |
| **Lame de Foudre** | Clic droit | Invoque un éclair + dégâts de zone |
| **Lame de Glace** | Clic droit | Gèle et ralentit la cible visée |
| **Lame Vampirique** | Passive | Vole de la vie à chaque coup |
| **Lame du Vide** | Clic droit | Te téléporte au bloc visé |
| **Lame Explosive** | Clic droit | Explosion (sans casser les blocs) |

## 🎮 Commandes

- `/epee <dash|foudre|glace|vampire|vide|explosive> [joueur]` — donne une épée
- `/rituel [épée]` — lance un rituel : une épée apparaît **en l'air, en glowing**,
  flotte **2 minutes** (personne ne peut la prendre), puis **tombe** au sol.
  **Le premier qui la ramasse la garde.** Sans argument, l'épée est aléatoire.

Permissions (op par défaut) : `sanssmp.epee`, `sanssmp.rituel`

## 🛠️ Compiler le plugin (obtenir le .jar)

Il te faut **Java 21** et **Maven** installés.

1. Ouvre un terminal dans le dossier du projet (là où se trouve `pom.xml`)
2. Lance :
   ```
   mvn clean package
   ```
3. Le fichier `SansSMP.jar` se trouve dans le dossier **`target/`**

> Pas de Maven ? Le plus simple est d'ouvrir le dossier dans **IntelliJ IDEA**
> (gratuit, édition Community), qui télécharge tout et compile automatiquement.

## 📦 Installer

1. Mets le serveur en **Paper 1.21.x** (recommandé) ou Spigot 1.21.x — Java 21 requis
2. Place `SansSMP.jar` dans le dossier **`plugins/`** du serveur
3. Redémarre le serveur

C'est prêt ! Tape `/epee dash` en jeu pour tester.

---
Non affilié à Mojang ou Microsoft.
