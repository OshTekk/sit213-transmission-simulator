package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

public class CodeNRZ extends CodeBinaire {
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

    public Information<Float> encoder(Information<Boolean> information) {
        Information<Float> infoEncodee = new Information<>();
        for (Boolean bit : information) {
            float amplitudeBit = bit ? amplitudeMax : amplitudeMin;
            ajouterEchantillons(infoEncodee, amplitudeBit, nbEchParBit);
        }
        return infoEncodee;
    }

    public Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException {
        if (information.nbElements() % nbEchParBit != 0)
            throw new InformationNonConformeException();

        Information<Boolean> infoDecodee = new Information<>();
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            float amplitude = 0;
            for (int i = 0; i < nbEchParBit; i++) {
                amplitude = it.next();
            }
            infoDecodee.add(amplitude == amplitudeMax);
        }
        return infoDecodee;
    }


}
