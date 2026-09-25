#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Tracé de la gaussienne du bruit généré par le TransmetteurBruite Java.
Utilise directement `./script/simulateur`.
"""

import subprocess
import os
import sys
import numpy as np
import matplotlib.pyplot as plt
import scipy.stats as stats

# Paramètres de simulation
SNR_PB = 0.0          # SNR en dB (0 dB => Eb/N0 = 1)
NB_BITS = 50_000      # 50 000 bits pour avoir 1.5 million d'échantillons
NB_ECH = 30           # 30 échantillons par bit
FICHIER_OUT = "out.txt"

def lancer_simulation():
    """Exécute `./script/simulateur` avec un signal constant (bits à 1) pour isoler le bruit."""
    # Séquence de bits à 1 : signal émis s(t) = 1.0 constant partout (Ps = 1.0)
    message_constant = "1" * NB_BITS

    cmd = [
        "./script/simulateur",
        "-form", "NRZ",
        "-snrpb", str(SNR_PB),
        "-nbEch", str(NB_ECH),
        "-ampl", "-1.0", "1.0",
        "-mess", message_constant,
        "-save", FICHIER_OUT
    ]

    print(f"-> Exécution de {' '.join(cmd[:6])} ... -save {FICHIER_OUT}")
    try:
        subprocess.run(cmd, check=True)
        print("[OK] Simulation Java terminée avec succès.\n")
    except subprocess.CalledProcessError as e:
        print(f"[ERREUR] Échec lors de l'exécution de ./script/simulateur : {e}")
        sys.exit(1)

def main():
    # 1. Exécution du simulateur
    lancer_simulation()

    # 2. Chargement des échantillons reçus : r(n) = s(n) + b(n) = 1.0 + b(n)
    print(f"Chargement de '{FICHIER_OUT}'...")
    r = np.loadtxt(FICHIER_OUT)

    # 3. Extraction du bruit pur : b(n) = r(n) - 1.0
    bruit = r - 1.0

    # 4. Calcul de la variance théorique attendue :
    # sigma_b^2 = (Ps * N) / (2 * 10^(SNR/10)) avec Ps = 1.0
    snr_lin = 10.0 ** (SNR_PB / 10.0)
    variance_th = (1.0 * NB_ECH) / (2.0 * snr_lin)
    sigma_th = np.sqrt(variance_th)

    moyenne_emp = np.mean(bruit)
    variance_emp = np.var(bruit)
    sigma_emp = np.std(bruit)

    print(f"{'='*50}")
    print(f" ANALYSE DU BRUIT GAUSSIEN (N = {len(bruit)} éch.)")
    print(f"{'='*50}")
    print(f"Moyenne empirique (μ)    : {moyenne_emp:+.5f} (attendu ≈ 0.0)")
    print(f"Variance empirique (σ²)  : {variance_emp:.5f}")
    print(f"Variance théorique       : {variance_th:.5f}")
    print(f"Écart relatif variance   : {abs(variance_emp - variance_th)/variance_th * 100:.2f} %")
    print(f"{'='*50}\n")

    # 5. Tracé Matplotlib
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5))

    # --- Histogramme vs Courbe théorique ---
    count, bins, _ = ax1.hist(
        bruit, bins=120, density=True, alpha=0.6,
        color="royalblue", edgecolor="black", linewidth=0.3,
        label="Échantillons Java (Box-Muller)"
    )

    x = np.linspace(bins[0], bins[-1], 500)
    pdf_th = stats.norm.pdf(x, loc=0.0, scale=sigma_th)
    ax1.plot(x, pdf_th, 'r-', linewidth=2.2, label=rf"Théorie $\mathcal{{N}}(0, \sigma^2={variance_th:.2f})$")

    ax1.set_title(f"Distribution du bruit ($SNR = {SNR_PB}$ dB)\n$\\mu={moyenne_emp:.3f}, \\sigma^2={variance_emp:.3f}$", fontsize=11)
    ax1.set_xlabel("Amplitude du bruit $b(n)$", fontsize=10)
    ax1.set_ylabel("Densité de probabilité", fontsize=10)
    ax1.legend(loc="upper right")
    ax1.grid(True, linestyle="--", alpha=0.5)

    # --- Q-Q Plot ---
    # Échantillonnage de 10 000 points pour un affichage Q-Q fluide
    ech_qq = np.random.choice(bruit, size=min(10000, len(bruit)), replace=False)
    stats.probplot(ech_qq, dist="norm", plot=ax2)
    ax2.set_title("Q-Q Plot (Droite de Henry)", fontsize=11)
    ax2.set_xlabel("Quantiles théoriques", fontsize=10)
    ax2.set_ylabel("Quantiles observés", fontsize=10)
    ax2.grid(True, linestyle="--", alpha=0.5)

    plt.tight_layout()
    output_png = "gaussienne_bruit.png"
    plt.savefig(output_png, dpi=300)
    print(f"[OK] Graphique sauvegardé sous : {output_png}")
    plt.show()

if __name__ == "__main__":
    main()
