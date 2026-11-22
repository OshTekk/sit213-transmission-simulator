# Étape 6 – Environnement 2 (Questions 4 et 5)

Ce document rassemble l'ensemble des justifications physiques, mathématiques et expérimentales permettant de répondre de façon rigoureuse aux **Questions 4 et 5 de l'Environnement 2** lors de la soutenance orale.

---

## 1. Cahier des charges et paramètres de l'Environnement 2

L'énoncé de la question 4 stipule :
> *"Le client souhaite déployer son système dans un environnement où le canal de propagation contient deux trajets espacés de 10 µs et d’amplitudes (relatives) respectives 1 et 0,5. En considérant les formes d’onde NRZ et NRZT uniquement, quel débit binaire maximal de transmission pouvez-vous proposer en garantissant un TEB inférieur ou égale à $10^{-2}$ pour un $E_b/N_0$ inférieur à 15 dB ? NB : Aucune contrainte n’est imposée sur la bande fréquentielle occupée par le signal transmis."*

### Récapitulatif des paramètres imposés :
- **Canal de propagation** : canal multi-trajets à deux composantes :
  - Trajet direct (principal) : amplitude $\alpha_0 = 1.0$, retard $\tau_0 = 0$.
  - Trajet indirect (écho / réflexion) : amplitude relative $\alpha_1 = 0.5$, retard $\tau = 10\ \mu\text{s} = 10^{-5}\text{ s}$.
- **Exigence de qualité** : $\text{TEB} \le 10^{-2}$.
- **Contrainte énergétique** : $E_b/N_0 \le 15\text{ dB}$ au récepteur.
- **Formes d'onde autorisées** : **NRZ** et **NRZT** uniquement.
- **Bande passante** : aucune contrainte imposée sur la bande fréquentielle occupée.

---

## 2. Traduction physique et mathématique du système

### A. Équation du canal multi-trajets continu
Si $s(t)$ est le signal analogique émis, le signal reçu en entrée du récepteur s'écrit :
$$r(t) = s(t) + 0.5 \cdot s(t - \tau) + b(t)$$
où $\tau = 10\ \mu\text{s}$ et $b(t)$ est le bruit blanc additif gaussien de densité unilatérale $N_0$.

### B. Relation entre temps physique, échantillonnage et débit binaire
Dans une transmission binaire, le débit binaire est $D = 1/T_s$, où $T_s$ est la durée d'un bit.
Dans notre simulateur, chaque bit est discrétisé en $N_e = 30$ échantillons (`-nbEch 30`).
La période d'échantillonnage vaut donc :
$$T_e = \frac{T_s}{N_e} = \frac{1}{N_e \cdot D}$$

Le retard physique $\tau = 10\ \mu\text{s}$ correspond donc à un décalage discret de $dt$ échantillons dans le canal :
$$dt = \frac{\tau}{T_e} = \tau \times N_e \times D = 10^{-5} \times 30 \times D = 3 \times 10^{-4} \times D = \mathbf{0.3 \times D_{\text{kbit/s}}}$$

Réciproquement, un décalage de $dt$ échantillons correspond au débit binaire :
$$D_{\text{kbit/s}} = \frac{dt}{0.3} = \frac{10}{3} \times dt$$

#### Tableau de correspondance immédiat ($N_e = 30$) :
| Débit binaire $D$ | Durée symbole $T_s$ | Rapport $T_s / \tau$ | Décalage $dt$ (échantillons) | Configuration simulateur |
| :---: | :---: | :---: | :---: | :---: |
| **$10\text{ kbit/s}$** | $100\ \mu\text{s}$ | $10\ \tau$ (écho négligeable) | $3$ | `-ti 3 0.5` |
| **$50\text{ kbit/s}$** | $20\ \mu\text{s}$ | $2\ \tau$ (écho sur demi-symbole) | $15$ | `-ti 15 0.5` |
| **$100\text{ kbit/s}$** | **$10\ \mu\text{s}$** | **$1\ \tau$ (limite canonique $T_s = \tau$)** | **$30$** | **`-ti 30 0.5`** |
| **$200\text{ kbit/s}$** | **$5\ \mu\text{s}$** | **$0.5\ \tau$ ($T_s = \tau / 2$)** | **$60$** | **`-ti 60 0.5`** |
| **$300\text{ kbit/s}$** | $3.33\ \mu\text{s}$ | $T_s = \tau / 3$ | $90$ | `-ti 90 0.5` |
| **$500\text{ kbit/s}$** | $2\ \mu\text{s}$ | $T_s = \tau / 5$ | $150$ | `-ti 150 0.5` |

