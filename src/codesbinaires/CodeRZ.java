package codesbinaires;

import information.Information;

import java.util.Iterator;

public class CodeRZ extends CodeBinaire {
    public CodeRZ(int nbEchantillonParSymbole, float amplitudeMin, float amplitudeMax) {
        if (amplitudeMin != 0)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être nulle en CodeRZ.");

        if (nbEchantillonParSymbole % 3 != 0)
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être un multiple de 3 en CodeRZ.");

        super(nbEchantillonParSymbole, amplitudeMin, amplitudeMax);
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

    public Information<Boolean> decoder(Information<Float> information) {
        Information<Boolean> infoDecodee = new Information<>();
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {

        }
        return infoDecodee;
    }
}
