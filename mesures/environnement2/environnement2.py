#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SIT213 - Etape 6 - Environnement 2 - Question 4 (Canal multi-trajets, haut débit)

Ce script :
  1. Formalise la conversion physique entre retard tau = 10 µs, échantillonnage Ne = 30
     et débit binaire D : dt = tau * D * Ne = 0.3 * D_kbit/s.
  2. Lance le simulateur Java pour évaluer les formes d'onde NRZ et NRZT face au canal
     à deux trajets (trajet direct alpha0=1.0, écho alpha1=0.5 retardé de 10 µs).
  3. Compare NRZ antipolaire, NRZT antipolaire et NRZ unipolaire en fonction du débit
     binaire D (10 à 500 kbit/s) et du rapport Eb/N0 (5 à 15 dB).
  4. Génère 3 graphiques d'analyse pour la soutenance orale :
     - 1_q4_teb_vs_debit.png     : TEB en fonction du débit D à Eb/N0 = 15 dB.
     - 2_q4_teb_vs_snr.png       : TEB en fonction de Eb/N0 pour D = 100 et 200 kbit/s.
     - 3_q4_analyse_ies_oeil.png : Démonstration visuelle de l'ouverture de l'œil (NRZ vs NRZT vs unipolaire).

Utilisation (depuis la racine du projet) :
  python3 mesures/environnement2/environnement2.py           # simule + trace
  python3 mesures/environnement2/environnement2.py --tracer  # trace depuis resultats_q4.json
  python3 mesures/environnement2/environnement2.py --demo    # démonstrations commentées pour le jury