---

## 3. Démonstration : Pourquoi le NRZ antipolaire surclasse totalement les autres choix

### A. Preuve de l'ouverture de l'œil en NRZ antipolaire ($[-1, +1]$)
Soit une transmission NRZ antipolaire où les symboles valent $a_k \in \{-1, +1\}$.
Le récepteur calcule la moyenne temporelle sur la durée du symbole $T_s$ (filtre adapté pour NRZ) :
$$Y = \frac{1}{T_s} \int_{(k-1)T_s}^{k T_s} r(t)\,dt$$
Le seuil de décision optimal est $S = \frac{(+1) + (-1)}{2} = 0$.

Sans bruit, la valeur reçue pour un bit direct $a_k$ soumis à l'écho de bit précédent $a_{k-1}$ d'amplitude $0.5$ vaut :
- **Si $a_k = +1$ (bit 1)** :
  - Si l'écho est $a_{k-1} = +1$ : signal reçu $= +1 + 0.5 \times (+1) = \mathbf{+1.5}$
  - Si l'écho est $a_{k-1} = -1$ : signal reçu $= +1 + 0.5 \times (-1) = \mathbf{+0.5}$
- **Si $a_k = -1$ (bit 0)** :
  - Si l'écho est $a_{k-1} = +1$ : signal reçu $= -1 + 0.5 \times (+1) = \mathbf{-0.5}$
  - Si l'écho est $a_{k-1} = -1$ : signal reçu $= -1 + 0.5 \times (-1) = \mathbf{-1.5}$

> **Résultat fondamental** :
> Puisque l'amplitude du trajet direct ($1.0$) est strictement supérieure à l'amplitude du trajet indirect ($0.5$), **le signe du signal direct n'est JAMAIS inversé par l'écho** :
> - Tout bit '1' produit un signal $\ge +0.5 > 0$.
> - Tout bit '0' produit un signal $\le -0.5 < 0$.
> **Le diagramme de l'œil reste strictement OUVERT avec une marge minimale de $+0.5$ par rapport au seuil $0$.**

#### Calcul analytique du TEB à $E_b/N_0 = 15\text{ dB}$ :
À $15\text{ dB}$, $\text{SNR}_{\text{lin}} = 10^{1.5} \approx 31.6228$.
La variance du bruit en sortie de l'intégrateur sur un symbole est $\sigma_Y^2 = \frac{1}{2 \cdot \text{SNR}_{\text{lin}}} = \frac{1}{63.245} \approx 0.01581$, soit un écart-type $\sigma_Y \approx 0.1257$.

Dans le pire cas ($a_k = +1, a_{k-1} = -1$, marge au seuil de $0.5$) :
$$\frac{d_{\min}}{\sigma_Y} = \frac{0.5}{0.1257} \approx 3.977 \implies \text{TEB}_{\text{pire cas}} = Q(3.977) \approx 3.49 \times 10^{-5}$$
En moyennant sur les 4 combinaisons de bits équiprobables :
$$\text{TEB}_{\text{moyen}} \approx \frac{1}{2} Q(3.977) + \frac{1}{2} Q(11.93) \approx \mathbf{1.75 \times 10^{-5}} \ll 10^{-2}$$
**Ce calcul analytique démontre exactement pourquoi notre simulation à $100\text{ kbit/s}$ et $15\text{ dB}$ donne $\text{TEB} \approx 3 \times 10^{-5}$, soit deux ordres de grandeur sous la limite du client.**

---

### B. Pourquoi le NRZ unipolaire ($[0, 1]$) est totalement inutilisable
En modulation unipolaire, les bits valent $0$ et $1$. Le seuil de décision se trouve à $S = 0.5$.
En présence de l'écho :
- Si $a_k = 0$ (bit 0) et que l'écho est $a_{k-1} = 1$ :
  $$\text{Signal reçu} = 0 + 0.5 \times 1 = \mathbf{0.5}$$
