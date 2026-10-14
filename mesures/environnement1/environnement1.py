#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SIT213 - Etape 6 - Environnement 1 (canal BBAG, emetteur sur batterie)

Ce script :
  1. calcule les reponses theoriques aux questions 1, 2 et 3 ;
  2. lance notre simulateur Java pour verifier ces reponses (TEB mesures) ;
  3. trace les 3 graphiques utilises pour la presentation.

Utilisation (depuis la racine du projet, apres ./script/compile) :
  python3 mesures/environnement1/environnement1.py           # simule + trace
  python3 mesures/environnement1/environnement1.py --tracer  # trace depuis resultats.json
  python3 mesures/environnement1/environnement1.py --demo    # 4 simulations "en direct"
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

# ------------------------------------------------------------------
# Donnees du sujet
# ------------------------------------------------------------------
N0_DBM_HZ = -80.0         # plancher de bruit du recepteur (dBm/Hz)
ATTENUATION_DB = 40.0     # attenuation de la ligne (dB)
TEB_CIBLE = 1e-3          # TEB maximal demande par le client
BATTERIE_J = 3.0          # energie de la batterie (J)
BITS_PAR_JOUR = 1e6       # bits utiles transmis par jour

# Conversions : dBm -> W  (P_dBm = 30 + 10 log10(P))
N0 = 10 ** ((N0_DBM_HZ - 30) / 10)    # 1e-11 W/Hz = 1e-11 J
ATTENUATION = 10 ** (ATTENUATION_DB / 10)  # 10 000 en puissance

# ------------------------------------------------------------------
# Parametres des simulations
# ------------------------------------------------------------------
NB_BITS = 100_000          # 100 erreurs attendues a TEB = 1e-3 : precision suffisante
SEED = 42                  # meme graine pour toutes les simulations (reproductible)
EBN0_DB = list(range(0, 11))  # Eb/N0 de 0 a 10 dB par pas de 1 dB

# Formes d'onde comparees (arguments passes au simulateur)
FORMES = {
    "NRZ antipolaire [-1, 1]": ["-form", "NRZ", "-ampl", "-1", "1"],
    "NRZ unipolaire [0, 1]":   ["-form", "NRZ", "-ampl", "0", "1"],
    "RZ [0, 1]":               ["-form", "RZ", "-ampl", "0", "1"],
    "NRZT antipolaire [-1, 1]": ["-form", "NRZT", "-ampl", "-1", "1"],
}
NRZ_ANTIPOLAIRE = FORMES["NRZ antipolaire [-1, 1]"]

DOSSIER = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.abspath(os.path.join(DOSSIER, "..", ".."))
FICHIER_RESULTATS = os.path.join(DOSSIER, "resultats.json")


# ------------------------------------------------------------------
# 1. Calculs theoriques
# ------------------------------------------------------------------
def Q(x):
    """Fonction Q : probabilite qu'une gaussienne centree reduite depasse x."""
    return 0.5 * math.erfc(x / math.sqrt(2))


def dichotomie(f, cible, a, b):
    """Cherche x dans [a, b] tel que f(x) = cible (f decroissante)."""
    for _ in range(100):
        milieu = (a + b) / 2
        if f(milieu) > cible:
            a = milieu
        else:
            b = milieu
    return (a + b) / 2


def teb_nrz_antipolaire(ebn0):
    """TEB theorique du NRZ antipolaire (ebn0 en lineaire)."""
    return Q(math.sqrt(2 * ebn0))


def teb_avec_codeur(ebn0_utile):
    """TEB theorique avec le codeur de l'etape 5 (3 bits emis par bit utile).

    Chaque bit du canal ne recoit que le tiers de l'energie du bit utile.
    Le decodeur se trompe si au moins 2 bits sur 3 sont faux.
    """
    p = teb_nrz_antipolaire(ebn0_utile / 3)
    return 3 * p**2 - 2 * p**3


def db(x):
    return 10 * math.log10(x)


def lin(x_db):
    return 10 ** (x_db / 10)


