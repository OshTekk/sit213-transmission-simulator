package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

/**
 * CodeRZ
 * Permet d'encoder ou de décoder un signal selon le format RZ.
 *
 * @author Antonin Pons
 */
public class CodeRZ extends CodeBinaire {
    /**
     * Constructeur de la classe CodeRZ
     * @param nbEchParBit
     *      Nombre d'échantillon par bit
     * @param amplitudeMin
     *      Amplitude minimale du signal NRZ
     * @param amplitudeMax
     *      Amplitude maximale du signal NRZ
     */
    public CodeRZ(int nbEchParBit, float amplitudeMin, float amplitudeMax) {
        super(nbEchParBit, amplitudeMin, amplitudeMax);

        if (amplitudeMax <= 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude maximale doit être supérieure à 0.");

        if (amplitudeMin != 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être nulle.");

        if (nbEchParBit <= 0 || nbEchParBit % 3 != 0)
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être un multiple de 3 strictement positif en CodeRZ.");
    }

    /**
     * Encoder une suite binaire suivant le format RZ
     * @param information Les bits à émettre.
     * @return
     *      Signal RZ échantilloné
     */
    public Information<Float> encoder(Information<Boolean> information) {
        Information<Float> infoEncodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;

        for (Boolean bit : information) {
            // Si le bit est a 1, amplitudeMax, sinon amplitudeMin
            float amplitudeMilieu = bit ? amplitudeMax : amplitudeMin;
            ajouterEchantillons(infoEncodee, amplitudeMin, tiersNbEchParBit);       // Début en bas
            ajouterEchantillons(infoEncodee, amplitudeMilieu, tiersNbEchParBit);    // Haut ou bas au milieu
            ajouterEchantillons(infoEncodee, amplitudeMin, tiersNbEchParBit);       // Fin en bas
        }
        return infoEncodee;
    }

    /**
     * Decoder une suite d'échantillons suivant le format RZ
     * @param information Signal RZ échantilloné
     * @return
     *      Suite de bits
     */
    public Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException {
        if (information.nbElements() % nbEchParBit != 0)
            // Le nombre d'échantillon par bits
            throw new InformationNonConformeException();

        Information<Boolean> infoDecodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        float amplitudeMilieu = (amplitudeMax + amplitudeMin) / 2;
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            float amplitudeMoy = 0;
            for (int i = 0; i < tiersNbEchParBit; i++) {
                amplitudeMoy += it.next();
            }
            amplitudeMoy /= tiersNbEchParBit;
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            infoDecodee.add(amplitudeMoy > amplitudeMilieu);
        }
        return infoDecodee;
    }
}
