# Tests du TP1 avec JUnit 6

Cette campagne vérifie le tag `etape_1` (`ceee612`). Les tests compilent directement les sources du dossier `../src` : ils évaluent donc le checkout ouvert, et non une copie cachée. Pour reproduire les résultats du rapport, conserver cette version des sources. Les nouveaux fichiers de tests et de rapport doivent être sauvegardés dans votre travail Git avant de changer de version.

## Dans IntelliJ IDEA

1. Ouvrir `tests/pom.xml` comme projet Maven dans une nouvelle fenêtre (File → Open), ou l’ajouter comme projet Maven dans la fenêtre actuelle (clic droit → Add as Maven Project).
2. Choisir un **JDK 17 ou supérieur**, et non un JRE, dans File → Project Structure → Project SDK. Utiliser aussi ce JDK pour le Maven Runner dans Settings → Build, Execution, Deployment → Build Tools → Maven → Runner.
3. Recharger le projet Maven. Maven télécharge automatiquement JUnit Jupiter **6.0.2**. Les imports `org.junit.jupiter.api` sont corrects pour JUnit 6.
4. Le dossier `tests/java` est déclaré comme dossier de tests par le fichier Maven. Le dossier `src` du projet est déclaré comme dossier de sources. Ne pas ajouter les sources du TP2 à ce module.
5. Clic droit sur une classe de test ou sur `tests/java` → Run Tests. Pour la même mesure de couverture que le rapport, utiliser la commande ci-dessous dans le terminal d’IntelliJ.

La suite n’emploie ni Mockito ni architecture particulière. Les tests suivent directement préparation, appel, assertions. Les seuls outils supplémentaires dans les tests sont la réflexion pour atteindre les champs privés de Simulateur et un processus séparé pour tester ses appels à System.exit. Ils évitent de modifier le code de production uniquement pour les tests.

## Commande complète depuis la racine du dépôt

```sh
sh script/runTestsTP1
```

Prérequis : Maven et un JDK 17 ou supérieur. Vérifier le JDK réellement utilisé avec `mvn -version` ; `java -version` peut désigner une autre installation. La campagne enregistrée a été exécutée avec le JDK 26 de Maven, en compilant pour Java 17.

Les résultats se trouvent dans :

- `tests/target/surefire-reports` : détails des tests et XML.
- `tests/target/site/jacoco/index.html` : couverture lisible dans un navigateur.
- `rendu/tp1/Dossier de tests/Rapport de test.md` : rapport rédigé selon le modèle demandé.
- `rendu/tp1/Dossier de tests/preuves` : résultats archivés de la campagne décrite dans le rapport.

Le script génère la couverture même si des assertions échouent, puis renvoie un code non nul. Un « BUILD SUCCESS » pour JaCoCo ne signifie pas que les tests précédents ont réussi. Le rapport et les preuves archivées ne sont pas réécrits automatiquement lors d’une nouvelle exécution : les actualiser après toute correction, avec le nouveau commit et les résultats réels.

## Résultats attendus sur etape_1

91 exécutions : **88 réussies et 3 en échec**. Les trois tests rouges sont volontaires au sens où ils conservent l’attente correcte face à un défaut existant ; ils ne sont ni ignorés ni désactivés :

- `messageAbsentDoitLeverArgumentsException` : `-mess` sans valeur lève ArrayIndexOutOfBoundsException.
- `graineAbsenteDoitLeverArgumentsException` : `-seed` sans valeur lève ArrayIndexOutOfBoundsException.
- `virguleDansMessageDoitEtreRejetee` : un message contenant une virgule est accepté.

Les tests dont le nom contient « Actuellement » sont des caractérisations de comportements non spécifiés ou de limites de robustesse. Leur réussite ne valide pas un choix de conception : après accord sur un autre contrat, changer conjointement l’implémentation et ces attentes.

Les sondes sont testées pour leur raccordement. Le test de `main` avec `-s` impose le mode sans écran pour provoquer une erreur contrôlée ; il ne valide pas le dessin des fenêtres. Les comparaisons de grands messages se limitent à la longueur et à la transmission de la même information, pour éviter le coût quadratique du parcours indexé d’une LinkedList. Aucun test ne prétend vérifier toutes les graines ou prouver la qualité statistique de Random.

## Générer la Javadoc de la version étudiée

Depuis la racine, avec le JDK 17 ou supérieur sélectionné :

```sh
javadoc -quiet -Xdoclint:none -private -encoding UTF-8 -charset UTF-8 -d tests/target/javadoc -sourcepath src -subpackages sources:destinations:transmetteurs:simulateur:information:visualisations
```

L’option `-private` inclut `generer` et `analyseArguments`. Le rapport cite la Javadoc existante du TP1 et signale les méthodes sans commentaire propre. `-Xdoclint:none` permet la génération malgré les balises HTML historiques des commentaires fournis.

Documentation de référence : [JUnit 6.0.2](https://docs.junit.org/6.0.2/overview.html) et [JUnit dans IntelliJ IDEA](https://www.jetbrains.com/help/idea/junit.html).
