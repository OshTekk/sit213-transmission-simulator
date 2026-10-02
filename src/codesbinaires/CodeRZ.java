package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

/**
 * CodeRZ
 * Permet d'encoder ou de décoder un signal selon le format RZ.
 */
public class CodeRZ extends CodeBinaire {
    /**
     * Constructeur de la classe CodeRZ
     * @param nbEchParBit
     *      Nombre d'échantillons par bit
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

        if (nbEchParBit < 3)
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être au moins de 3 en CodeRZ.");
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
            ajouterEchantillons(infoEncodee, amplitudeMin, tiersNbEchParBit);
            ajouterEchantillons(infoEncodee, amplitudeMilieu, tiersNbEchParBit + nbEchParBit % 3);
            ajouterEchantillons(infoEncodee, amplitudeMin, tiersNbEchParBit);
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
            // Le nombre d'échantillons par bits
            throw new InformationNonConformeException();

        Information<Boolean> infoDecodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        int nbEchMilieu = tiersNbEchParBit + (nbEchParBit % 3);
        float amplitudeMilieu = (amplitudeMax + amplitudeMin) / 2;
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            float amplitudeMoy = 0;
            for (int i = 0; i < nbEchMilieu; i++) {
                amplitudeMoy += it.next();
            }
            amplitudeMoy /= nbEchMilieu;
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            infoDecodee.add(amplitudeMoy > amplitudeMilieu);
        }
        return infoDecodee;
    }
}