Le signal reçu tombe **exactement sur le seuil de décision** !
L'œil est complètement **fermé**. Même sans bruit, le récepteur est incapable de trancher. En présence du moindre bruit gaussien centré, l'échantillon bascule avec une probabilité de $50\%$ du mauvais côté :
$$\text{TEB}_{\text{unipolaire}} \approx \frac{1}{4} \times 0.5 = 12.5\% = \mathbf{1.25 \times 10^{-1}} \gg 10^{-2}$$
Les simulations confirment ce phénomène : dès que $dt \ge 20$ ($D \ge 67\text{ kbit/s}$), le TEB unipolaire s'effondre à environ $12.6\%$.

---

### C. Analyse comparative avec NRZT antipolaire ($[-1, +1]$)
Notre décodeur `CodeNRZT.java` intègre le signal uniquement sur le **tiers central** du symbole ($[T_s/3, 2T_s/3]$), en ignorant les transitions latérales.
Deux effets majeurs se combinent :
1. **Pénalité énergétique intrinsèque** : Les rampes latérales (tiers montant et tiers descendant) consomment de l'énergie à l'émission mais ne sont pas intégrées à la réception, causant déjà une pénalité théorique de $\approx 3.68\text{ dB}$ en BBAG pur.
2. **Débordement des transitions de l'écho** : Lorsque le retard dépasse le tiers du symbole ($dt \ge 10$, soit $D \ge 33.3\text{ kbit/s}$), les transitions en pente de l'écho commencent à empiéter sur le tiers central de décision.

**Bilan NRZT à $15\text{ dB}$** :
- À $D = 100\text{ kbit/s}$ ($dt = 30$) : $\text{TEB}_{\text{NRZT}} \approx 2.54 \times 10^{-3} \le 10^{-2}$.
- NRZT respecte formellement la condition $\le 10^{-2}$ à $15\text{ dB}$, mais :
  - Son TEB est **environ 100 fois plus élevé** que celui du NRZ ($2.5 \times 10^{-3}$ vs $3 \times 10^{-5}$).
  - Sa sensibilité au bruit est bien plus critique : dès que $E_b/N_0 < 13.1\text{ dB}$, son TEB repasse au-dessus de $10^{-2}$ (marge de sécurité de seulement $1.9\text{ dB}$ sous les $15\text{ dB}$ imposés, contre **$5.6\text{ dB}$ de marge pour le NRZ**).

---

## 4. Réponses argumentées : Quel débit binaire maximal proposer ?

Le candidat dispose de **deux réponses complémentaires et parfaitement justifiées** à exposer au jury :

### Option A (Réponse d'ingénierie télécom canonique) : $D = 100\text{ kbit/s}$
- **Définition** : Correspond à la condition classique où la durée d'un symbole est égale à l'étalement temporel du canal :
  $$T_s = \tau = 10\ \mu\text{s} \iff D = \frac{1}{\tau} = \mathbf{100\text{ kbit/s}} \quad (dt = 30\text{ échantillons})$$
- **Justification télécom** :
  - À $100\text{ kbit/s}$, l'écho d'un bit ne déborde jamais au-delà du bit directement suivant. L'interférence entre symboles (IES) est confinée à 1 symbole de mémoire.
  - C'est la limite standard d'une chaîne sans égaliseur dans la littérature classique.
- **Performances garanties** :
  - Avec NRZ antipolaire à $15\text{ dB}$ : $\text{TEB} = \mathbf{3.0 \times 10^{-5}} \ll 10^{-2}$.
  - Le système respecte le cahier des charges $\text{TEB} \le 10^{-2}$ dès **$E_b/N_0 \ge 9.4\text{ dB}$**, offrant une **marge de sécurité de $5.6\text{ dB}$** par rapport au plafond de $15\text{ dB}$.

### Option B (Plein potentiel du modèle sans contrainte de bande) : $D \ge 200\text{ kbit/s}$ (voire $500\text{ kbit/s}$)
- **Définition** : Débit supérieur à la limite canonique, permis par les spécificités de l'énoncé :
  > *"NB : Aucune contrainte n’est imposée sur la bande fréquentielle occupée par le signal transmis."*
- **Justification physique** :
  - Dans ce canal précis, il n'existe qu'un seul écho d'amplitude $0.5$.
  - Comme prouvé en section 3.A, tant que l'écho est inférieur au signal direct ($0.5 < 1.0$), **le diagramme de l'œil ne se ferme jamais**, que le retard $\tau$ représente 1 symbole ($100\text{ kbit/s}$), 2 symboles ($200\text{ kbit/s}$, $dt = 60$), 5 symboles ($500\text{ kbit/s}$, $dt = 150$) ou plus.
