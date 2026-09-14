# SIT213 – Simulation d'un système de transmission

**Noms :** Antonin Pons, Nathan Weymeels, Amandine Touchais, Aymeric Schaeffer  
**Formation :** FIP 2A – IMT Atlantique (2026-2027)

---

## Description
Projet de simulation d'une chaîne de transmission numérique (UV SIT210 / Module SIT213).  
À l'étape 1, la chaîne simule une transmission logique élémentaire non bruitée :
$$\text{Source} \longrightarrow \text{Transmetteur Parfait} \longrightarrow \text{Destination Finale}$$
Le canal étant parfait, le Taux d'Erreur Binaire (TEB) attendu est de 0.0.

---

## Structure du dépôt
* `src/` : code source Java (`sources`, `transmetteurs`, `destinations`, `simulateur`, `visualisations`, `information`).
* `bin/` : fichiers compilés `.class` (vide au départ).
* `docs/` : documentation Javadoc générée en HTML (vide au départ).
* `script/` : scripts d'automatisation du projet.

---

## Utilisation

1. **Compiler le projet :**
   ```bash
   ./script/compile
   ```
2. **Lancer une simulation :**
   ```bash
   ./script/simulateur [options]
   ```
   *Exemples :*
   * `./script/simulateur` : 100 bits aléatoires par défaut.
   * `./script/simulateur -mess 1010101` : message binaire fixé.
   * `./script/simulateur -mess 50` : message aléatoire de 50 bits.
   * `./script/simulateur -seed 42` : graine aléatoire pour rejouer à l'identique.
   * `./script/simulateur -s` : activation des sondes logiques graphiques.

3. **Lancer les tests automatiques :**
   ```bash
   ./script/runTests
   ```
4. **Générer la Javadoc :**
   ```bash
   ./script/genDoc
   ```
5. **Nettoyer le projet :**
   ```bash
   ./script/cleanAll
   ```
6. **Générer l'archive de livraison :**
   ```bash
   ./script/genDeliverable
   ```
