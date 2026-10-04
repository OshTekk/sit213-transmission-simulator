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
import argparse
import os
import re
import sys
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
from fractions import Fraction

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

# Echantillonnage d'un bit
# 90 > valeur par défaut 30 et divisible par 3.
# Une période de bit correspond donc ici à 90 échantillons.
NB_ECH_BIT = 30

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

# Valeur max du décalage en échantillons
MAX_ECH_RETARD = 2 * NB_ECH_BIT
PAS_RETARD = 1
PAS_ALPHA = 0.1

RETARDS = np.arange(0, MAX_ECH_RETARD + 1, PAS_RETARD)
ALPHAS = np.round(
    np.arange(0.0, 1.0 + PAS_ALPHA / 2.0, PAS_ALPHA),
    10
)

MAX_WORKERS = 12


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
        "-nbEch", str(NB_ECH_BIT),
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
        f"[INFO] Retard : 0 à {MAX_ECH_RETARD} échantillons "
        f"({MAX_ECH_RETARD / NB_ECH_BIT:g} T), pas={PAS_RETARD}"
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
                    NB_ECH_BIT,
                    SNR_DB,
                    int(retard),
                    retard / NB_ECH_BIT,
                    f"{alpha:.4f}",
                    tebs[i_alpha, i_retard],
                    ])

    print(f"[OK] CSV sauvegardé : {csv_path}")


def charger_csv(chemin):
    """Reconstruit une carte et ses paramètres à partir d'un CSV de mesures."""
    colonnes = {
        "forme", "nb_ech_par_bit", "snr_db", "retard_echantillons",
        "amplitude_relative", "teb",
    }
    points = {}
    parametres = None

    with open(chemin, newline="", encoding="utf-8") as fichier:
        lecteur = csv.DictReader(fichier)
        if lecteur.fieldnames is None or not colonnes.issubset(lecteur.fieldnames):
            raise ValueError(f"Colonnes manquantes dans {chemin}.")

        for numero, ligne in enumerate(lecteur, start=2):
            try:
                forme = ligne["forme"]
                nb_ech_bit = int(ligne["nb_ech_par_bit"])
                snr_db = float(ligne["snr_db"])
                retard = int(ligne["retard_echantillons"])
                alpha = float(ligne["amplitude_relative"])
                teb = float(ligne["teb"]) if ligne["teb"] else np.nan
            except (TypeError, ValueError) as erreur:
                raise ValueError(f"Valeur invalide dans {chemin}, ligne {numero}.") from erreur

            if (forme not in CONFIG_FORMES or nb_ech_bit <= 0 or retard < 0
                    or not np.isfinite(snr_db) or not 0 <= alpha <= 1
                    or np.isinf(teb) or (np.isfinite(teb) and not 0 <= teb <= 1)):
                raise ValueError(f"Valeur hors limites dans {chemin}, ligne {numero}.")

            if parametres is None:
                parametres = (forme, nb_ech_bit, snr_db)
            elif parametres != (forme, nb_ech_bit, snr_db):
                raise ValueError(f"Paramètres incohérents dans {chemin}, ligne {numero}.")

            cle = (alpha, retard)
            if cle in points:
                raise ValueError(f"Point répété dans {chemin}, ligne {numero}.")
            points[cle] = teb

    if not points:
        raise ValueError(f"Aucune mesure dans {chemin}.")

    alphas = np.array(sorted({alpha for alpha, _ in points}))
    retards = np.array(sorted({retard for _, retard in points}))
    if len(alphas) < 2 or len(retards) < 2:
        raise ValueError(f"La carte de {chemin} doit avoir au moins deux valeurs par axe.")

    tebs = np.full((len(alphas), len(retards)), np.nan)
    indices_alpha = {alpha: indice for indice, alpha in enumerate(alphas)}
    indices_retard = {retard: indice for indice, retard in enumerate(retards)}
    for (alpha, retard), teb in points.items():
        tebs[indices_alpha[alpha], indices_retard[retard]] = teb

    forme, nb_ech_bit, snr_db = parametres
    return forme, tebs, retards, alphas, nb_ech_bit, snr_db


# ==============================================================================
# GRAPHIQUE
# ==============================================================================