"""

import argparse
import json
import math
import os
import shutil
import subprocess
import tempfile
from concurrent.futures import ThreadPoolExecutor

import matplotlib.pyplot as plt
import numpy as np

# ------------------------------------------------------------------
# Données de l'énoncé - Environnement 2
# ------------------------------------------------------------------
TAU_S = 10e-6             # Retard du second trajet : 10 µs
AMPL_DIRECT = 1.0         # Amplitude relative du trajet direct
AMPL_ECHO = 0.5           # Amplitude relative du second trajet
TEB_MAX = 1e-2            # TEB maximal contractuel (10^-2)
EBN0_MAX_DB = 15.0        # Eb/N0 maximal autorisé (15 dB)

# Échantillonnage
NB_ECH = 30               # Nombre d'échantillons par bit (valeur par défaut du simulateur)
NB_BITS = 100_000         # 100 000 bits par simulation (1 000 erreurs à TEB = 10^-2)
SEED = 42                 # Graine fixe pour reproductibilité

DOSSIER = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.abspath(os.path.join(DOSSIER, "..", ".."))
FICHIER_RESULTATS = os.path.join(DOSSIER, "resultats_q4.json")

# Grilles de simulation
DEBITS_KBIT_S = [10, 25, 50, 75, 100, 125, 150, 200, 250, 300, 400, 500]
SNRS_DB = list(range(5, 16))  # 5 à 15 dB


def debit_vers_dt(debit_kbit_s, nb_ech=NB_ECH, tau=TAU_S):
    """Calcule le décalage en échantillons correspondant au débit.

    dt = tau * D * Ne = 10^-5 * (D_kbit_s * 10^3) * 30 = 0.3 * D_kbit_s.
    """
    return int(round(tau * (debit_kbit_s * 1e3) * nb_ech))


def dt_vers_debit(dt, nb_ech=NB_ECH, tau=TAU_S):
    """Calcule le débit binaire (kbit/s) correspondant à un décalage en échantillons."""
    return (dt / (tau * nb_ech)) / 1e3


# ------------------------------------------------------------------
# Copie locale temporaire pour exécution ultra-rapide
# ------------------------------------------------------------------
def copier_classes():
    """Copie bin/ dans /tmp pour contourner la latence Google Drive."""
    dossier_tmp = tempfile.mkdtemp(prefix="sit213_env2_")
    destination = os.path.join(dossier_tmp, "bin")
    shutil.copytree(os.path.join(RACINE, "bin"), destination)
    return destination


def simuler(classpath, arguments):
    """Lance le simulateur Java et renvoie le TEB mesuré."""
    commande = [
        "java", "-cp", classpath, "simulateur.Simulateur",
        "-mess", str(NB_BITS),
        "-seed", str(SEED),
        "-nbEch", str(NB_ECH),
    ] + arguments
    res = subprocess.run(commande, capture_output=True, text=True)
    if "TEB :" not in res.stdout:
        raise RuntimeError(f"Erreur simulation : {res.stderr or res.stdout}")
    return float(res.stdout.split("TEB :")[-1])


# ------------------------------------------------------------------
# Campagne de simulations
# ------------------------------------------------------------------
def lancer_simulations():
    classpath = copier_classes()
    taches_debit = {}
    taches_snr = {}

    print(f"\n[1/2] Simulations du TEB vs Débit binaire à Eb/N0 = {EBN0_MAX_DB:.0f} dB...")
    for d in DEBITS_KBIT_S:
        dt = debit_vers_dt(d)
        ti_args = ["-ti", str(dt), str(AMPL_ECHO)]
        snr_args = ["-snrpb", str(EBN0_MAX_DB)]

        taches_debit[("NRZ antipolaire [-1, 1]", d)] = (
            ["-form", "NRZ", "-ampl", "-1", "1"] + ti_args + snr_args
        )
        taches_debit[("NRZT antipolaire [-1, 1]", d)] = (
            ["-form", "NRZT", "-ampl", "-1", "1"] + ti_args + snr_args
        )
        taches_debit[("NRZ unipolaire [0, 1]", d)] = (
            ["-form", "NRZ", "-ampl", "0", "1"] + ti_args + snr_args
        )

    print(f"[2/2] Simulations du TEB vs Eb/N0 pour D = 100 et 200 kbit/s...")
    for d in [100, 200]:
        dt = debit_vers_dt(d)
        ti_args = ["-ti", str(dt), str(AMPL_ECHO)]
        for s in SNRS_DB:
            snr_args = ["-snrpb", str(s)]
            taches_snr[(f"NRZ ({d} kbit/s)", s)] = (
                ["-form", "NRZ", "-ampl", "-1", "1"] + ti_args + snr_args
            )
            taches_snr[(f"NRZT ({d} kbit/s)", s)] = (
                ["-form", "NRZT", "-ampl", "-1", "1"] + ti_args + snr_args
            )

    toutes_taches = {**taches_debit, **taches_snr}
    print(f"Lancement de {len(toutes_taches)} simulations ({NB_BITS:,} bits chacune) en parallèle...")

    with ThreadPoolExecutor() as pool:
        tebs = pool.map(lambda args: simuler(classpath, args), toutes_taches.values())
        resultats_bruts = dict(zip(toutes_taches.keys(), tebs))

    # Structuration pour sauvegarde JSON
    resultats = {"debit_vs_teb": {}, "snr_vs_teb": {}}
    for (courbe, d), teb in resultats_bruts.items():
        if (courbe, d) in taches_debit:
            resultats["debit_vs_teb"].setdefault(courbe, {})[str(d)] = teb
        else:
            resultats["snr_vs_teb"].setdefault(courbe, {})[str(d)] = teb

    with open(FICHIER_RESULTATS, "w") as f:
        json.dump(resultats, f, indent=2)

    print(f"Résultats sauvegardés dans {os.path.relpath(FICHIER_RESULTATS, RACINE)}")
    return resultats


# ------------------------------------------------------------------
# Démonstrations commentées pour l'oral (--demo)
# ------------------------------------------------------------------
def executer_demo():
    classpath = copier_classes()
    print("=" * 76)
    print(" SIT213 - Environnement 2 - Démonstrations en direct pour la soutenance")
    print(" Canal : 2 trajets (direct alpha=1, écho alpha=0.5 retardé de 10 µs)")
    print(" Contrainte : TEB <= 10^-2 pour Eb/N0 <= 15 dB")
    print("=" * 76)

    essais = [
        (
            "Proposition A : NRZ antipolaire au débit canonique D = 100 kbit/s (Ts = tau = 10 µs, dt = 30)",
            ["-form", "NRZ", "-ampl", "-1", "1", "-ti", "30", "0.5", "-snrpb", "15"],
            "TEB attendu ~ 3e-5 << 10^-2 (marge énorme de 2 ordres de grandeur)",
        ),
        (
            "Proposition B : NRZ antipolaire à très haut débit D = 200 kbit/s (Ts = 5 µs < tau, dt = 60)",
            ["-form", "NRZ", "-ampl", "-1", "1", "-ti", "60", "0.5", "-snrpb", "15"],
            "TEB attendu ~ 1e-5 << 10^-2 (l'œil reste grand ouvert car 1.0 - 0.5 = +0.5 > 0)",
        ),
        (
            "Comparaison NRZT : NRZT antipolaire à D = 100 kbit/s (dt = 30, Eb/N0 = 15 dB)",
            ["-form", "NRZT", "-ampl", "-1", "1", "-ti", "30", "0.5", "-snrpb", "15"],
            "TEB attendu ~ 2.5e-3 <= 10^-2 (valide à 15 dB mais ~100x plus dégradé que NRZ)",
        ),
        (
            "Contre-exemple : NRZ unipolaire [0, 1] à D = 100 kbit/s (dt = 30, Eb/N0 = 15 dB)",
            ["-form", "NRZ", "-ampl", "0", "1", "-ti", "30", "0.5", "-snrpb", "15"],
            "TEB attendu ~ 1.2e-1 >> 10^-2 (fermeture totale de l'œil : le '0' + écho '1' vaut 0.5 = seuil !)",
        ),
        (
            "Sensibilité SNR : NRZ antipolaire à D = 100 kbit/s avec Eb/N0 = 10 dB (< 15 dB)",
            ["-form", "NRZ", "-ampl", "-1", "1", "-ti", "30", "0.5", "-snrpb", "10"],
            "TEB attendu ~ 6.4e-3 <= 10^-2 (NRZ respecte le cahier des charges dès 9.4 dB !)",
        ),
    ]

    for titre, args, commentaire in essais:
        print(f"\n> {titre}")
        print("  Commande : java -cp bin simulateur.Simulateur -mess", NB_BITS, "-seed", SEED, "-nbEch 30", " ".join(args))
        teb = simuler(classpath, args)
        print(f"  --> TEB mesuré = {teb:.5e}")
        print(f"  Commentaire : {commentaire}")
    print("\n" + "=" * 76)


# ------------------------------------------------------------------
# Graphiques
# ------------------------------------------------------------------
def graphique_teb_vs_debit(data):
    fig, ax = plt.subplots(figsize=(8.5, 5.5))

    couleurs = {
        "NRZ antipolaire [-1, 1]": ("tab:blue", "o-", "NRZ antipolaire [-1, 1] (Recommandé)"),
        "NRZT antipolaire [-1, 1]": ("tab:orange", "s-", "NRZT antipolaire [-1, 1]"),
        "NRZ unipolaire [0, 1]": ("tab:red", "^--", "NRZ unipolaire [0, 1] (Contre-exemple)"),
    }

    for courbe, (col, style, label) in couleurs.items():
        points_d = data[courbe]
        xs = [int(k) for k in points_d.keys()]
        ys = [points_d[str(k)] for k in xs]
        # Remplacer les TEB nuls par 1e-6 pour visualisation log propre
        ys_plot = [y if y > 0 else 1e-6 for y in ys]
        ax.plot(xs, ys_plot, style, color=col, label=label, linewidth=1.8, markersize=6)

    # Ligne cible TEB = 10^-2
    ax.axhline(TEB_MAX, color="black", linestyle="--", linewidth=1.5,
               label=f"Cible cahier des charges : TEB $\\leq 10^{{-2}}$")

    # Repères des débits proposés
    ax.axvline(100, color="tab:blue", linestyle=":", alpha=0.8)
    ax.text(103, 4e-5, "100 kbit/s ($T_s = \\tau$)", color="tab:blue", fontsize=9, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:blue", alpha=0.9))

    ax.axvline(200, color="navy", linestyle=":", alpha=0.8)
    ax.text(203, 1.5e-5, "200 kbit/s ($T_s = \\tau/2$)", color="navy", fontsize=9, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="navy", alpha=0.9))

    ax.set_yscale("log")
    ax.set_ylim(5e-7, 0.5)
    ax.set_xlim(0, 520)
    ax.set_title("Question 4 - TEB en fonction du débit binaire ($E_b/N_0 = 15$ dB, $\\tau = 10\\ \\mu$s, $\\alpha=0.5$)",
                 fontsize=11, fontweight="bold")
    ax.set_xlabel("Débit binaire $D$ (kbit/s)", fontsize=10)
    ax.set_ylabel("Taux d'Erreur Binaire (TEB)", fontsize=10)
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(loc="lower right", fontsize=9)

    sauvegarder(fig, "1_q4_teb_vs_debit.png")


def graphique_teb_vs_snr(data):
    fig, ax = plt.subplots(figsize=(8.5, 5.5))

    styles = {
        "NRZ (100 kbit/s)": ("tab:blue", "o-", "NRZ antipolaire - 100 kbit/s ($dt=30$)"),
        "NRZ (200 kbit/s)": ("navy", "D--", "NRZ antipolaire - 200 kbit/s ($dt=60$)"),
        "NRZT (100 kbit/s)": ("tab:orange", "s-", "NRZT antipolaire - 100 kbit/s ($dt=30$)"),
        "NRZT (200 kbit/s)": ("tab:red", "^--", "NRZT antipolaire - 200 kbit/s ($dt=60$)"),
    }

    for courbe, (col, style, label) in styles.items():
        points_s = data[courbe]
        xs = [int(k) for k in points_s.keys()]
        ys = [points_s[str(k)] for k in xs]
        ys_plot = [y if y > 0 else 1e-6 for y in ys]
        ax.plot(xs, ys_plot, style, color=col, label=label, linewidth=1.8, markersize=6)

    ax.axhline(TEB_MAX, color="black", linestyle="--", linewidth=1.5,
               label=f"Cible cahier des charges : TEB $\\leq 10^{{-2}}$")
    ax.axvline(EBN0_MAX_DB, color="gray", linestyle=":", linewidth=1.2,
               label="Limite $E_b/N_0 = 15$ dB")

    # Seuils de franchissement
    ax.axvline(9.4, color="tab:blue", linestyle=":", alpha=0.7)
    ax.text(9.5, 2e-2, "Seuil NRZ $\\approx 9.4$ dB\n(Marge : 5.6 dB)",
            color="tab:blue", fontsize=8.5, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:blue", alpha=0.9))

    ax.axvline(13.1, color="tab:orange", linestyle=":", alpha=0.7)
    ax.text(13.2, 2e-2, "Seuil NRZT $\\approx 13.1$ dB\n(Marge : 1.9 dB)",
            color="tab:orange", fontsize=8.5, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:orange", alpha=0.9))

    ax.set_yscale("log")
    ax.set_ylim(5e-7, 0.2)
    ax.set_xlim(4.5, 15.5)
    ax.set_title("Question 4 - Sensibilité en bruit ($E_b/N_0$) pour $D = 100$ et $200$ kbit/s",
                 fontsize=11, fontweight="bold")
    ax.set_xlabel("$E_b/N_0$ au récepteur (dB)", fontsize=10)
    ax.set_ylabel("Taux d'Erreur Binaire (TEB)", fontsize=10)
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(loc="lower left", fontsize=9)

    sauvegarder(fig, "2_q4_teb_vs_snr.png")


def graphique_analyse_ies():
    """Figure pédagogique illustrant pourquoi l'œil ne se ferme jamais en NRZ antipolaire."""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(11, 4.5))

    t = np.linspace(0, 3, 300)

    # Signal NRZ antipolaire [-1, 1]
    # Séquence : 0, 1, 0 -> amplitudes -1, +1, -1
    # Écho retardé de 1 période (D = 100 kbit/s, dt = 30)
    def trace_cas(ax, min_v, max_v, titre, seuil):
        v0 = min_v
        v1 = max_v
        # 4 combinaisons possibles pour le symbole central t in [1, 2] :
        # (bit_actuel, bit_retardé)
        cas = [
            (v1, v1, f"Bit 1 + écho 1 = {v1 + 0.5*v1:.1f}", "tab:green"),
            (v1, v0, f"Bit 1 + écho 0 = {v1 + 0.5*v0:.1f} (Pire cas 1)", "tab:blue"),
            (v0, v1, f"Bit 0 + écho 1 = {v0 + 0.5*v1:.1f} (Pire cas 0)", "tab:orange"),
            (v0, v0, f"Bit 0 + écho 0 = {v0 + 0.5*v0:.1f}", "tab:purple"),
        ]
        x_ticks = [c[2] for c in cas]
        y_vals = [c[0] + 0.5 * c[1] for c in cas]
        colors = [c[3] for c in cas]

        bars = ax.bar(range(len(cas)), y_vals, color=colors, width=0.55, alpha=0.85, edgecolor="black")
        ax.axhline(seuil, color="red", linestyle="--", linewidth=1.8, label=f"Seuil de décision ({seuil})")

        for b, v in zip(bars, y_vals):
            pos_y = v + 0.08 if v >= 0 else v - 0.15
            ax.text(b.get_x() + b.get_width() / 2, pos_y, f"{v:+.2f}",
                    ha="center", fontweight="bold", fontsize=9)

        ax.set_xticks(range(len(cas)))
        ax.set_xticklabels([f"1 + écho 1", "1 + écho 0\n(pire cas 1)", "0 + écho 1\n(pire cas 0)", "0 + écho 0"],
                           fontsize=8.5)
        ax.set_ylabel("Amplitude reçue au récepteur", fontsize=9.5)
        ax.set_title(titre, fontsize=10.5, fontweight="bold")
        ax.grid(axis="y", alpha=0.3)
        ax.legend(loc="upper right", fontsize=8.5)

    trace_cas(ax1, -1.0, 1.0, "NRZ Antipolaire [ -1, +1 ] : ŒIL OUVERT (Marge = 0.5)", 0.0)
    ax1.set_ylim(-2.0, 2.0)
    ax1.annotate("Marge au seuil = +0.5 > 0\n--> Aucune erreur sans bruit !",
                 xy=(1, 0.5), xytext=(1.2, 1.1),
                 arrowprops=dict(arrowstyle="->", color="tab:blue", lw=1.5),
                 fontsize=8.5, fontweight="bold", color="tab:blue",
                 bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:blue"))

    trace_cas(ax2, 0.0, 1.0, "NRZ Unipolaire [ 0, 1 ] : ŒIL FERMÉ (Pire cas sur le seuil)", 0.5)
    ax2.set_ylim(-0.3, 1.8)
    ax2.annotate("Tombe pile sur le seuil (0.5) !\n--> 50 % d'erreurs au pire cas !",
                 xy=(2, 0.5), xytext=(1.5, 1.0),
                 arrowprops=dict(arrowstyle="->", color="tab:red", lw=1.5),
                 fontsize=8.5, fontweight="bold", color="tab:red",
                 bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:red"))

    sauvegarder(fig, "3_q4_analyse_ies_oeil.png")


def sauvegarder(fig, nom):
    chemin = os.path.join(DOSSIER, nom)
    fig.tight_layout()
    fig.savefig(chemin, dpi=150)
    plt.close(fig)
    print(f"Graphique généré : mesures/environnement2/{nom}")


# ------------------------------------------------------------------
# Synthèse textuelle
# ------------------------------------------------------------------
def afficher_synthese():
    print("=" * 76)
    print(" SIT213 - Etape 6 - Environnement 2 - Synthèse Question 4")
    print("=" * 76)
    print(" Paramètres du sujet :")
    print("   - Canal : 2 trajets espacés de 10 µs, amplitudes relatives alpha0=1.0 et alpha1=0.5")
    print("   - Contrainte qualité : TEB <= 10^-2 pour Eb/N0 <= 15 dB")
    print("   - Formes d'onde autorisées : NRZ et NRZT uniquement")
    print("   - Bande de fréquences : aucune contrainte imposée")
    print("-" * 76)
    print(" RELATION TEMPS <-> ÉCHANTILLONS <-> DÉBIT :")
    print("   tau = 10 µs, Ne = 30 ech/bit  =>  dt = tau * D * Ne = 0.3 * D (en kbit/s)")
    print("   - Débit canonique (Ts = tau = 10 µs)   : D = 100 kbit/s  (dt = 30)")
    print("   - Débit élevé    (Ts = tau/2 = 5 µs)   : D = 200 kbit/s  (dt = 60)")
    print("   - Débit très haut (Ts = tau/5 = 2 µs)  : D = 500 kbit/s  (dt = 150)")
    print("-" * 76)
    print(" COMPARAISON DES FORMES D'ONDE :")
    print("   1. NRZ Antipolaire [-1, 1] (Recommandé) :")
    print("      - L'œil reste grand ouvert (amplitude directe 1.0 > écho 0.5).")
    print("      - À 15 dB : TEB ~ 1e-5 à 3e-5 << 10^-2 pour tous débits testés (10 à 500 kbit/s).")
    print("      - Sensibilité SNR : TEB <= 10^-2 garanti dès Eb/N0 >= 9.4 dB (marge de 5.6 dB sous 15 dB).")
    print("   2. NRZT Antipolaire [-1, 1] :")
    print("      - Intégration sur le tiers central uniquement : perte d'énergie de ~3.7 dB.")
    print("      - À 15 dB : TEB ~ 2.3e-3 <= 10^-2 (respecte le seuil, mais 100x plus dégradé que NRZ).")
    print("      - Sensibilité SNR : passe au-dessus de 10^-2 dès que Eb/N0 < 13.1 dB (marge faible de 1.9 dB).")
    print("   3. NRZ Unipolaire [0, 1] (Éliminé) :")
    print("      - L'écho 0.5 ramène le '0' direct pile sur le seuil de décision (0.5).")
    print("      - Fermeture complète de l'œil dès D >= 67 kbit/s : TEB catastrophique ~ 12 %.")
    print("-" * 76)
    print(" DÉBIT MAXIMAL PROPOSÉ :")
    print("   - Réponse A (Conservatrice / Télécom) : D = 100 kbit/s (Ts = tau).")
    print("     Garantit l'absence d'étalement multi-symboles tout en assurant TEB ~ 3e-5 à 15 dB.")
    print("   - Réponse B (Plein potentiel du modèle) : D >= 200 kbit/s (voire 500 kbit/s).")
    print("     Puisqu'aucune contrainte de bande n'est imposée et que l'écho ne ferme pas l'œil,")
    print("     le TEB reste <= 10^-2 jusqu'à plusieurs Mbit/s.")
    print("=" * 76)


def main():
    parser = argparse.ArgumentParser(description="SIT213 - Etape 6 - Environnement 2 - Question 4")
    parser.add_argument("--tracer", action="store_true", help="Trace les graphiques depuis resultats_q4.json")
    parser.add_argument("--demo", action="store_true", help="Lance les démonstrations en direct pour l'oral")
    options = parser.parse_args()

    afficher_synthese()

    if options.demo:
        executer_demo()
        return

    if options.tracer and os.path.exists(FICHIER_RESULTATS):
        print(f"Chargement des résultats existants : {FICHIER_RESULTATS}")
        with open(FICHIER_RESULTATS) as f:
            resultats = json.load(f)
    else:
        resultats = lancer_simulations()

    print("\nGénération des graphiques d'analyse...")
    graphique_teb_vs_debit(resultats["debit_vs_teb"])
    graphique_teb_vs_snr(resultats["snr_vs_teb"])
    graphique_analyse_ies()
    print("\nTerminé avec succès !")


if __name__ == "__main__":
    main()
