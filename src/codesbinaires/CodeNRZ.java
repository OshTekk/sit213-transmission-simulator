package codesbinaires;

import information.Information;

import java.util.Iterator;

public class CodeNRZ extends CodeBinaire {
    public CodeNRZ(int nbEchParBit, float amplitudeMin, float amplitudeMax) {
        super(nbEchParBit, amplitudeMin, amplitudeMax);
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

    public Information<Boolean> decoder(Information<Float> information) {
        Information<Boolean> infoDecodee = new Information<>();
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            float amplitude = 0;
            for (int i = 0; i < nbEchParBit && it.hasNext(); i++) {
                amplitude = it.next();
            }
            infoDecodee.add(amplitude == amplitudeMax);
        }
        return infoDecodee;
    }


}
