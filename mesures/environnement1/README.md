# SIT213 – Étape 6 : Cas d'étude client (Environnement 1)

Ce dossier regroupe le script d'automatisation, les calculs théoriques, les courbes et la fiche de synthèse pour l'oral de l'Environnement 1.

---

## 🎯 Rappel du Cahier des Charges

* **Canal de transmission** : Bruit Blanc Additif Gaussien (BBAG), sans multitrajets.
* **Plancher de bruit du récepteur** : $N_0 = -80\text{ dBm/Hz} = -110\text{ dBW/Hz} = 10^{-11}\text{ J}$.
* **Atténuation de la ligne** : $A = 40\text{ dB}$ (facteur en puissance $10^{40/10} = 10\,000$).
* **Exigence de qualité** : $\text{TEB} \le 10^{-3}$.
* **Batterie du capteur** : $E_{\text{batterie}} = 3\text{ Joules}$.
* **Trafic utile** : $10^6$ bits utiles transmis par jour.

---

## 📋 Réponses aux Questions du Client

### 1. Quelle solution proposez-vous et quelle est la consommation énergétique ?

#### Solution technique retenue : **NRZ antipolaire** (`-form NRZ -ampl -1 1`)
* **Justification théorique** :
  * Le récepteur de notre chaîne effectue une moyenne (intégration) sur la durée du symbole : c'est le **filtre adapté** parfait pour un symbole rectangulaire NRZ.
  * La modulation antipolaire ($\pm A$) maximise la distance euclidienne entre symboles ($d = 2A$) pour une puissance donnée, surclassant les modulations unipolaires ($d = A$) qui perdent $3\text{ dB}$.
  * Les modulations RZ et NRZT n'intègrent que sur une fraction du symbole (le tiers central), ce qui dégrade leur efficacité face au bruit blanc gaussien.

#### Calculs de dimensionnement :
1. **SNR requis au récepteur pour $\text{TEB} = 10^{-3}$** :
   $$\text{TEB} = Q\left(\sqrt{\frac{2 E_b}{N_0}}\right) \le 10^{-3} \implies \sqrt{\frac{2 E_b}{N_0}} \approx 3.090 \implies \frac{E_b}{N_0} \approx 4.775 \iff \mathbf{6.79\text{ dB}}$$
2. **Énergie par bit reçue au récepteur** :
   $$E_{b,\text{reçu}} = 4.775 \times N_0 = 4.775 \times 10^{-11}\text{ J}$$
3. **Énergie par bit émise par l'émetteur (compensation des 40 dB d'atténuation)** :
   $$E_{b,\text{émis}} = 10^4 \times E_{b,\text{reçu}} = 4.775 \times 10^{-7}\text{ J/bit} \approx \mathbf{0.478\ \mu\text{J / bit}}$$

---

### 2. Combien de jours tiendra la batterie de 3 Joules avant décharge ?

* **Consommation énergétique journalière** :
  $$E_{\text{jour}} = 10^6\text{ bits/jour} \times 4.775 \times 10^{-7}\text{ J/bit} \approx \mathbf{0.478\text{ J / jour}}$$
* **Autonomie de la batterie** :
  $$\text{Autonomie} = \frac{E_{\text{batterie}}}{E_{\text{jour}}} = \frac{3\text{ J}}{0.478\text{ J/jour}} \approx \mathbf{6.28\text{ jours}} \quad (\text{soit 6 jours et 7 heures})$$

---

### 3. Quel est l'impact énergétique du code correcteur d'erreurs (Étape 5) ? La batterie tiendra-t-elle plus longtemps ?

#### Réponse directe au client :
> **NON, la batterie ne tiendra PAS plus longtemps : elle tiendra MOINS longtemps (4.58 jours au lieu de 6.28 jours, soit une perte de 1.7 jour d'autonomie !)**

#### Démonstration technique :
1. **Principe du code de répétition $R_3$** :
   * Rendement de codage faible : $R_c = 1/3$ (on émet 3 bits canal pour 1 bit utile).
   * Décodage par vote majoritaire : $\text{TEB}_{\text{utile}} = 3 p^2 - 2 p^3 \approx 3 p^2$.
2. **Gain apparent sur le canal** :
   * Pour obtenir $\text{TEB}_{\text{utile}} \le 10^{-3}$, il suffit que le TEB sur le canal soit $p \le 1.83\%$.
   * Le SNR par bit canal requis descend donc à $\mathbf{3.39\text{ dB}}$ (gain apparent de $+3.40\text{ dB}$ sur le canal).
3. **Le coût réel par bit utile (pénalité de débit)** :
   * Comme il faut émettre 3 bits par bit utile, l'énergie utile requise est :
     $$E_{b,\text{utile}} = 3 \times E_{b,\text{canal}} \implies \left(\frac{E_{b,\text{utile}}}{N_0}\right)_{\text{dB}} = 3.39\text{ dB} + 10 \log_{10}(3) = 3.39 + 4.77 = \mathbf{8.16\text{ dB}}$$
   * Sans codeur : $\mathbf{6.79\text{ dB}}$ par bit utile.
   * **Surconsommation par bit utile** : $+1.37\text{ dB}$, soit **$+37.0\%$ d'énergie consommée en plus** !
4. **Impact sur l'autonomie** :
   * Consommation journalière : $10^6 \times 6.55 \times 10^{-7}\text{ J} \approx 0.655\text{ J/jour}$.
   * Autonomie : $\frac{3\text{ J}}{0.655\text{ J/jour}} \approx \mathbf{4.58\text{ jours}}$ (perte de 41 heures d'autonomie).

---

## 📊 Graphiques Générés (pour les slides de présentation)

Les graphiques sont générés automatiquement par `environnement1.py` :

1. **`1_formes_onde.png`** : Montre les courbes $\text{TEB} = f(E_b/N_0)$ et prouve que NRZ antipolaire atteint $10^{-3}$ dès **$6.79\text{ dB}$**, surclassant largement RZ, NRZT et NRZ unipolaire.
2. **`2_codeur.png`** : Compare le TEB utile en fonction de $E_{b,\text{utile}}/N_0$ avec et sans codeur. Met en évidence le décalage de **$+1.37\text{ dB}$** de surconsommation au point de fonctionnement $10^{-3}$.
3. **`3_batterie.png`** : Diagramme comparatif de l'autonomie (6.28 jours vs 4.58 jours) et de la consommation par bit ($0.48\ \mu\text{J}$ vs $0.65\ \mu\text{J}$).

---

## 💻 Utilisation des Scripts

```bash
# 1. Simuler avec le simulateur Java et générer les graphiques :
python3 mesures/environnement1/environnement1.py

# 2. Retracer les graphiques instantanément sans resimuler :
python3 mesures/environnement1/environnement1.py --tracer

# 3. Lancer les 4 simulations en direct devant le jury (démo oral) :
python3 mesures/environnement1/environnement1.py --demo
```

