package codesbinaires;

import information.Information;

public class CodeNRZT extends CodeBinaire {
    public CodeNRZT(int nbEchParBit, float amplitudeMin, float amplitudeMax) {
        super(nbEchParBit, amplitudeMin, amplitudeMax);
        if (amplitudeMin >= amplitudeMax)
            throw new AmplitudeInvalideException("Erreur, l'amplitude minimale doit être inférieure à l'amplitude maximale.");
    }

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

    public Information<Boolean> decoder(Information<Float> information) {
        Information<Boolean> infoDecodee = new Information<>();
        return infoDecodee;
    }

    private void ajouterPente(Information<Float> information, int nbEchantillons, float depart, float fin) {
        float pente = (fin-depart)/nbEchantillons;
        for (int i = 0; i < nbEchantillons; i++) {
            information.add(depart + i * pente);
        }
    }

    public static void main(String[] args) {
        CodeNRZT c = new CodeNRZT(12, -5, 5);
        Information<Boolean> info = new Information<>();
        info.add(true);
        info.add(false);
        info.add(true);
        info.add(true);
        info.add(false);
        Information<Float> output = c.encoder(info);
        System.out.println("end");
    }
}
