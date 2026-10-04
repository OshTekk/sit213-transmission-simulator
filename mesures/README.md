# Mesures du simulateur

Les quatre scripts Python se lancent depuis la racine du projet. Ils utilisent le simulateur Java compilé dans `bin` et les bibliothèques indiquées dans `mesures/requirements.txt` (`numpy`, `matplotlib` et `scipy`). Vérifier que `java` et `javac` désignent une version capable de lire les classes compilées. Sur un Mac avec OpenJDK installé par Homebrew :

```bash
export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"
java -version
javac -version
python3 -m pip install -r mesures/requirements.txt
./script/compile
```

| Script | Mesure | Résultats par défaut |
| --- | --- | --- |
| `teb.py` | TEB en fonction du rapport signal sur bruit par bit (`E_b/N_0`). | `mesures/teb_resultats.json` et `courbes_teb.png` |
| `pbruit.py` | Distribution du bruit gaussien et comparaison à la théorie. | `out.txt` et `gaussienne_bruit.png` |
| `teb_multitrajet.py` | Carte du TEB selon le retard et l'amplitude d'un trajet indirect. | `results/teb_multitrajet_nrz.csv` / `.png`, `results/teb_multitrajet_rz.csv` / `.png` et `results/teb_multitrajet_nrzt.csv` / `.png` |
| `teb_multitrajets.py` | Cinq cartes TEB, pour 1 à 5 trajets espacés régulièrement, et cinq exemples de leurs amplitudes dans le temps. | `results/teb_multitrajets_nrz.csv` / `.png`, `results/teb_multitrajets_rz.csv` / `.png` et `results/teb_multitrajets_nrzt.csv` / `.png` |

Le JSON de `teb.py` est écrit dans `mesures/` ; les CSV et PNG sont écrits à la racine du projet avec les commandes ci-dessous.

## TEB en fonction du SNR : `teb.py`

```bash
python3 mesures/teb.py
```

Le script simule NRZ, RZ et NRZT, écrit les mesures dans `mesures/teb_resultats.json`, puis trace `courbes_teb.png` avec la courbe théorique NRZ. Les points dont le TEB vaut zéro restent dans le JSON, mais n'apparaissent pas sur l'axe logarithmique. Le JSON précédent n'est remplacé qu'après une campagne complète ; si une simulation échoue, il est conservé.

Les réglages se trouvent en haut de `teb.py` :

| Paramètre | Rôle | Valeur actuelle |
| --- | --- | --- |
| `NB_BITS` | Nombre de bits par simulation Java (`-mess`). | `999_999` |
| `NB_ECH` | Échantillons par bit (`-nbEch`). | `30` |
| `SNR_RANGE` | SNR par bit en dB (`-snrpb`) ; la borne de fin de `np.arange` est exclue. | `np.arange(5, 15, 0.1)` : de 5 à 14,9 dB |
| `FORMES` | Formes d'onde et amplitudes (`-form`, `-ampl`). | NRZ, RZ et NRZT |

Avec ces réglages, la campagne compte 100 SNR × 3 formes, soit 300 simulations Java. Chaque forme est traitée à son tour, avec jusqu'à quatre simulations simultanées. Avec la syntaxe actuelle de `-mess`, `1_000_000` serait interprété comme un message binaire fixe de sept bits : pour une longueur aléatoire, rester au plus à `999_999`.

Pour recréer seulement le graphique à partir du JSON déjà enregistré :

```bash
python3 mesures/teb.py --tracer
```

| Argument | Effet |
| --- | --- |
| Aucun | Simule, sauvegarde le JSON et trace le graphique. |
| `--tracer` | Lit le JSON et trace sans relancer Java. |
| `--donnees CHEMIN` | Choisit le JSON à écrire ou à relire ; par défaut `mesures/teb_resultats.json`. |
| `--image CHEMIN` | Choisit le PNG à créer ; par défaut `courbes_teb.png`. |
| `-h`, `--help` | Affiche l'aide. |

Par exemple :

```bash
python3 mesures/teb.py --donnees mesures/essai.json --image essai.png
python3 mesures/teb.py --tracer --donnees mesures/essai.json --image essai.png
```

Changer les constantes du script ne modifie pas les données déjà enregistrées : il faut relancer la simulation pour recalculer les TEB.

## Distribution du bruit : `pbruit.py`

```bash
python3 mesures/pbruit.py
```

Le script envoie un signal NRZ constant de bits à `1` via `./script/simulateur`, sauvegarde le signal reçu dans `out.txt`, puis soustrait `1` pour isoler le bruit. Il affiche la moyenne et la variance mesurées et crée `gaussienne_bruit.png`, qui contient un histogramme comparé à la densité gaussienne théorique et un graphique Q-Q (droite de Henry).

Ses réglages sont en haut de `pbruit.py` : `SNR_PB = 0.0` dB, `NB_BITS = 50_000`, `NB_ECH = 30` et `FICHIER_OUT = "out.txt"`. Cela produit 1 500 000 échantillons et une variance théorique de 15. Le script n'a pas d'arguments en ligne de commande. `out.txt` et le PNG sont écrits dans le répertoire depuis lequel la commande est lancée.

## TEB avec trajet indirect : `teb_multitrajet.py`

```bash
python3 mesures/teb_multitrajet.py
```

