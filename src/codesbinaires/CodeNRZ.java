package codesbinaires;

import information.Information;

public class CodeNRZ extends CodeBinaire {
    public CodeNRZ(int nbEchParBit, float amplitudeMin, float amplitudeMax) {
        if (amplitudeMin >= amplitudeMax)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être inférieure à l'amplitude maximale.");

        super(nbEchParBit, amplitudeMin, amplitudeMax);
    }

    public Information<Float> encoder(Information<Boolean> information) {
        Information<Float> infoEncodee = new Information<>();
        for (Boolean bit : information) {
            float amplitudeBit = bit ? amplitudeMax : amplitudeMin;
            ajouterEchantillons(infoEncodee, amplitudeBit, nbEchParBit);
        }
        return infoEncodee;
    }

    public Information<Boolean> decoder(Information<Float> information) {
        Information<Boolean> infoDecodee = new Information<>();
        return infoDecodee;
    }


}
