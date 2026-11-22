# Étape 6 – Environnement 1

Ce document synthétise les réponses au cahier des charges de l'Environnement 1 et détaille les justifications physiques et mathématiques pour la soutenance orale.

---

## Paramètres imposés par le sujet

- Canal de transmission : Bruit blanc additif gaussien (BBAG), sans multitrajets
- Plancher de bruit du récepteur : $N_0 = -80\text{ dBm/Hz} = 10^{-11}\text{ J}$ (soit $-110\text{ dBW/Hz}$)
- Atténuation de la ligne : $A = 40\text{ dB}$ (facteur en puissance $10^{40/10} = 10\,000$)
- Exigence de qualité : $\text{TEB} \le 10^{-3}$
- Capacité de la batterie : $E_{\text{batterie}} = 3\text{ Joules}$
- Trafic journalier maximal contractuel : $10^6\text{ bits utiles / jour}$ (capteurs câblés)

---

## Question 1
**Quelle solution proposez-vous pour répondre au cahier des charges et quelle est la consommation énergétique de votre solution (en négligeant la consommation propre des composants électroniques de l’émetteur) ?**

### Solution technique retenue
**NRZ antipolaire** (`-form NRZ -ampl -1 1`), sans codage de canal.

---

### Dimensionnement énergétique (deux approches à exposer à l'oral)

