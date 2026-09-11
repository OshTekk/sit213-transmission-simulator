package recepteurs;

import information.Information;
import simulateur.Simulateur;

/**
 * Récepteur permettant de reconstruire une suite de bits
 * à partir d'échantillons booléens.
 *
 * Le nombre d'échantillons par bit est fourni par le Simulateur.
 */
public class Recepteur {

    /** Simulateur contenant les paramètres de la simulation. */
    private final Simulateur simulateur;

    /** Information binaire reconstruite. */
    private Information<Boolean> informationRecue;

    /** Nombre d'échantillons reçus pour le bit en cours. */
    private int compteurEchantillons;

    /** Nombre d'échantillons valant true pour le bit en cours. */
    private int nombreTrue;

    /**
     * Constructeur.
     *
     * @param simulateur simulateur contenant les paramètres
     */
    public Recepteur(Simulateur simulateur) {
        this.simulateur = simulateur;
        this.informationRecue = new Information<Boolean>();
        this.compteurEchantillons = 0;
        this.nombreTrue = 0;
    }

    /**
     * Reçoit un échantillon booléen.
     *
     * @param echantillon échantillon reçu
     */
    public void recevoir(Boolean echantillon) {

        compteurEchantillons++;

        if (echantillon) {
            nombreTrue++;
        }

        // On récupère la valeur définie dans le Simulateur
        int nbEchParBit =
                simulateur.getnbEchParBit();

        // Un bit complet a été reçu
        if (compteurEchantillons == nbEchParBit) {

            boolean bitReconstruit = nombreTrue > nbEchParBit / 2;

            informationRecue.add(bitReconstruit);

            // Réinitialisation pour le bit suivant
            compteurEchantillons = 0;
            nombreTrue = 0;
        }
    }

    /**
     * Retourne l'information binaire reconstruite.
     *
     * @return information reçue
     */
    public Information<Boolean> getInformationRecue() {
        return informationRecue;
    }
}
