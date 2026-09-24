package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

/**
 * CodeNRZ
 * Permet d'encoder ou de décoder un signal selon le format NRZ.
 *
 * @author Antonin Pons
 */
public class CodeNRZ extends CodeBinaire {
    /**
     * Constructeur de la classe CodeNRZ
     * @param nbEchParBit
     *      Nombre d'échantillons par bit
     * @param amplitudeMin
     *      Amplitude minimale du signal NRZ
     * @param amplitudeMax
     *      Amplitude maximale du signal NRZ
     */
    public CodeNRZ(int nbEchParBit, float amplitudeMin, float amplitudeMax) {
        super(nbEchParBit, amplitudeMin, amplitudeMax);
        if (nbEchParBit <= 0)
            throw new NbEchantillonInvalideException("Erreur, il doit y avoir au moins 1 échantillon par bit.");

        if (amplitudeMax < 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude maximale doit être positive.");

        if (amplitudeMin > 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être négative.");

        if (amplitudeMin >= amplitudeMax)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être inférieure à l'amplitude maximale.");
    }

    /**
     * Encoder une suite binaire suivant le format NRZ
     * @param information Les bits à émettre.
     * @return
     *      Signal NRZ échantilloné
     */
    public Information<Float> encoder(Information<Boolean> information) {
        Information<Float> infoEncodee = new Information<>();
        for (Boolean bit : information) {
            float amplitudeBit = bit ? amplitudeMax : amplitudeMin;
            ajouterEchantillons(infoEncodee, amplitudeBit, nbEchParBit);
        }
        return infoEncodee;
    }

    /**
     * Decoder une suite d'échantillons suivant le format NRZ
     * @param information Signal NRZ échantilloné
     * @return
     *      Suite de bits
     */
    public Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException {
        if (information.nbElements() % nbEchParBit != 0)
            throw new InformationNonConformeException();

        float amplitudeMilieu = (amplitudeMax + amplitudeMin) / 2;
        Information<Boolean> infoDecodee = new Information<>();
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            float amplitudeMoy = 0;
            for (int i = 0; i < nbEchParBit; i++) {
                amplitudeMoy += it.next();
            }
            amplitudeMoy /= nbEchParBit;
            infoDecodee.add(amplitudeMoy > amplitudeMilieu);
        }
        return infoDecodee;
    }


}