def calculs_theoriques():
    """Calcule les reponses aux questions 1, 2 et 3."""
    # Eb/N0 minimal pour atteindre TEB = 1e-3 (recherche par dichotomie)
    ebn0_sans = dichotomie(teb_nrz_antipolaire, TEB_CIBLE, 0.01, 100)
    ebn0_avec = dichotomie(teb_avec_codeur, TEB_CIBLE, 0.01, 100)

    resultats = {}
    for nom, ebn0 in [("sans", ebn0_sans), ("avec", ebn0_avec)]:
        eb_recu = ebn0 * N0                    # energie par bit utile au recepteur
        eb_emis = eb_recu * ATTENUATION        # energie a fournir par l'emetteur
        conso_jour = eb_emis * BITS_PAR_JOUR   # energie consommee par jour
        resultats[nom] = {
            "ebn0_db": db(ebn0),
            "eb_recu": eb_recu,
            "eb_emis": eb_emis,
            "conso_jour": conso_jour,
            "jours": BATTERIE_J / conso_jour,
        }
    return resultats


def afficher_reponses(r):
    s, a = r["sans"], r["avec"]
    print("=" * 66)
    print(" ENVIRONNEMENT 1 - Reponses (theorie)")
    print("=" * 66)
    print(f" N0 = {N0:.0e} J  |  attenuation = x{ATTENUATION:.0f}  |  TEB cible = {TEB_CIBLE:.0e}")
    print("\n Q1. Solution : NRZ antipolaire [-1, 1], sans codeur")
    print(f"     Eb/N0 necessaire     : {s['ebn0_db']:.2f} dB")
    print(f"     Eb recu              : {s['eb_recu']:.2e} J")
    print(f"     Eb emis (x10 000)    : {s['eb_emis']:.2e} J/bit  (~{s['eb_emis'] * 1e6:.2f} uJ/bit)")
    print(f"\n Q2. Consommation       : {s['conso_jour']:.3f} J/jour")
    print(f"     Autonomie batterie   : {s['jours']:.2f} jours")
    print("\n Q3. Avec le codeur de l'etape 5 (3 bits emis par bit utile)")
    print(f"     Eb/N0 par bit canal  : {db(lin(a['ebn0_db']) / 3):.2f} dB")
    print(f"     Eb/N0 par bit utile  : {a['ebn0_db']:.2f} dB  (+{a['ebn0_db'] - s['ebn0_db']:.2f} dB)")
    print(f"     Eb emis              : {a['eb_emis']:.2e} J/bit  (+{(a['eb_emis'] / s['eb_emis'] - 1) * 100:.0f} %)")
    print(f"     Autonomie batterie   : {a['jours']:.2f} jours  (au lieu de {s['jours']:.2f})")
    print("=" * 66)


# ------------------------------------------------------------------
# 2. Simulations avec notre simulateur Java
# ------------------------------------------------------------------
def copier_classes():
    """Copie bin/ dans un dossier temporaire.

    Le projet est sur Google Drive : la JVM y charge les classes tres
    lentement (~30 s par simulation contre ~1 s en local).
    """
    dossier_tmp = tempfile.mkdtemp(prefix="sit213_")
    destination = os.path.join(dossier_tmp, "bin")
    shutil.copytree(os.path.join(RACINE, "bin"), destination)
    return destination


def simuler(classpath, arguments):
    """Lance une simulation et renvoie le TEB affiche par le simulateur."""
    commande = ["java", "-cp", classpath, "simulateur.Simulateur",
                "-mess", str(NB_BITS), "-seed", str(SEED)] + arguments
    sortie = subprocess.run(commande, capture_output=True, text=True).stdout
    return float(sortie.split("TEB :")[-1])


