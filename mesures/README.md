# Mesures du taux d'erreur binaire

Le projet utilise Java 17 au minimum. Vérifier `javac -version` avant de compiler et `java -version` avant de lancer les mesures : le programme exécuté doit accepter la version des classes dans `bin`. Sur un Mac où OpenJDK est installé par Homebrew, placer d'abord ses exécutables en tête du `PATH` :

```bash
export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"
```

Depuis la racine du projet, installer les dépendances Python et compiler le simulateur Java :

```bash
python3 -m pip install -r mesures/requirements.txt
./script/compile
```

## Simuler et conserver les résultats

```bash
python3 mesures/teb.py
```

Le script lance les simulations Java, écrit les résultats dans `mesures/teb_resultats.json`, puis crée `courbes_teb.png` à la racine du projet. Le JSON contient le nombre de bits, le nombre d'échantillons par bit, les valeurs de SNR en dB et les TEB de chaque forme d'onde. Une nouvelle simulation complète remplace ce fichier ; si une simulation échoue, le fichier précédent est conservé.

Les valeurs utilisées pour une nouvelle simulation se règlent en haut de `teb.py` :

| Paramètre | Rôle | Valeur actuelle |
| --- | --- | --- |
| `NB_BITS` | Nombre de bits aléatoires transmis à chaque simulation Java (`-mess`). | `100_000` |
| `NB_ECH` | Nombre d'échantillons par bit (`-nbEch`). | `30` |
| `SNR_RANGE` | Valeurs de SNR par bit en dB (`-snrpb`). `np.arange(début, fin, pas)` exclut `fin`. | `np.arange(5, 15, 3)` : 5, 8, 11 et 14 dB |
| `FORMES` | Formes d'onde et amplitudes passées à Java (`-form` et `-ampl`). | NRZ, RZ et NRZT |

Cela représente actuellement 4 valeurs de SNR × 3 formes, donc 12 simulations Java. Le script en exécute jusqu'à quatre en parallèle. Avec la syntaxe actuelle de `-mess`, `1_000_000` est interprété comme le message binaire fixe `1000000` (7 bits) : utiliser au plus `999_999` pour demander une longueur aléatoire.

## Retracer sans simuler

```bash
python3 mesures/teb.py --tracer
```

Ce mode lit `mesures/teb_resultats.json` et recrée `courbes_teb.png` sans lancer Java. Il utilise les SNR enregistrés dans le JSON, même si `SNR_RANGE` a changé depuis. Il faut avoir terminé une simulation pour disposer du fichier JSON.

## Arguments de `teb.py`

| Argument | Effet |
| --- | --- |
| Aucun | Lance les simulations avec les réglages de `teb.py`, sauvegarde le JSON et trace le graphique. |
| `--tracer` | Lit le JSON et trace le graphique sans relancer les simulations. |
| `--donnees CHEMIN` | Choisit le fichier JSON. En mode simulation, il est créé ou remplacé ; avec `--tracer`, il est lu. Par défaut : `mesures/teb_resultats.json`. |
| `--image CHEMIN` | Choisit le PNG créé ou remplacé. Par défaut : `courbes_teb.png` à la racine du projet. |
| `-h`, `--help` | Affiche l'aide de la commande. |

Les arguments `--donnees` et `--image` peuvent être combinés avec `--tracer`. Modifier `NB_BITS`, `NB_ECH` ou `SNR_RANGE` n'affecte pas les résultats déjà enregistrés : il faut relancer une simulation pour les recalculer.

Pour choisir les chemins des résultats et de l'image :

```bash
python3 mesures/teb.py --donnees mesures/essai.json --image essai.png
python3 mesures/teb.py --tracer --donnees mesures/essai.json --image essai.png
```

Le graphique est logarithmique : les points de TEB nul sont enregistrés dans le JSON, mais ne peuvent pas être affichés sur l'axe vertical.