#### 1. Dimensionnement au minimum théorique (réponse principale au cahier des charges)
Pour une modulation binaire antipolaire sur canal BBAG avec filtre adapté :
$$\text{TEB} = Q\left(\sqrt{\frac{2 E_b}{N_0}}\right) = \frac{1}{2} \text{erfc}\left(\sqrt{\frac{E_b}{N_0}}\right) = 10^{-3} \implies \sqrt{\frac{E_b}{N_0}} \approx 2.1851 \implies \frac{E_b}{N_0} \approx 4.7748 \iff \mathbf{6.79\text{ dB}}$$
- **Rapport $E_b/N_0$ minimal au récepteur** : **$6.79\text{ dB}$** ($4.7748$ en linéaire).
- **Énergie reçue par bit utile** : $E_{b,\text{reçu}} = 4.7748 \times 10^{-11}\text{ J}$ (avec $N_0 = 10^{-11}\text{ J}$).
- **Énergie émise par bit utile** (compensation des $40\text{ dB}$ d'atténuation, facteur $10^4$) :
  $$E_{b,\text{émis}} = 10\,000 \times E_{b,\text{reçu}} \approx \mathbf{0.4775\ \mu\text{J / bit}}$$

#### 2. Variante opérationnelle avec marge dans le modèle BBAG
Pour intégrer une marge face aux fluctuations statistiques et garantir un TEB moyen inférieur au seuil :
- **Point retenu** : **$7.00\text{ dB}$** ($E_b/N_0 = 5.0119$ en linéaire $\implies \text{TEB}_{\text{théorique}} \approx 7.73 \times 10^{-4}$).
- **Énergie émise par bit utile** :
  $$E_{b,\text{émis, marge}} = 10\,000 \times 5.0119 \times 10^{-11}\text{ J} \approx \mathbf{0.5012\ \mu\text{J / bit}}$$

---

### Justifications physiques et comparaisons (le pourquoi du comment)

#### Pourquoi le NRZ antipolaire surclasse-t-il les autres formes d'onde ?

1. **Filtre adapté optimal pour NRZ** :
   Dans notre simulateur, `Recepteur.java` effectue la moyenne arithmétique sur les $N_e$ échantillons du symbole ($\frac{1}{T_s} \int_0^{T_s} r(t) dt$). Pour une impulsion rectangulaire NRZ, cette intégration temporelle correspond exactement à la réponse impulsionnelle du filtre adapté $h(t) = s(T_s - t)$. Le récepteur est donc optimal au sens du rapport signal sur bruit en sortie.

2. **Distance euclidienne maximale vs NRZ unipolaire** :
   En modulation antipolaire ($\pm A$), la distance entre symboles est $d = 2A$. En unipolaire ($[0, A]$), elle n'est que de $d = A$. L'énergie du signal est partagée avec une composante continue ne transportant aucune information, induisant une pénalité théorique exacte de $10 \log_{10}(2) \approx \mathbf{3.01\text{ dB}}$ à TEB égal.

3. **Comportement réel du RZ unipolaire ($[0, A]$)** :
   Contrairement à une idée reçue, notre décodeur `CodeRZ.java` n'intègre pas le bruit sur les deux tiers où le signal est nul : il ignore les zones latérales et moyenne **uniquement le tiers central**.
   Pour une séquence de bits indépendants équiprobables, la puissance moyenne émise est $P_s = \frac{A^2}{6}$ (un bit '1' a une puissance de $A^2/3$, un bit '0' a une puissance nulle). Après intégration sur le tiers central, le TEB théorique est :
   $$\text{TEB} = Q\left(\sqrt{\frac{E_b}{N_0}}\right)$$
   Le RZ unipolaire présente donc exactement la même performance que le NRZ unipolaire, et subit la même pénalité de **$3.01\text{ dB}$** face au NRZ antipolaire (les courbes simulées orange et verte se superposent pour cette raison).

4. **Comportement du NRZT antipolaire ($[-A, A]$)** :
   Notre décodeur `CodeNRZT.java` moyenne lui aussi **uniquement le tiers central**, ignorant les transitions latérales.
   Pour une longue séquence de bits indépendants équiprobables avec $N_e = 30$ échantillons par bit, la puissance moyenne émise dépend des transitions et vaut en moyenne théorique discrète $P_s \approx 0.778333 A^2$ (très proche du modèle continu $7A^2/9 \approx 0.777778 A^2$).
   L'énergie présente dans les rampes latérales est émise et payée par la batterie, mais n'est pas exploitée par notre récepteur. Il en résulte une pénalité théorique d'environ **$3.68\text{ dB}$** face au NRZ antipolaire. Cette pénalité est propre à l'architecture de notre récepteur et non une limite absolue du format NRZT.

---

## Question 2
**Le système sera alimenté par une batterie stockant une énergie de 3 Joules. En sachant que l’émetteur ne transmettra pas plus de $10^6$ bits utiles par jour, avec votre solution, combien de jours tiendra la batterie avant décharge ?**

### Réponses chiffrées selon le dimensionnement

#### 1. Autonomie au minimum théorique (cohérente avec $6.79\text{ dB}$)
- Consommation journalière au trafic maximal ($10^6\text{ bits utiles/jour}$) :
  $$E_{\text{jour}} = 10^6 \times 4.7748 \times 10^{-7}\text{ J} \approx \mathbf{0.4775\text{ J / jour}}$$
- Autonomie de la batterie de $3\text{ Joules}$ :
  $$\text{Autonomie} = \frac{3\text{ J}}{0.47748\text{ J/jour}} \approx \mathbf{6.28\text{ jours}} \quad (\text{soit 6 jours, 6 heures et 47 minutes})$$

#### 2. Autonomie avec marge (cohérente avec $7.00\text{ dB}$)
- Consommation journalière :
  $$E_{\text{jour, marge}} = 10^6 \times 5.0119 \times 10^{-7}\text{ J} \approx \mathbf{0.5012\text{ J / jour}}$$
- Autonomie résultante :
  $$\text{Autonomie}_{\text{marge}} = \frac{3\text{ J}}{0.50119\text{ J/jour}} \approx \mathbf{5.99\text{ jours}} \quad (\text{soit environ 6 jours})$$

*Précision pour l'oral* : Ces calculs correspondent au trafic maximal contractuel sur un réseau câblé. Un trafic réel inférieur augmente l'autonomie. Conformément à l'énoncé, la consommation propre des composants électroniques en veille est négligée.

---

## Question 3
**Quel est l’impact énergétique du code correcteur d’erreur ? La batterie tiendra-t-elle plus longtemps pour le même TEB cible ?**

### Réponse directe
**Non, la batterie tiendra MOINS longtemps : elle durera 4.58 jours au lieu de 6.28 jours (perte sèche de 1.70 jour d'autonomie, soit -27.03 % d'autonomie).**

---

### Démonstration technique (le pourquoi du comment)

#### 1. Équivalence du code de l'étape 5
Le code développé ($0 \to 010$, $1 \to 101$) est équivalent à un code de répétition d'ordre 3 ($R_3$) après inversion systématique du bit central. Le vote majoritaire porte sur les bits $(a, \neg b, c)$.
Pour des erreurs de canal indépendantes de probabilité $p$, la probabilité d'erreur utile décodée est :
$$\text{TEB}_{\text{utile}} = \binom{3}{2} p^2 (1-p) + \binom{3}{3} p^3 = 3p^2 - 2p^3$$

#### 2. Gain apparent sur le canal
Pour garantir $\text{TEB}_{\text{utile}} \le 10^{-3}$, la résolution de $3p^2 - 2p^3 = 10^{-3}$ donne :
$$p \approx 0.018370 \approx 1.84\%$$
Pour du NRZ antipolaire, atteindre ce TEB sur le canal requiert :
$$\frac{E_c}{N_0} \approx 2.181200 \iff \mathbf{3.3870\text{ dB}}$$
Sur le canal seul, le seuil toléré est plus bas de $6.7895 - 3.3870 \approx \mathbf{+3.40\text{ dB}}$.

#### 3. Pénalité de débit et bilan net par bit utile
Comme le rendement de codage est $R_c = 1/3$, chaque bit utile nécessite la transmission de 3 bits canal. L'énergie requise par bit utile est donc $E_{b,\text{utile}} = 3 \times E_c$ :
$$\left(\frac{E_{b,\text{utile}}}{N_0}\right)_{\text{dB}} = \left(\frac{E_c}{N_0}\right)_{\text{dB}} + 10 \log_{10}(3) = 3.3870\text{ dB} + 4.7712\text{ dB} = \mathbf{8.1582\text{ dB}}$$

Comparaison par bit utile pour $\text{TEB} = 10^{-3}$ :
- Sans codeur : **$6.7895\text{ dB}$** ($0.4775\ \mu\text{J / bit utile}$)
- Avec codeur : **$8.1582\text{ dB}$** ($0.6544\ \mu\text{J / bit utile}$)
- **Surconsommation nette** : $+1.3686\text{ dB}$, soit **$+37.05\%$ d'énergie en plus**.

#### 4. Impact sur l'autonomie
- **Énergie émise par bit utile** (avec compensation des $40\text{ dB}$ d'atténuation de ligne, soit un facteur $10^4$) :
  $$E_{b,\text{utile},\text{émis}} = 3 \times E_{c,\text{émis}} = 3 \times \left(E_{c,\text{reçu}} \times 10^4\right) = 3 \times \left(2.181200 \times 10^{-11}\text{ J} \times 10^4\right) \approx \mathbf{0.65436\ \mu\text{J / bit utile}}$$
- **Consommation journalière pour $10^6$ bits utiles/jour** :
  $$E_{\text{jour, codeur}} = 10^6 \times E_{b,\text{utile},\text{émis}} \approx \mathbf{0.6544\text{ J / jour}}$$
- **Autonomie résultante** :
  $$\text{Autonomie}_{\text{codeur}} = \frac{3\text{ J}}{0.65436\text{ J/jour}} \approx \mathbf{4.58\text{ jours}} \quad (\text{perte de 1.70 jour, soit } \mathbf{-27.03\%})$$

*Conclusion pour l'oral* : Le code de répétition avec décodage dur induit un gain de codage net négatif à ce niveau de TEB, car la pénalité de débit ($+4.77\text{ dB}$) est supérieure à la marge gagnée sur le canal ($+3.40\text{ dB}$).

---

## Guide pour la démonstration en direct (Oral)

Commande d'exécution :
```bash
python3 mesures/environnement1/environnement1.py --demo
```

Conditions des simulations : $100\,000$ bits par essai, $N_e = 30$ échantillons par bit canal, graine fixée à $42$, canal BBAG sans multitrajets.

### Analyse des 4 résultats obtenus :

1. **NRZ antipolaire à $6.8\text{ dB}$** :
   ```bash
   java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -form NRZ -ampl -1 1 -snrpb 6.8
   ```
   *Mesure* : `TEB = 0.00107` (107 erreurs sur 100 000 bits).  
   *Commentaire oral* : La valeur théorique vaut environ $9.88 \times 10^{-4}$ ($9.875 \times 10^{-4}$). À $100\,000$ bits, l'incertitude relative approximative à $95\%$ sous le modèle binomial autour de $\text{TEB} = 10^{-3}$ est d'environ $\pm 20\%$ ($\sigma \approx 10$ erreurs). La mesure de 107 erreurs est pleinement compatible avec la théorie. Pour disposer d’une marge par rapport au seuil théorique, on peut retenir 7.0 dB : le TEB théorique vaut alors environ $7.73 \times 10^{-4}$, pour une autonomie de 5.99 jours. Cette marge réduit le risque de dépassement du seuil sur un essai fini.

2. **RZ unipolaire à $6.8\text{ dB}$** :
   ```bash
   java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -form RZ -ampl 0 1 -snrpb 6.8
   ```
   *Mesure* : `TEB = 0.01452`.  
   *Commentaire oral* : TEB environ 14 fois plus dégradé, parfaitement conforme à la théorie du RZ unipolaire ($1.43 \times 10^{-2}$).

3. **NRZT antipolaire à $6.8\text{ dB}$** :
   ```bash
   java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -form NRZT -ampl -1 1 -snrpb 6.8
   ```
   *Mesure* : `TEB = 0.02188`.  
   *Commentaire oral* : TEB environ 20 fois plus dégradé, conforme à la théorie ($2.14 \times 10^{-2}$) liée à la décision sur le tiers central.

4. **NRZ antipolaire avec codeur à $3.4\text{ dB}$ canal** (soit $8.2\text{ dB}$ par bit utile) :
   ```bash
   java -cp bin simulateur.Simulateur -mess 100000 -seed 42 -form NRZ -ampl -1 1 -codeur -snrpb 3.4
   ```
   *Mesure* : `TEB = 0.00101` (101 erreurs sur 100 000 bits utiles).  
   *Commentaire oral* : Confirme expérimentalement que le codeur nécessite $3.4\text{ dB}$ par bit canal pour approcher $10^{-3}$, soit une dépense réelle de $8.2\text{ dB}$ par bit utile ($+1.37\text{ dB}$ par rapport aux $6.8\text{ dB}$ sans codeur).

---

## Graphiques pour les diapositives

- `1_formes_onde.png` : Comparaison des formes d'onde face au seuil de $10^{-3}$ à $6.79\text{ dB}$.
- `2_codeur.png` : Comparaison sans et avec codeur par bit utile, illustrant le surcoût de $+1.37\text{ dB}$ à $\text{TEB} = 10^{-3}$.
- `3_batterie.png` : Comparaison de l'autonomie ($6.28\text{ jours}$ vs $4.58\text{ jours}$) et de l'énergie par bit utile ($0.48\ \mu\text{J}$ vs $0.65\ \mu\text{J}$).