def lancer_simulations():
    classpath = copier_classes()
    taches = {}  # (courbe, Eb/N0) -> arguments du simulateur

    # Courbes du graphique 1 : formes d'onde sans codeur
    for nom, args in FORMES.items():
        for x in EBN0_DB:
            taches[(nom, x)] = args + ["-snrpb", str(x)]

    # Courbe du graphique 2 : NRZ antipolaire avec codeur.
    # -snrpb est le Eb/N0 d'un bit emis sur le canal. Un bit utile = 3 bits
    # emis, donc Eb/N0 (bit canal) = Eb/N0 (bit utile) - 10 log10(3).
    for x in EBN0_DB:
        snr_canal = x - db(3)
        taches[("codeur", x)] = NRZ_ANTIPOLAIRE + ["-codeur", "-snrpb", f"{snr_canal:.3f}"]

    print(f"\n{len(taches)} simulations de {NB_BITS} bits en cours...")
    with ThreadPoolExecutor() as executeur:
        tebs = executeur.map(lambda args: simuler(classpath, args), taches.values())
        mesures = dict(zip(taches.keys(), tebs))

    resultats = {}
    for (courbe, x), teb in mesures.items():
        resultats.setdefault(courbe, {})[str(x)] = teb

    with open(FICHIER_RESULTATS, "w") as f:
        json.dump(resultats, f, indent=2)
    print(f"Resultats sauvegardes dans {os.path.relpath(FICHIER_RESULTATS, RACINE)}")
    return resultats


def points(resultats, courbe):
    """Points (Eb/N0, TEB) d'une courbe, sans les TEB nuls (axe log)."""
    xs, ys = [], []
    for x in EBN0_DB:
        teb = resultats[courbe][str(x)]
        if teb > 0:
            xs.append(x)
            ys.append(teb)
    return xs, ys


def demo():
    """Quatre simulations a lancer devant le jury."""
    classpath = copier_classes()
    essais = [
        ("NRZ antipolaire a 6,8 dB (notre solution)", NRZ_ANTIPOLAIRE + ["-snrpb", "6.8"]),
        ("RZ a 6,8 dB", FORMES["RZ [0, 1]"] + ["-snrpb", "6.8"]),
        ("NRZT antipolaire a 6,8 dB", FORMES["NRZT antipolaire [-1, 1]"] + ["-snrpb", "6.8"]),
        ("NRZ antipolaire + codeur, 3,4 dB par bit canal (= 8,2 dB par bit utile)",
         NRZ_ANTIPOLAIRE + ["-codeur", "-snrpb", "3.4"]),
    ]
    for titre, args in essais:
        print(f"\n> {titre}")
        print("  java -cp bin simulateur.Simulateur -mess", NB_BITS, "-seed", SEED, " ".join(args))
        print(f"  TEB = {simuler(classpath, args)}")


# ------------------------------------------------------------------
# 3. Graphiques
# ------------------------------------------------------------------
def courbe_theorique(fonction):
    xs = [i / 10 for i in range(0, 101)]
    return xs, [fonction(lin(x)) for x in xs]


def mise_en_forme(ax, titre, xlabel):
    ax.axhline(TEB_CIBLE, color="red", linestyle="--", label="TEB cible = $10^{-3}$")
    ax.set_yscale("log")
    ax.set_ylim(1e-5, 1)
    ax.set_xlim(0, 10)
    ax.set_title(titre)
    ax.set_xlabel(xlabel)
    ax.set_ylabel("TEB")
    ax.grid(True, which="both", alpha=0.3)
    ax.legend()


def graphique_formes(resultats, theorie):
    fig, ax = plt.subplots(figsize=(8, 5.5))
    ax.plot(*courbe_theorique(teb_nrz_antipolaire), color="gray", label="Théorie NRZ antipolaire")
    for nom, marqueur in zip(FORMES, ["o", "s", "^", "D"]):
        ax.plot(*points(resultats, nom), marqueur + "-", label=f"Simulé : {nom}")

    x = theorie["sans"]["ebn0_db"]
    ax.axvline(x, color="green", linestyle=":")
    ax.text(x + 0.2, 2.5e-3, f"{x:.2f} dB", color="green",
            fontsize=10, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="green", lw=0.8, alpha=0.9))

    mise_en_forme(ax, "Q1 - Comparaison des formes d'onde (canal BBAG)", "$E_b/N_0$ (dB)")
    sauvegarder(fig, "1_formes_onde.png")