def tracer_carte(forme, tebs, retards=RETARDS, alphas=ALPHAS,
                 nb_ech_bit=NB_ECH_BIT, snr_db=SNR_DB, png_path=None):
    if png_path is None:
        _, png_path = chemins_sortie(forme)

    retard_max = retards[-1]
    largeur = max(11, 5.5 + 2.8 * retard_max / nb_ech_bit)
    fig, ax = plt.subplots(figsize=(largeur, 7))

    matrice = np.ma.masked_invalid(tebs)

    image = ax.imshow(
        matrice,
        origin="lower",
        aspect="auto",
        extent=[
            float(retards[0]),
            float(retards[-1]),
            float(alphas[0]),
            float(alphas[-1]),
        ],
        interpolation="nearest",
    )

    barre = fig.colorbar(image, ax=ax)
    barre.set_label("TEB")
    borne_basse, borne_haute = image.get_clim()
    ticks_teb = np.linspace(borne_basse, borne_haute, 6)
    barre.set_ticks(ticks_teb)
    barre.set_ticklabels([f"{valeur:.4g}" for valeur in ticks_teb])

    ax.set_xlabel(
        "Décalage temporel du trajet indirect τ (échantillons)"
    )
    ax.set_ylabel(
        "Amplitude relative du trajet indirect α"
    )

    ax.set_title(
        f"TEB en fonction du retard et de l'amplitude du trajet indirect\n"
        f"{forme} — {nb_ech_bit} échantillons/bit — "
        f"retard max = {retard_max / nb_ech_bit:g} T — SNR = {snr_db} dB"
    )

    # Un repère tous les quarts de période, y compris après T.
    ticks = [0]
    labels = ["0"]
    for quart in range(1, int(4 * retard_max // nb_ech_bit) + 1):
        retard = quart * nb_ech_bit / 4
        fraction = Fraction(quart, 4)
        if fraction.denominator == 1:
            notation = "T" if fraction.numerator == 1 else f"{fraction.numerator}T"
        else:
            coefficient = "" if fraction.numerator == 1 else str(fraction.numerator)
            notation = f"{coefficient}T/{fraction.denominator}"
        ticks.append(retard)
        labels.append(f"{retard:g}\n({notation})")

    if ticks[-1] < retard_max:
        ticks.append(retard_max)
        labels.append(
            f"{retard_max:g}\n({retard_max / nb_ech_bit:g}T)"
        )

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
    parser = argparse.ArgumentParser(
        description="Simule ou retrace des cartes TEB multitrajet depuis des CSV."
    )
    parser.add_argument(
        "--tracer", action="store_true",
        help="Retrace les cartes des CSV sans lancer Java."
    )
    parser.add_argument(
        "--donnees", nargs="+", metavar="CSV",
        help="Un ou plusieurs CSV de mesures à relire avec --tracer."
    )
    parser.add_argument(
        "--image",
        help="PNG de sortie si un seul CSV est relu."
    )
    args = parser.parse_args()

    if args.tracer:
        try:
            chemins = args.donnees
            if chemins is None:
                formes = list(CONFIG_FORMES) if FORME == "TOUS" else [FORME]
                chemins = []
                for forme in formes:
                    csv_path, _ = chemins_sortie(forme)
                    if not os.path.isfile(csv_path):
                        csv_path = os.path.join(
                            os.path.dirname(__file__), f"teb_multitrajet_{forme.lower()}.csv"
                        )
                    chemins.append(csv_path)

            if args.image and len(chemins) != 1:
                parser.error("--image nécessite un seul CSV.")

            formes_vues = set()
            for chemin in chemins:
                forme, tebs, retards, alphas, nb_ech_bit, snr_db = charger_csv(chemin)
                if forme in formes_vues:
                    raise ValueError(f"Plusieurs CSV pour la forme {forme}.")
                formes_vues.add(forme)
                tracer_carte(
                    forme, tebs, retards, alphas, nb_ech_bit, snr_db, args.image
                )
        except (OSError, ValueError) as erreur:
            parser.exit(1, f"[ERREUR] {erreur}\n")
        return

    if args.donnees or args.image:
        parser.error("--donnees et --image nécessitent --tracer.")

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
