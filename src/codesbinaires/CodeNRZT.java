package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

public class CodeNRZT extends CodeBinaire {
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

    public Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException {
        if (information.nbElements() % nbEchParBit != 0)
            throw new InformationNonConformeException();

        Information<Boolean> infoDecodee = new Information<>();
        int tiersNbEchParBit = nbEchParBit / 3;
        float amplitudeMoyenne = (amplitudeMax + amplitudeMin) / 2.0f;
        float amplitudeRelevee;
        Iterator<Float> it = information.iterator();
        while (it.hasNext()) {
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
            amplitudeRelevee = 0.0f;
            for (int i = 0; i < tiersNbEchParBit; i++) {
                amplitudeRelevee += it.next();
            }
            amplitudeRelevee = amplitudeRelevee / 3;
            infoDecodee.add(amplitudeRelevee > amplitudeMoyenne);
            for (int i = 0; i < tiersNbEchParBit; i++) {
                it.next();
            }
        }

        return infoDecodee;
    }

    private void ajouterPente(Information<Float> information, int nbEchantillons, float depart, float fin) {
        float pente = (fin-depart)/nbEchantillons;
        for (int i = 0; i < nbEchantillons; i++) {
            information.add(depart + i * pente);
        }
    }
}