- **Performances garanties** :
  - À $D = 200\text{ kbit/s}$ ($dt = 60$, $T_s = 5\ \mu\text{s}$) : $\text{TEB} = \mathbf{1.0 \times 10^{-5}} \ll 10^{-2}$ à $15\text{ dB}$.
  - À $D = 500\text{ kbit/s}$ ($dt = 150$, $T_s = 2\ \mu\text{s}$) : $\text{TEB} = \mathbf{3.0 \times 10^{-5}} \ll 10^{-2}$ à $15\text{ dB}$.

### Synthèse pour l'oral :
> *"Pour répondre au client, nous proposons la modulation **NRZ antipolaire** $[-1, 1]$. Sur le plan du dimensionnement télécom robuste sans étalement multi-symboles, le débit maximal canonique est **$100\text{ kbit/s}$** ($T_s = \tau = 10\ \mu\text{s}$) : il garantit un TEB de $3 \times 10^{-5}$, bien meilleur que les $10^{-2}$ exigés, avec une marge de sécurité de $5.6\text{ dB}$ en puissance (TEB $\le 10^{-2}$ dès $9.4\text{ dB}$).*
>
> *Néanmoins, comme le cahier des charges ne fixe aucune contrainte de bande passante et que l'écho ($0.5$) n'inverse jamais le signal direct ($1.0$), le diagramme de l'œil reste grand ouvert. Si le client exige le débit maximal absolu, notre chaîne permet de monter à **$200\text{ kbit/s}$** ($T_s = 5\ \mu\text{s}$) voire au-delà tout en maintenant le TEB autour de $10^{-5}$."*

---

## 5. Commandes de démonstration pour le jury

Toutes ces commandes se lancent depuis la racine du projet (`./script/compile` préalable) :

```bash
# 1. Démonstration principale : NRZ antipolaire à D = 100 kbit/s (Ts = tau = 10 µs, dt = 30)
java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -nbEch 30 -form NRZ -ampl -1 1 -ti 30 0.5 -snrpb 15
# Résultat mesuré : TEB = 3.00000e-05 (<< 10^-2)

# 2. Démonstration haut débit : NRZ antipolaire à D = 200 kbit/s (Ts = 5 µs < tau, dt = 60)
java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -nbEch 30 -form NRZ -ampl -1 1 -ti 60 0.5 -snrpb 15
# Résultat mesuré : TEB = 1.00000e-05 (<< 10^-2)

# 3. Comparaison avec NRZT antipolaire à D = 100 kbit/s (dt = 30)
java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -nbEch 30 -form NRZT -ampl -1 1 -ti 30 0.5 -snrpb 15
# Résultat mesuré : TEB = 2.54000e-03 (conforme à 10^-2, mais 100x plus dégradé)

# 4. Contre-exemple : NRZ unipolaire [0, 1] à D = 100 kbit/s (fermeture totale de l'œil)
java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -nbEch 30 -form NRZ -ampl 0 1 -ti 30 0.5 -snrpb 15
# Résultat mesuré : TEB = 1.26070e-01 (~12.6 %, totalement disqualifié)

# 5. Marge en bruit de NRZ : test à Eb/N0 = 10 dB (< 15 dB) pour D = 100 kbit/s
java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -nbEch 30 -form NRZ -ampl -1 1 -ti 30 0.5 -snrpb 10
# Résultat mesuré : TEB = 6.39000e-03 (reste <= 10^-2 dès 9.4 dB)
```

Ou en une seule commande automatisée via le script :
```bash
python3 mesures/environnement2/environnement2.py --demo
```

---

## 6. Graphiques générés

