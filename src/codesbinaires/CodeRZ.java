package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

public class CodeRZ extends CodeBinaire {
    public CodeRZ(int nbEchantillonParSymbole, float amplitudeMin, float amplitudeMax) {
        super(nbEchantillonParSymbole, amplitudeMin, amplitudeMax);

        if (amplitudeMax <= 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude maximale doit être supérieure à 0.");

        if (amplitudeMin != 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être nulle.");

        if (nbEchantillonParSymbole <= 0 || nbEchantillonParSymbole % 3 != 0)
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être un multiple de 3 strictement positif en CodeRZ.");
    }

    public Information<Float> encoder(Information<Boolean> information) {
        Information<Float> infoEncodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        for (Boolean bit : information) {
            float amplitudeMilieu = bit ? amplitudeMax : amplitudeMin;
            ajouterEchantillons(infoEncodee, amplitudeMin, tiersNbEchParBit);
            ajouterEchantillons(infoEncodee, amplitudeMilieu, tiersNbEchParBit);
            ajouterEchantillons(infoEncodee, amplitudeMin, tiersNbEchParBit);
        }
        return infoEncodee;
    }

    public Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException {
        if (information.nbElements() % nbEchParBit != 0)
            throw new InformationNonConformeException();

        Information<Boolean> infoDecodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            float amplitudeMilieu = 0;
            for (int i = 0; i < tiersNbEchParBit; i++) {
                amplitudeMilieu = it.next();
            }
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            infoDecodee.add(amplitudeMilieu == amplitudeMax);
        }
        return infoDecodee;
    }
}
