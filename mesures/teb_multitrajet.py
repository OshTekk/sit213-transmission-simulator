#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Carte du TEB en fonction d'un trajet indirect pour les codes implémentés
dans le projet SIT213 : NRZ, RZ et NRZT.

Axe X : retard du trajet indirect (nombre d'échantillons)
Axe Y : amplitude relative alpha du trajet indirect (0 à 1)
Couleur : TEB

Le script adapte automatiquement les amplitudes au code choisi :
    NRZ  : -1.0 / +1.0
    RZ   :  0.0 / +1.0
    NRZT : -1.0 / +1.0

NB_ECH est choisi divisible par 3 afin d'être compatible proprement
avec RZ et NRZT, qui découpent chaque bit en trois zones.
"""

import csv
import os
import re
import sys
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed

import numpy as np
import matplotlib.pyplot as plt


# ==============================================================================
# CONFIGURATION
# ==============================================================================

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
BIN_DIR = os.path.join(PROJECT_ROOT, "bin")
SIMULATEUR_CLASS = os.path.join(BIN_DIR, "simulateur", "Simulateur.class")

# Choix :
#   "NRZ", "RZ", "NRZT"
# ou "TOUS" pour générer les trois cartes successivement.
FORME = "TOUS"

# Amplitudes compatibles avec les contraintes Java de chaque code.
CONFIG_FORMES = {
    "NRZ": {
        "ampl_min": -1.0,
        "ampl_max": 1.0,
    },
    "RZ": {
        "ampl_min": 0.0,
        "ampl_max": 1.0,
    },
    "NRZT": {
        "ampl_min": -1.0,
        "ampl_max": 1.0,
    },
}

# 90 > valeur par défaut 30 et divisible par 3.
# Une période de bit correspond donc ici à 90 échantillons.
NB_ECH = 50

# Nombre de bits utilisés pour estimer le TEB.
NB_BITS = 1_000

# Le SNR reste identique pour toute une carte.
SNR_DB = 10.0

# Même message et même réalisation de bruit pour rendre les points comparables.
SEED = 12345

# --------------------------------------------------------------------------
# Résolution de la carte
# --------------------------------------------------------------------------
# Valeurs raisonnables pour éviter plusieurs dizaines de milliers de JVM.
# Pour une carte très fine :
#   PAS_RETARD = 1
#   PAS_ALPHA = 0.01
#
# Réglage actuel :
#   retards : 0, 3, 6, ..., 90  -> 31 valeurs
#   alpha   : 0, 0.05, ..., 1   -> 21 valeurs
# donc 651 simulations par forme d'onde.
PAS_RETARD = 5
PAS_ALPHA = 0.05

RETARDS = np.arange(0, NB_ECH + 1, PAS_RETARD)
ALPHAS = np.round(
    np.arange(0.0, 1.0 + PAS_ALPHA / 2.0, PAS_ALPHA),
    10
)

MAX_WORKERS = 6


# ==============================================================================
# OUTILS
# ==============================================================================

def verifier_configuration():
    if not os.path.exists(SIMULATEUR_CLASS):
        print(f"[ERREUR] {SIMULATEUR_CLASS} introuvable.")
        print("Compile d'abord le projet depuis sa racine avec : ./script/compile")
        sys.exit(1)

    if FORME != "TOUS" and FORME not in CONFIG_FORMES:
        print(
            f"[ERREUR] FORME='{FORME}' invalide. "
            "Valeurs possibles : NRZ, RZ, NRZT ou TOUS."
        )
        sys.exit(1)

    # RZ et NRZT travaillent par tiers. Ce n'est pas strictement obligatoire
    # dans tout le code, mais c'est le choix le plus propre pour la simulation.
    formes = list(CONFIG_FORMES) if FORME == "TOUS" else [FORME]



def construire_commande(forme, retard, alpha):
    config = CONFIG_FORMES[forme]

    return [
        "java", "-cp", BIN_DIR, "simulateur.Simulateur",
        "-mess", str(NB_BITS),
        "-nbEch", str(NB_ECH),
        "-form", forme,
        "-ampl", str(config["ampl_min"]), str(config["ampl_max"]),
        "-snrpb", str(SNR_DB),
        "-seed", str(SEED),
        "-ti", str(int(retard)), f"{float(alpha):.4f}",
    ]


def extraire_teb(stdout):
    match = re.search(r"TEB\s*:\s*([0-9.eE+-]+)", stdout)
    if match is None:
        return None
    return float(match.group(1))


def simuler_point(forme, indice_alpha, indice_retard, alpha, retard):
    commande = construire_commande(forme, retard, alpha)

    try:
        resultat = subprocess.run(
            commande,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True,
        )

        teb = extraire_teb(resultat.stdout)

        if teb is None:
            print(
                f"[WARN] TEB introuvable : "
                f"{forme}, retard={retard}, alpha={alpha:.2f}"
            )

        return indice_alpha, indice_retard, teb

    except subprocess.CalledProcessError as erreur:
        print(
            f"[ERREUR] Simulation impossible : "
            f"{forme}, retard={retard}, alpha={alpha:.2f}"
        )

        if erreur.stdout:
            print("--- stdout ---")
            print(erreur.stdout)

        if erreur.stderr:
            print("--- stderr ---")
            print(erreur.stderr)

        return indice_alpha, indice_retard, None


# ==============================================================================
# CALCUL
# ==============================================================================

def calculer_carte(forme):
    """
    Retourne une matrice telle que :
        lignes    = alpha
        colonnes  = retard
        valeur    = TEB
    """
    tebs = np.full(
        (len(ALPHAS), len(RETARDS)),
        np.nan,
        dtype=float,
    )

    taches = [
        (i_alpha, i_retard, alpha, retard)
        for i_alpha, alpha in enumerate(ALPHAS)
        for i_retard, retard in enumerate(RETARDS)
    ]

    total = len(taches)

    config = CONFIG_FORMES[forme]

    print()
    print("=" * 72)
    print(f"[INFO] Forme d'onde : {forme}")
    print(
        f"[INFO] Amplitudes du code : "
        f"{config['ampl_min']} / {config['ampl_max']}"
    )
    print(f"[INFO] Nombre de simulations : {total}")
    print(
        f"[INFO] Retard : 0 à {NB_ECH} échantillons "
        f"(une période de bit), pas={PAS_RETARD}"
    )
    print(
        f"[INFO] Amplitude relative : 0.00 à 1.00, "
        f"pas={PAS_ALPHA}"
    )
    print(f"[INFO] SNR : {SNR_DB} dB")
    print("=" * 72)

    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {
            executor.submit(
                simuler_point,
                forme,
                i_alpha,
                i_retard,
                alpha,
                retard,
            ): (i_alpha, i_retard)
            for i_alpha, i_retard, alpha, retard in taches
        }

        terminees = 0

        for future in as_completed(futures):
            i_alpha, i_retard, teb = future.result()

            if teb is not None:
                tebs[i_alpha, i_retard] = teb

            terminees += 1

            if terminees % 100 == 0 or terminees == total:
                print(f"[INFO] {terminees}/{total} simulations terminées")

    return tebs


# ==============================================================================
# SAUVEGARDE
# ==============================================================================

def chemins_sortie(forme):
    base = f"teb_multitrajet_{forme.lower()}"
    csv_path = os.path.join(PROJECT_ROOT, base + ".csv")
    png_path = os.path.join(PROJECT_ROOT, base + ".png")
    return csv_path, png_path


def sauvegarder_csv(forme, tebs):
    csv_path, _ = chemins_sortie(forme)

    with open(csv_path, "w", newline="", encoding="utf-8") as fichier:
        writer = csv.writer(fichier)

        writer.writerow([
            "forme",
            "nb_ech_par_bit",
            "snr_db",
            "retard_echantillons",
            "retard_fraction_periode",
            "amplitude_relative",
            "teb",
        ])

        for i_alpha, alpha in enumerate(ALPHAS):
            for i_retard, retard in enumerate(RETARDS):
                writer.writerow([
                    forme,
                    NB_ECH,
                    SNR_DB,
                    int(retard),
                    retard / NB_ECH,
                    f"{alpha:.4f}",
                    tebs[i_alpha, i_retard],
                    ])

    print(f"[OK] CSV sauvegardé : {csv_path}")


# ==============================================================================
# GRAPHIQUE
# ==============================================================================

def tracer_carte(forme, tebs):
    _, png_path = chemins_sortie(forme)

    fig, ax = plt.subplots(figsize=(11, 7))

    matrice = np.ma.masked_invalid(tebs)

    image = ax.imshow(
        matrice,
        origin="lower",
        aspect="auto",
        extent=[
            float(RETARDS[0]),
            float(RETARDS[-1]),
            float(ALPHAS[0]),
            float(ALPHAS[-1]),
        ],
        interpolation="nearest",
    )

    barre = fig.colorbar(image, ax=ax)
    barre.set_label("TEB")

    ax.set_xlabel(
        "Décalage temporel du trajet indirect τ (échantillons)"
    )
    ax.set_ylabel(
        "Amplitude relative du trajet indirect α"
    )

    ax.set_title(
        f"TEB en fonction du retard et de l'amplitude du trajet indirect\n"
        f"{forme} — {NB_ECH} échantillons/bit — "
        f"retard max = 1 période — SNR = {SNR_DB} dB"
    )

    # Repères en fractions de période.
    ticks = [
        0,
        NB_ECH / 4,
        NB_ECH / 2,
        3 * NB_ECH / 4,
        NB_ECH,
        ]
    labels = [
        "0",
        f"{NB_ECH / 4:g}\n(T/4)",
        f"{NB_ECH / 2:g}\n(T/2)",
        f"{3 * NB_ECH / 4:g}\n(3T/4)",
        f"{NB_ECH}\n(T)",
    ]

    ax.set_xticks(ticks)
    ax.set_xticklabels(labels)
    ax.set_yticks(np.arange(0.0, 1.01, 0.1))

    plt.tight_layout()
    plt.savefig(png_path, dpi=300)

    print(f"[OK] Graphique sauvegardé : {png_path}")

    # En environnement IntelliJ non interactif, plt.show() peut produire
    # un avertissement. On ferme donc la figure après sauvegarde.
    plt.close(fig)


# ==============================================================================
# MAIN
# ==============================================================================

def main():
    verifier_configuration()

    formes = (
        list(CONFIG_FORMES.keys())
        if FORME == "TOUS"
        else [FORME]
    )

    for forme in formes:
        tebs = calculer_carte(forme)
        sauvegarder_csv(forme, tebs)
        tracer_carte(forme, tebs)

    print()
    print("[OK] Toutes les simulations demandées sont terminées.")


if __name__ == "__main__":
    main()