Pour chaque forme choisie, le script simule une grille de retards et d'amplitudes relatives du trajet indirect. La carte PNG utilise le retard en abscisse, l'amplitude relative en ordonnée et la couleur pour le TEB. Le CSV correspondant conserve chaque point, notamment le retard en échantillons et en fractions de période. Les cellules dont la simulation échoue restent sans TEB (`nan` dans le CSV).

Les paramètres se modifient en haut de `teb_multitrajet.py` :

| Paramètre | Rôle | Valeur actuelle |
| --- | --- | --- |
| `FORME` | Une des formes `NRZ`, `RZ`, `NRZT`, ou `TOUS`. | `TOUS` |
| `NB_ECH_BIT`, `NB_BITS` | Échantillons par bit et bits par simulation. | `30`, `1_000` |
| `SNR_DB`, `SEED` | SNR fixe et graine commune pour comparer les points. | `10.0` dB, `12345` |
| `MAX_ECH_RETARD`, `PAS_RETARD` | Retard maximal et pas, en échantillons. | `2 * NB_ECH_BIT` (soit `2T`), `1` |
| `PAS_ALPHA` | Pas de l'amplitude relative, de 0 à 1 inclus. | `0.1` |
| `MAX_WORKERS` | Nombre maximal de simulations Java simultanées. | `12` |

Le réglage actuel donne 61 retards × 11 amplitudes, soit 671 simulations par forme et 2 013 pour `TOUS`. Les formes sont traitées successivement. Le retard de `-ti` étant entier côté Java, garder `PAS_RETARD` entier pour éviter de recalculer plusieurs fois le même retard. Les CSV et PNG sont écrits à la racine du projet sous les noms indiqués dans le tableau initial.

Pour retracer les cartes à partir des CSV existants sans relancer Java :

```bash
python3 mesures/teb_multitrajet.py --tracer
python3 mesures/teb_multitrajet.py --tracer --donnees mesures/teb_multitrajet_nrz.csv
python3 mesures/teb_multitrajet.py --tracer --donnees mesures/teb_multitrajet_nrz.csv mesures/teb_multitrajet_rz.csv mesures/teb_multitrajet_nrzt.csv
```

Sans `--donnees`, le script cherche les CSV des formes sélectionnées par `FORME` à la racine du projet, puis dans `mesures/`. L'option `--donnees` accepte un ou plusieurs CSV précis. Le retard, l'amplitude, les échantillons par bit et le SNR proviennent de chaque CSV : les réglages actuels du script n'altèrent pas le graphique reconstitué. Les PNG sont enregistrés sous les noms habituels à la racine du projet.

Avec un seul CSV, `--image` permet de choisir un autre PNG sans remplacer le graphique existant :

```bash
python3 mesures/teb_multitrajet.py --tracer --donnees mesures/teb_multitrajet_nrz.csv --image carte_nrz_retracee.png
```

## TEB avec 1 à 5 trajets indirects : `teb_multitrajets.py`

```bash
python3 mesures/teb_multitrajets.py
```

Pour chaque forme d'onde, le script génère cinq cartes TEB disposées dans une grille de deux colonnes et trois lignes. Les cartes correspondent respectivement à 1, 2, 3, 4 et 5 trajets indirects ; elles partagent la même échelle de couleur. La sixième case contient cinq petits graphiques d'impulsions montrant les trajets envoyés à Java lorsque l'amplitude de référence vaut `0,5` et que le retard initial vaut `0`.

À chaque point des cartes, on choisit le retard du premier trajet `d₁`, l'amplitude de référence `α` et le nombre de trajets `N`. Avec l'intervalle fixe `Δ` et la constante de décroissance `τ`, le trajet `i` reçoit le retard `dᵢ = d₁ + (i − 1)Δ` et l'amplitude `aᵢ = α × exp(−(dᵢ − d₁)/τ)`. Le premier trajet conserve donc l'amplitude `α`, comme dans `teb_multitrajet.py` ; chaque trajet supplémentaire est atténué selon son retard par rapport au premier.

Les réglages sont en haut de `teb_multitrajets.py` : `FORME = "TOUS"`, `NB_ECH_BIT = 100`, `NB_BITS = 1_000`, `SNR_DB = 10.0`, `SEED = 12345`, `MAX_RETARD_INITIAL = 2 * NB_ECH_BIT`, `PAS_RETARD = 3`, `PAS_ALPHA = 0.01`, `INTERVALLE_ECH = NB_ECH_BIT // 3`, `DECROISSANCE_ECH = NB_ECH_BIT` et `MAX_WORKERS = 12`. La grille actuelle représente 67 retards × 101 amplitudes × 5 nombres de trajets, soit 33 835 simulations par forme et 101 505 pour `TOUS`.

Les CSV et PNG sont écrits à la racine du projet. Pour retracer les figures depuis les CSV sans relancer Java :

```bash
python3 mesures/teb_multitrajets.py --tracer
python3 mesures/teb_multitrajets.py --tracer --donnees teb_multitrajets_nrz.csv
python3 mesures/teb_multitrajets.py --tracer --donnees teb_multitrajets_nrz.csv --image carte_nrz_1_a_5.png
```

L'option `--donnees` accepte aussi plusieurs CSV de formes différentes. Le tracé utilise les paramètres enregistrés dans chaque CSV, même si les constantes du script ont changé depuis la simulation.
Les anciens CSV sans colonne `modele_amplitude` restent lisibles avec leur ancienne loi d'atténuation, fondée sur le retard absolu ; pour obtenir les cartes avec la loi corrigée, il faut relancer les simulations.
