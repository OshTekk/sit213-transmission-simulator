package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

/**
 * Base commune des codes en ligne binaires.
 *
 * @author Antonin Pons
 */
public abstract class CodeBinaire implements CodeBinaireInterface {
    protected float amplitudeMin;
    protected float amplitudeMax;
    protected int nbEchParBit;

    /**
     * Construit un code binaire avec ses paramètres d'échantillonnage et d'amplitude.
     *
     * @param nbEchantillonParSymbole nombre d'échantillons utilisés pour représenter un bit
     * @param amplitudeMin amplitude associée au niveau bas
     * @param amplitudeMax amplitude associée au niveau haut
     */
    public CodeBinaire(int nbEchantillonParSymbole, float amplitudeMin, float amplitudeMax) {
        this.nbEchParBit = nbEchantillonParSymbole;
        this.amplitudeMin = amplitudeMin;
        this.amplitudeMax = amplitudeMax;
    }

    /**
     * Encode une information binaire en signal échantillonné.
     *
     * @param information suite de bits à encoder
     * @return signal échantillonné correspondant
     */
    public abstract Information<Float> encoder(Information<Boolean> information);

    /**
     * Décode un signal échantillonné en information binaire.
     *
     * @param information signal à décoder
     * @return suite de bits décodée
     * @throws InformationNonConformeException si le signal ne respecte pas le format attendu
     */
    public abstract Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException;

    /**
     * Ajoute plusieurs échantillons de même amplitude à une information.
     *
     * @param info information à compléter
     * @param amplitude amplitude des échantillons ajoutés
     * @param nbEchantillons nombre d'échantillons à ajouter
     */
    protected void ajouterEchantillons(Information<Float> info, float amplitude, int nbEchantillons) {
        for (int i = 0; i < nbEchantillons; i++) {
            info.add(amplitude);
        }
    }
}
