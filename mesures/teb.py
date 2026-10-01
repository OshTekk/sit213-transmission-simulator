#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Script d'évaluation du TEB (Taux d'Erreur Binaire) en fonction du SNR par bit (Eb/N0).
Interfaçage Python <-> Simulateur Java SIT213 + Tracé Matplotlib.

Usage : python3 mesures/teb.py          # simule, sauvegarde et trace
        python3 mesures/teb.py --tracer # trace depuis mesures/teb_resultats.json
"""

import subprocess
import re
import math
import os
import json
import argparse
from concurrent.futures import ThreadPoolExecutor
import numpy as np
import matplotlib.pyplot as plt

# ==============================================================================
# CONFIGURATION
# ==============================================================================

# Racine du projet (dossier contenant bin/ ou src/)
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
BIN_DIR = os.path.join(PROJECT_ROOT, "bin")
DATA_FILE = os.path.join(os.path.dirname(__file__), "teb_resultats.json")

# Paramètres de simulation
NB_BITS = 100_000         # Nombre de bits par simulation (ex: 100 000 pour descendre à 1e-4)
NB_ECH = 30               # Échantillons par bit
SNR_RANGE = np.arange(5, 15, 0.25)  # Plage de SNR en dB : de -4 à 10 dB par pas de 1 dB

# Formes d'onde à tester avec leurs arguments
FORMES = {
    "NRZ":  ["-form", "NRZ",  "-ampl", "-1.0", "1.0"],
    "RZ":   ["-form", "RZ",   "-ampl", "0.0", "1.0"],
    "NRZT": ["-form", "NRZT", "-ampl", "-1.0", "1.0"]
}

# ==============================================================================
# FONCTIONS DE SIMULATION ET PARSING
# ==============================================================================

def simuler_teb(forme, snr, nb_bits=NB_BITS, nb_ech=NB_ECH, seed=None):
    """
    Lance une instance de Simulateur Java et extrait le TEB calculé.
    """
    cmd = [
        "java", "-cp", BIN_DIR, "simulateur.Simulateur",
        "-mess", str(nb_bits),
        "-nbEch", str(nb_ech),
        "-snrpb", str(float(snr))
    ] + FORMES[forme]

    if seed is not None:
        cmd.extend(["-seed", str(seed)])

    try:
        res = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        # Extraction du TEB via expression régulière : "=>   TEB : 0.0123"
        match = re.search(r"TEB\s*:\s*([0-9.eE+-]+)", res.stdout)
        if match:
            return float(match.group(1))
        else:
            print(f"[WARN] Sortie non reconnue pour {forme} SNR={snr} dB:\n{res.stdout}")
            return None
    except subprocess.CalledProcessError as e:
        print(f"[ERREUR] Échec simulation {forme} SNR={snr} dB : {e.stderr}")
        return None


def simuler_courbe(forme, snr_list, max_workers=4):
    """
    Exécute les simulations d'une forme d'onde en parallèle sur plusieurs threads.
    """
    print(f"-> Simulation en cours pour {forme} ({len(snr_list)} points)...")
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Exécute tous les points de SNR en parallèle
        tebs = list(executor.map(lambda snr: simuler_teb(forme, snr), snr_list))
    return tebs


# ==============================================================================
# COURBES THÉORIQUES
# ==============================================================================

def teb_theorique_nrz(snr_db):
    """
    TEB théorique NRZ antipodal (±A) avec récepteur à filtre adapté :
    Pb = 0.5 * erfc( sqrt(10^(SNR/10)) ) = Q( sqrt(2 * 10^(SNR/10)) )
    """
    snr_lin = 10.0 ** (snr_db / 10.0)
    return 0.5 * math.erfc(math.sqrt(snr_lin))


# ==============================================================================
# MAIN / TRACÉ MATPLOTLIB
# ==============================================================================

def simuler_donnees():
    """Calcule les TEB et conserve les paramètres nécessaires au tracé."""
    # Vérification que le dossier bin existe et est compilé
    if not os.path.exists(os.path.join(BIN_DIR, "simulateur", "Simulateur.class")):
        raise RuntimeError(f"Le projet n'est pas compilé dans {BIN_DIR}.")

    donnees = {
        "nb_bits": NB_BITS,
        "nb_ech": NB_ECH,
        "snr_db": SNR_RANGE.tolist(),
        "teb": {}
    }
    for forme in FORMES:
        tebs = simuler_courbe(forme, SNR_RANGE)
        if any(teb is None for teb in tebs):
            raise RuntimeError(f"Simulation incomplète pour {forme} : aucun fichier de données enregistré.")
        donnees["teb"][forme] = tebs
    return donnees


def tracer_courbes(donnees, output_png):
    """Trace les courbes à partir des points simulés ou rechargés."""
    snr_list = donnees["snr_db"]
    if not snr_list or any(len(tebs) != len(snr_list) for tebs in donnees["teb"].values()):
        raise ValueError("Le fichier de données ne contient pas des courbes complètes.")

    plt.figure(figsize=(10, 6))

    # 1. Courbe théorique NRZ
    snr_fine = np.linspace(snr_list[0], snr_list[-1], 200)
    teb_th = [teb_theorique_nrz(s) for s in snr_fine]
    plt.semilogy(snr_fine, teb_th, 'k--', label="Théorique NRZ (erfc)", linewidth=1.8)

    # 2. Simulation des différentes formes d'ondes
    styles = {
        "NRZ":  {"color": "tab:blue",   "marker": "o"},
        "RZ":   {"color": "tab:orange", "marker": "s"},
        "NRZT": {"color": "tab:green",  "marker": "^"}
    }

    for forme, tebs in donnees["teb"].items():

        # Filtrage des points à TEB = 0 (non affichables en échelle log)
        snr_valides = []
        teb_valides = []
        for snr, teb in zip(snr_list, tebs):
            if teb is not None and teb > 0:
                snr_valides.append(snr)
                teb_valides.append(teb)

        plt.semilogy(
            snr_valides, teb_valides,
            label=f"Simulé {forme}",
            color=styles[forme]["color"],
            marker=styles[forme]["marker"],
            linestyle="-",
            markersize=6,
            alpha=0.9
        )

    # Configuration du graphique
    plt.title("Taux d'Erreur Binaire (TEB) vs Rapport Signal sur Bruit par bit ($E_b/N_0$)", fontsize=13)
    plt.xlabel("$E_b/N_0$ (dB)", fontsize=12)
    plt.ylabel("TEB", fontsize=12)
    plt.ylim(bottom=1e-6, top=1.0)
    plt.xlim(left=snr_list[0], right=snr_list[-1])
    plt.grid(True, which="both", linestyle="--", alpha=0.5)
    plt.legend(fontsize=11)
    plt.tight_layout()

    # Sauvegarde et affichage
    plt.savefig(output_png, dpi=300)
    print(f"\n[OK] Graphique sauvegardé dans : {output_png}")
    plt.show()


def main():
    parser = argparse.ArgumentParser(description="Simule le TEB et trace les courbes.")
    parser.add_argument("--tracer", action="store_true", help="Trace les données sauvegardées sans lancer Java.")
    parser.add_argument("--donnees", default=DATA_FILE, help="Fichier JSON des résultats de simulation.")
    parser.add_argument("--image", default=os.path.join(PROJECT_ROOT, "courbes_teb.png"), help="Image PNG à créer.")
    args = parser.parse_args()

    try:
        if args.tracer:
            with open(args.donnees, encoding="utf-8") as fichier:
                donnees = json.load(fichier)
        else:
            donnees = simuler_donnees()
            fichier_temporaire = args.donnees + ".tmp"
            with open(fichier_temporaire, "w", encoding="utf-8") as fichier:
                json.dump(donnees, fichier, ensure_ascii=False, indent=2)
            os.replace(fichier_temporaire, args.donnees)
            print(f"[OK] Données sauvegardées dans : {args.donnees}")
        tracer_courbes(donnees, args.image)
    except (OSError, ValueError, KeyError, RuntimeError) as erreur:
        parser.exit(1, f"[ERREUR] {erreur}\n")


if __name__ == "__main__":
    main()
