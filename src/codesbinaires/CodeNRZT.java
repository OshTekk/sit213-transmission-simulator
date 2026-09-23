package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

/**
 * CodeNRZT
 * Permet d'encoder ou de décoder un signal selon le format NRZT.
 *
 * @author Antonin Pons
 */
public class CodeNRZT extends CodeBinaire {
    /**
     * Constructeur de la classe CodeNRZT
     * @param nbEchParBit
     *      Nombre d'échantillon par bit
     * @param amplitudeMin
     *      Amplitude minimale du signal NRZ
     * @param amplitudeMax
     *      Amplitude maximale du signal NRZ
     */
    public CodeNRZT(int nbEchParBit, float amplitudeMin, float amplitudeMax) {
        super(nbEchParBit, amplitudeMin, amplitudeMax);

        if (nbEchParBit <= 0 || nbEchParBit % 3 != 0)
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être un multiple de 3 positif.");

        if (amplitudeMax < 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude maximale doit être positive.");

        if (amplitudeMin > 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être négative.");

        if (amplitudeMin >= amplitudeMax)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être inférieure à l'amplitude maximale.");

    }

    /**
     * Encoder une suite binaire suivant le format NRZT
     * @param information Les bits à émettre.
     * @return
     *      Signal NRZT échantilloné
     */
    public Information<Float> encoder(Information<Boolean> information) {
        Information<Float> infoEncodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        int nBits = information.nbElements();
        boolean bit;
        float amplitudeMoyenne = (amplitudeMax + amplitudeMin) / 2;
        float amplitudePic;

        for (int i = 0; i < nBits; i++) {
            bit = information.iemeElement(i);
            amplitudePic = bit ? amplitudeMax : amplitudeMin;

            if (i == 0 || bit != information.iemeElement(i - 1)) {
                ajouterPente(infoEncodee, tiersNbEchParBit, amplitudeMoyenne, amplitudePic);
            } else {
                ajouterEchantillons(infoEncodee, amplitudePic, tiersNbEchParBit);
            }

            ajouterEchantillons(infoEncodee, amplitudePic, tiersNbEchParBit);

            if (i == nBits - 1 || bit != information.iemeElement(i + 1)) {
                ajouterPente(infoEncodee, tiersNbEchParBit, amplitudePic, amplitudeMoyenne);
            } else {
                ajouterEchantillons(infoEncodee, amplitudePic, tiersNbEchParBit);
            }
        }

        return infoEncodee;
    }

    /**
     * Decoder une suite d'échantillons suivant le format NRZT
     * @param information Signal NRZT échantilloné
     * @return
     *      Suite de bits
     */
    public Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException {
        if (information.nbElements() % nbEchParBit != 0)
            throw new InformationNonConformeException();

        Information<Boolean> infoDecodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        float amplitudeMoyenne = (amplitudeMax + amplitudeMin) / 2.0f;
        float amplitudeMoy;
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            amplitudeMoy = 0.0f;
            for (int i = 0; i < tiersNbEchParBit; i++) {
                amplitudeMoy += it.next();
            }
            amplitudeMoy /= tiersNbEchParBit;
            infoDecodee.add(amplitudeMoy > amplitudeMoyenne);
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
        }

        return infoDecodee;
    }

    /**
     * Ajoute une transition linéaire entre deux amplitudes.
     *
     * @param information information à compléter
     * @param nbEchantillons nombre d'échantillons de la transition
     * @param depart amplitude de départ
     * @param fin amplitude d'arrivée
     */
    private void ajouterPente(Information<Float> information, int nbEchantillons, float depart, float fin) {
        float pente = (fin-depart)/nbEchantillons;
        for (int i = 0; i < nbEchantillons; i++) {
            information.add(depart + i * pente);
        }
    }
}