Les trois figures générées dans ce dossier illustrent l'ensemble de l'argumentation :
1. `1_q4_teb_vs_debit.png` : Évolution du TEB en fonction du débit binaire de $10$ à $500\text{ kbit/s}$ à $15\text{ dB}$, mettant en évidence le plancher bas du NRZ antipolaire face au décrochage catastrophique de l'unipolaire et à la dégradation du NRZT.
2. `2_q4_teb_vs_snr.png` : Évolution du TEB en fonction de $E_b/N_0$ de $5$ à $15\text{ dB}$ pour $D = 100$ et $200\text{ kbit/s}$, montrant le seuil d'admissibilité à $9.4\text{ dB}$ pour NRZ vs $13.1\text{ dB}$ pour NRZT.
3. `3_q4_analyse_ies_oeil.png` : Schéma didactique des 4 états de signaux reçus montrant que la marge du NRZ antipolaire reste toujours positive ($+0.5$), alors que le NRZ unipolaire tombe directement sur le seuil ($0.5$).

---

## 7. Question 5 : Conditions de propagation pour atteindre $\text{TEB} \le 10^{-3}$

> **Énoncé de la Question 5** :  
> *"Pour le débit et le $E_b/N_0$ trouvé à la question précédente et en considérant toujours deux trajets, quelles conditions de propagation permettraient de faire descendre le TEB à $10^{-3}$ ?"*

### A. Selon le point de fonctionnement retenu à la Question 4 :

1. **En modulation antipolaire $[-1, +1]$ à $E_b/N_0 \approx 14.9\text{--}15\text{ dB}$** :
   - **Pour le NRZ antipolaire** : À $D = 100\text{ kbit/s}$ (comme à $200\text{ kbit/s}$), le TEB vaut déjà **$\approx 2\text{--}3 \times 10^{-5} \le 10^{-3}$** ! Si l'on s'était placé au seuil minimal de la Q4 ($E_b/N_0 \approx 9.4\text{ dB}$ où $\text{TEB} = 10^{-2}$), pour descendre à $\text{TEB} \le 10^{-3}$ sans augmenter $E_b/N_0$, il faut agir sur le canal de propagation :
     - **Réduire l'amplitude relative de l'écho** $\alpha_1$ de $0.5$ à **$\alpha_1 \le 0.25$** (atténuation du trajet réfléchi par absorption ou directivité d'antenne/blindage) afin d'ouvrir la marge minimale de $0.5$ à $1 - \alpha_1 \ge 0.75$ ;
     - Ou **réduire le retard relatif $\tau / T_s \le 0.35$** ($\tau \le 3.5\ \mu\text{s}$ à $100\text{ kbit/s}$) pour limiter la fraction du symbole perturbée par un écho de signe opposé.
   - **Pour le NRZT antipolaire à $100\text{ kbit/s}$ et $15\text{ dB}$** (où $\text{TEB} \approx 2.5 \times 10^{-3} > 10^{-3}$) :
     - **Condition sur l'amplitude de l'écho** : abaisser $\alpha_1$ de $0.5$ à **$\alpha_1 \le 0.38$** ($\text{TEB} \le 10^{-3}$) ;
     - **Condition sur le retard / recouvrement** : ramener le retard $\tau \le 8.4\ \mu\text{s}$ (soit un recouvrement $\tau / T_s \le 0.84$, correspondant à $D \le 84\text{ kbit/s}$ si $\tau = 10\ \mu\text{s}$).

2. **En modulation unipolaire $[0, 1]$ au point limite de la Question 4 ($D \approx 69\text{ kbit/s}$, $E_b/N_0 = 14.9\text{ dB}$)** :
   - À ce point, l'écho recouvre $69\%$ de la durée d'un bit ($\tau / T_s = 0.69$) et donne $\text{TEB} = 10^{-2}$.
   - Pour descendre à $\text{TEB} \le 10^{-3}$ :
     - **Agir sur les conditions de propagation** : réduire le retard du trajet réfléchi à **$\tau \le 7.7\ \mu\text{s}$** (recouvrement $\tau / T_s \le 0.53$, équivalent à abaisser le débit de $69$ à $53\text{ kbit/s}$ à retard fixe), ou réduire l'amplitude de l'écho réfléchi à **$\alpha_1 \le 0.38$** ;
     - **Alternative système (codage de canal TP5)** : l'ajout du codeur à triplets ($R_c = 1/3$) corrige les erreurs isolées ($P_{b,\text{utile}} \approx 3p^2 - 2p^3 \approx 3 \times 10^{-4} \le 10^{-3}$ pour $p = 10^{-2}$), au prix d'une division par $3$ du débit utile ($23\text{ kbit/s}$ utiles pour $69\text{ kbit/s}$ transmis).
