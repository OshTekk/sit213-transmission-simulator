#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Cinq cartes TEB et cinq exemples temporels pour 1 à 5 trajets indirects.

Chaque essai choisit le retard initial d, l'amplitude de référence alpha et le
nombre N de trajets. Les retards sont d_i = d + (i - 1) * INTERVALLE_ECH et
les amplitudes sont alpha_i = alpha * exp(-(d_i - d) / DECROISSANCE_ECH).
Le premier trajet conserve ainsi l'amplitude de référence, comme dans
teb_multitrajet.py ; seuls les trajets suivants s'atténuent progressivement.

Le graphique place les cinq cartes sur une grille de trois lignes et deux
colonnes. La dernière case montre les retards/amplitudes envoyés à Java pour
une amplitude de référence de 0,5 et un retard initial nul.
"""

import argparse
import csv
import math
import os
import re
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed

import matplotlib.pyplot as plt
from matplotlib.colors import Normalize
import numpy as np


PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
BIN_DIR = os.path.join(PROJECT_ROOT, "bin")
SIMULATEUR_CLASS = os.path.join(BIN_DIR, "simulateur", "Simulateur.class")

# Même conventions de modulation que teb_multitrajet.py.
FORME = "TOUS"  # NRZ, RZ, NRZT ou TOUS
CONFIG_FORMES = {
    "NRZ": (-1.0, 1.0),
    "RZ": (0.0, 1.0),
    "NRZT": (-1.0, 1.0),
}
NB_ECH_BIT = 100
NB_BITS = 1_000
SNR_DB = 10.0
SEED = 12345

# d varie sur l'axe X ; l'intervalle entre trajets d'un même essai est fixe.
MAX_RETARD_INITIAL = 2 * NB_ECH_BIT
PAS_RETARD = 3
INTERVALLE_ECH = NB_ECH_BIT // 3

# L'amplitude du premier trajet est alpha, indépendamment de son retard.
# Chaque période de bit supplémentaire entre deux trajets divise l'amplitude
# par e : alpha_i = alpha * exp(-(d_i - d_1) / tau).
DECROISSANCE_ECH = NB_ECH_BIT
PAS_ALPHA = 0.01
MAX_TRAJETS = 5  # Limite de l'option Java -ti.
MAX_WORKERS = 12

RETARDS = np.arange(0, MAX_RETARD_INITIAL + 1, PAS_RETARD)
ALPHAS = np.round(np.arange(0, 1 + PAS_ALPHA / 2, PAS_ALPHA), 10)
NOMBRES_TRAJETS = range(1, MAX_TRAJETS + 1)


def chemins_sortie(forme):
    base = os.path.join(PROJECT_ROOT, f"teb_multitrajets_{forme.lower()}")
    return base + ".csv", base + ".png"


def verifier_configuration():
    if not os.path.isfile(SIMULATEUR_CLASS):
        raise ValueError(f"Classe Java introuvable : {SIMULATEUR_CLASS}. Compiler le projet d'abord.")
    if FORME != "TOUS" and FORME not in CONFIG_FORMES:
        raise ValueError(f"Forme d'onde inconnue : {FORME}.")
    if (NB_ECH_BIT <= 0 or NB_BITS <= 0 or MAX_RETARD_INITIAL <= 0
            or PAS_RETARD < 1 or not float(PAS_RETARD).is_integer()
            or INTERVALLE_ECH < 1 or not float(INTERVALLE_ECH).is_integer()
            or DECROISSANCE_ECH <= 0 or not 0 < PAS_ALPHA <= 1
            or not 1 <= MAX_TRAJETS <= 5 or MAX_WORKERS < 1):
        raise ValueError("Paramètres de simulation invalides.")


def trajets(retard_initial, alpha, nombre_trajets, intervalle_ech,
            decroissance_ech, modele="relatif_premier"):
    """Retourne les couples retard/amplitude passés à Java sous -ti."""
    return [
        (
            int(retard_initial + indice * intervalle_ech),
            float(alpha * math.exp(-(
                indice * intervalle_ech if modele == "relatif_premier"
                else retard_initial + indice * intervalle_ech
            ) / decroissance_ech)),
        )
        for indice in range(nombre_trajets)
    ]


def construire_commande(forme, retard_initial, alpha, nombre_trajets):
    ampl_min, ampl_max = CONFIG_FORMES[forme]
    commande = [
        "java", "-cp", BIN_DIR, "simulateur.Simulateur",
        "-mess", str(NB_BITS),
        "-nbEch", str(NB_ECH_BIT),
        "-form", forme,
        "-ampl", str(ampl_min), str(ampl_max),
        "-snrpb", str(SNR_DB),
        "-seed", str(SEED),
        "-ti",
    ]
    for retard, amplitude in trajets(
            retard_initial, alpha, nombre_trajets,
            INTERVALLE_ECH, DECROISSANCE_ECH):
        commande.extend((str(retard), f"{amplitude:.6f}"))
    return commande


def simuler_point(forme, indice_nombre, indice_alpha, indice_retard,
                  nombre_trajets, alpha, retard_initial):
    commande = construire_commande(forme, retard_initial, alpha, nombre_trajets)
    try:
        resultat = subprocess.run(
            commande, capture_output=True, text=True, check=True
        )
        correspondance = re.search(r"TEB\s*:\s*([0-9.eE+-]+)", resultat.stdout)
        if correspondance is None:
            print(f"[WARN] TEB absent : {forme}, N={nombre_trajets}, "
                  f"retard={retard_initial}, alpha={alpha:g}")
            teb = np.nan
        else:
            teb = float(correspondance.group(1))
    except (OSError, subprocess.CalledProcessError) as erreur:
        print(f"[ERREUR] {forme}, N={nombre_trajets}, retard={retard_initial}, "
              f"alpha={alpha:g} : {erreur}")
        if isinstance(erreur, subprocess.CalledProcessError) and erreur.stderr:
            print(erreur.stderr)
        teb = np.nan
    return indice_nombre, indice_alpha, indice_retard, teb


def calculer_volume(forme):
    """Volume TEB indexé par (nombre de trajets, amplitude, retard initial)."""
    volume = np.full((len(NOMBRES_TRAJETS), len(ALPHAS), len(RETARDS)), np.nan)
    taches = [
        (i_nombre, i_alpha, i_retard, nombre, alpha, retard)
        for i_nombre, nombre in enumerate(NOMBRES_TRAJETS)
        for i_alpha, alpha in enumerate(ALPHAS)
        for i_retard, retard in enumerate(RETARDS)
    ]
    print(f"[INFO] {forme} : {len(taches)} simulations, "
          f"{MAX_WORKERS} JVM simultanées")
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = [
            executor.submit(simuler_point, forme, *tache) for tache in taches
        ]
        for terminees, future in enumerate(as_completed(futures), start=1):
            i_nombre, i_alpha, i_retard, teb = future.result()
            volume[i_nombre, i_alpha, i_retard] = teb
            if terminees % 100 == 0 or terminees == len(taches):
                print(f"[INFO] {forme} : {terminees}/{len(taches)} terminées")
    return volume


COLONNES_CSV = (
    "forme", "nb_bits", "nb_ech_par_bit", "snr_db", "seed",
    "intervalle_ech", "decroissance_ech", "modele_amplitude", "retard_initial",
    "amplitude_reference", "nb_trajets", "teb",
)


def sauvegarder_csv(forme, volume):
    csv_path, _ = chemins_sortie(forme)
    with open(csv_path, "w", newline="", encoding="utf-8") as fichier:
        writer = csv.writer(fichier)
        writer.writerow(COLONNES_CSV)
        for i_nombre, nombre in enumerate(NOMBRES_TRAJETS):
            for i_alpha, alpha in enumerate(ALPHAS):
                for i_retard, retard in enumerate(RETARDS):
                    writer.writerow((
                        forme, NB_BITS, NB_ECH_BIT, SNR_DB, SEED,
                        INTERVALLE_ECH, DECROISSANCE_ECH, "relatif_premier", int(retard),
                        f"{alpha:.4f}", nombre,
                        volume[i_nombre, i_alpha, i_retard],
                    ))
    print(f"[OK] CSV sauvegardé : {csv_path}")


def charger_csv(chemin):
    """Reconstruit le volume et les paramètres sans utiliser les constantes courantes."""
    points = {}
    parametres = None
    with open(chemin, newline="", encoding="utf-8") as fichier:
        lecteur = csv.DictReader(fichier)
        anciennes_colonnes = set(COLONNES_CSV) - {"modele_amplitude"}
        if lecteur.fieldnames is None or not anciennes_colonnes.issubset(lecteur.fieldnames):
            raise ValueError(f"Colonnes manquantes dans {chemin}.")
        for numero, ligne in enumerate(lecteur, start=2):
            try:
                forme = ligne["forme"]
                nb_bits = int(ligne["nb_bits"])
                nb_ech_bit = int(ligne["nb_ech_par_bit"])
                snr_db = float(ligne["snr_db"])
                seed = int(ligne["seed"])
                intervalle = int(ligne["intervalle_ech"])
                decroissance = float(ligne["decroissance_ech"])
                modele = ligne.get("modele_amplitude", "retard_absolu")
                retard = int(ligne["retard_initial"])
                alpha = float(ligne["amplitude_reference"])
                nombre = int(ligne["nb_trajets"])
                teb = float(ligne["teb"]) if ligne["teb"] else np.nan
            except (TypeError, ValueError) as erreur:
                raise ValueError(f"Valeur invalide dans {chemin}, ligne {numero}.") from erreur

            if (forme not in CONFIG_FORMES or nb_bits <= 0 or nb_ech_bit <= 0
                    or not np.isfinite(snr_db) or intervalle < 1
                    or decroissance <= 0 or not np.isfinite(decroissance)
                    or modele not in ("relatif_premier", "retard_absolu")
                    or retard < 0 or not 0 <= alpha <= 1
                    or not 1 <= nombre <= 5 or np.isinf(teb)
                    or (np.isfinite(teb) and not 0 <= teb <= 1)):
                raise ValueError(f"Valeur hors limites dans {chemin}, ligne {numero}.")

            meta = (forme, nb_bits, nb_ech_bit, snr_db, seed,
                    intervalle, decroissance, modele)
            if parametres is None:
                parametres = meta
            elif parametres != meta:
                raise ValueError(f"Paramètres incohérents dans {chemin}, ligne {numero}.")

            cle = (nombre, alpha, retard)
            if cle in points:
                raise ValueError(f"Point répété dans {chemin}, ligne {numero}.")
            points[cle] = teb

    if not points:
        raise ValueError(f"Aucune mesure dans {chemin}.")
    nombres = np.array(sorted({nombre for nombre, _, _ in points}), dtype=int)
    alphas = np.array(sorted({alpha for _, alpha, _ in points}))
    retards = np.array(sorted({retard for _, _, retard in points}))
    if len(alphas) < 2 or len(retards) < 2:
        raise ValueError(f"Au moins deux valeurs par axe sont nécessaires dans {chemin}.")

    volume = np.full((len(nombres), len(alphas), len(retards)), np.nan)
    indices_n = {nombre: i for i, nombre in enumerate(nombres)}
    indices_a = {alpha: i for i, alpha in enumerate(alphas)}
    indices_r = {retard: i for i, retard in enumerate(retards)}
    for (nombre, alpha, retard), teb in points.items():
        volume[indices_n[nombre], indices_a[alpha], indices_r[retard]] = teb
    return parametres, nombres, alphas, retards, volume


def tracer_volume(parametres, nombres, alphas, retards, volume, png_path=None):
    forme, nb_bits, nb_ech_bit, snr_db, _, intervalle, decroissance, modele = parametres
    if png_path is None:
        _, png_path = chemins_sortie(forme)

    if list(nombres) != [1, 2, 3, 4, 5]:
        raise ValueError("Le graphique nécessite les cinq couches de 1 à 5 trajets.")
    valides = np.isfinite(volume)
    if not valides.any():
        raise ValueError("Aucun TEB valide à tracer.")
    minimum = float(volume[valides].min())
    maximum = float(volume[valides].max())
    if minimum == maximum:
        maximum = minimum + 1 / nb_bits
    normalisation = Normalize(vmin=minimum, vmax=maximum)
    figure = plt.figure(figsize=(16, 15))
    grille = figure.add_gridspec(3, 2)

    for indice, nombre in enumerate(nombres):
        axe = figure.add_subplot(grille[indice // 2, indice % 2])
        image = axe.imshow(
            np.ma.masked_invalid(volume[indice]),
            origin="lower", aspect="auto", interpolation="nearest",
            extent=[retards[0], retards[-1], alphas[0], alphas[-1]],
            cmap="viridis", norm=normalisation,
        )
        axe.set_title(f"{nombre} trajet{'s' if nombre > 1 else ''} indirect{'s' if nombre > 1 else ''}")
        axe.set_xlabel("Retard du premier trajet (échantillons)")
        axe.set_ylabel("Amplitude de référence α")
        axe.set_yticks(np.arange(0, 1.01, 0.2))

    barre = figure.colorbar(image, cax=figure.add_axes([0.89, 0.22, 0.018, 0.56]))
    barre.set_label("TEB commun aux cinq cartes")
    graduations = np.linspace(minimum, maximum, 6)
    barre.set_ticks(graduations)
    barre.set_ticklabels([f"{valeur:.4g}" for valeur in graduations])

    # Cinq petits diagrammes d'impulsions dans la sixième case.
    sous_grille = grille[2, 1].subgridspec(5, 1, hspace=0.15)
    alpha_exemple = 0.5
    retard_exemple = 0
    for indice, nombre in enumerate(nombres):
        axe = figure.add_subplot(sous_grille[indice])
        couples = trajets(retard_exemple, alpha_exemple, nombre,
                          intervalle, decroissance, modele)
        temps = [retard for retard, _ in couples]
        amplitudes = [amplitude for _, amplitude in couples]
        axe.vlines(temps, 0, amplitudes, color="tab:blue", linewidth=2)
        axe.scatter(temps, amplitudes, color="tab:blue", s=18, zorder=3)
        axe.axhline(0, color="0.5", linewidth=0.7)
        axe.set_xlim(-intervalle / 3, 4 * intervalle + intervalle / 2)
        axe.set_ylim(0, 0.55)
        axe.set_yticks([0, 0.5])
        axe.tick_params(labelsize=8)
        axe.text(0.02, 0.83, f"{nombre} trajet{'s' if nombre > 1 else ''}",
                 transform=axe.transAxes, fontsize=9, va="top")
        if indice == 0:
            axe.set_title("Répartition temporelle (α = 0,5 ; retard initial = 0)",
                          fontsize=11)
        if indice < len(nombres) - 1:
            axe.tick_params(labelbottom=False)
        else:
            axe.set_xlabel("Retard (échantillons)", fontsize=9)

    loi = (f"α exp(−(retard − d₁)/{decroissance:g})"
           if modele == "relatif_premier"
           else f"α exp(−retard/{decroissance:g})")
    figure.suptitle(
        f"TEB de 1 à 5 trajets indirects — {forme}\n"
        f"Δ = {intervalle} éch. ; amplitude = {loi} ; "
        f"{nb_ech_bit} éch./bit ; SNR = {snr_db:g} dB",
        fontsize=14,
    )
    figure.subplots_adjust(
        left=0.07, right=0.85, bottom=0.06, top=0.9,
        wspace=0.18, hspace=0.35,
    )
    figure.savefig(png_path, dpi=250, bbox_inches="tight")
    plt.close(figure)
    print(f"[OK] Graphique sauvegardé : {png_path}")


def main():
    parser = argparse.ArgumentParser(
        description="Simule ou retrace cinq cartes TEB pour 1 à 5 trajets."
    )
    parser.add_argument("--tracer", action="store_true",
                        help="Relit des CSV sans lancer Java.")
    parser.add_argument("--donnees", nargs="+", metavar="CSV",
                        help="Un ou plusieurs CSV à relire avec --tracer.")
    parser.add_argument("--image", help="PNG de sortie si un seul CSV est relu.")
    args = parser.parse_args()

    if args.tracer:
        try:
            chemins = args.donnees
            if chemins is None:
                formes = list(CONFIG_FORMES) if FORME == "TOUS" else [FORME]
                chemins = [chemins_sortie(forme)[0] for forme in formes]
            if args.image and len(chemins) != 1:
                parser.error("--image nécessite un seul CSV.")
            formes_vues = set()
            for chemin in chemins:
                parametres, nombres, alphas, retards, volume = charger_csv(chemin)
                forme = parametres[0]
                if forme in formes_vues:
                    raise ValueError(f"Plusieurs CSV pour {forme}.")
                formes_vues.add(forme)
                tracer_volume(parametres, nombres, alphas, retards, volume,
                              args.image)
        except (OSError, ValueError) as erreur:
            parser.exit(1, f"[ERREUR] {erreur}\n")
        return

    if args.donnees or args.image:
        parser.error("--donnees et --image nécessitent --tracer.")
    try:
        verifier_configuration()
    except ValueError as erreur:
        parser.exit(1, f"[ERREUR] {erreur}\n")

    formes = list(CONFIG_FORMES) if FORME == "TOUS" else [FORME]
    parametres = (FORME, NB_BITS, NB_ECH_BIT, SNR_DB, SEED,
                  INTERVALLE_ECH, DECROISSANCE_ECH, "relatif_premier")
    for forme in formes:
        volume = calculer_volume(forme)
        sauvegarder_csv(forme, volume)
        tracer_volume((forme, *parametres[1:]),
                      np.array(list(NOMBRES_TRAJETS)), ALPHAS, RETARDS, volume)


if __name__ == "__main__":
    main()