def graphique_codeur(resultats, theorie):
    fig, ax = plt.subplots(figsize=(8, 5.5))
    ax.plot(*courbe_theorique(teb_nrz_antipolaire), color="tab:blue", alpha=0.5, label="Théorie sans codeur")
    ax.plot(*courbe_theorique(teb_avec_codeur), color="tab:orange", alpha=0.5, label="Théorie avec codeur")
    ax.plot(*points(resultats, "NRZ antipolaire [-1, 1]"), "o", color="tab:blue", label="Simulé sans codeur")
    ax.plot(*points(resultats, "codeur"), "s", color="tab:orange", label="Simulé avec codeur")

    x1, x2 = theorie["sans"]["ebn0_db"], theorie["avec"]["ebn0_db"]
    ax.axvline(x1, color="tab:blue", linestyle=":")
    ax.axvline(x2, color="tab:orange", linestyle=":")

    # Annotations des seuils a TEB = 1e-3
    ax.text(x1 - 0.15, 3e-5, f"{x1:.2f} dB", color="tab:blue", ha="right", va="bottom",
            fontsize=9, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:blue", lw=0.8, alpha=0.9))
    ax.text(x2 + 0.15, 3e-5, f"{x2:.2f} dB", color="tab:orange", ha="left", va="bottom",
            fontsize=9, fontweight="bold",
            bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="tab:orange", lw=0.8, alpha=0.9))

    # Fleche de surconsommation energetique (+1.37 dB)
    ax.annotate("", xy=(x2, TEB_CIBLE), xytext=(x1, TEB_CIBLE),
                arrowprops=dict(arrowstyle="<->", color="black", lw=1.8))
    ax.text((x1 + x2) / 2, 2.2e-3, f"+{x2 - x1:.2f} dB\n(+37 % d'énergie)",
            ha="center", va="bottom", fontsize=9, fontweight="bold", color="black",
            bbox=dict(boxstyle="round,pad=0.25", fc="white", ec="black", lw=0.8, alpha=0.95))

    mise_en_forme(ax, "Q3 - Impact du codeur à énergie par bit utile égale",
                  "$E_b/N_0$ par bit utile (dB)")
    sauvegarder(fig, "2_codeur.png")


def graphique_batterie(theorie):
    s, a = theorie["sans"], theorie["avec"]
    fig, ax = plt.subplots(figsize=(6, 4.5))
    barres = ax.bar(["Sans codeur", "Avec codeur"], [s["jours"], a["jours"]],
                    color=["tab:blue", "tab:orange"], width=0.5)
    for barre, r in zip(barres, [s, a]):
        ax.text(barre.get_x() + barre.get_width() / 2, barre.get_height() + 0.1,
                f"{r['jours']:.2f} jours\n({r['eb_emis'] * 1e6:.2f} µJ/bit)", ha="center")
    ax.set_ylim(0, 8)
    ax.set_ylabel("Autonomie (jours)")
    ax.set_title("Q2 / Q3 - Autonomie de la batterie (3 J, $10^6$ bits/jour)")
    ax.grid(axis="y", alpha=0.3)
    sauvegarder(fig, "3_batterie.png")


def sauvegarder(fig, nom):
    fig.tight_layout()
    fig.savefig(os.path.join(DOSSIER, nom), dpi=150)
    plt.close(fig)
    print(f"Graphique : mesures/environnement1/{nom}")


# ------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description="SIT213 - Etape 6 - Environnement 1")
    parser.add_argument("--tracer", action="store_true", help="trace depuis resultats.json sans resimuler")
    parser.add_argument("--demo", action="store_true", help="lance 4 simulations commentees")
    options = parser.parse_args()

    theorie = calculs_theoriques()
    afficher_reponses(theorie)

    if options.demo:
        demo()
        return

    if options.tracer:
        with open(FICHIER_RESULTATS) as f:
            resultats = json.load(f)
    else:
        resultats = lancer_simulations()

    graphique_formes(resultats, theorie)
    graphique_codeur(resultats, theorie)
    graphique_batterie(theorie)


if __name__ == "__main__":
    main()
