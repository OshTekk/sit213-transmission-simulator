package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

public abstract class CodeBinaire implements CodeBinaireInterface {
    protected float amplitudeMin;
    protected float amplitudeMax;
    protected int nbEchParBit;

    public CodeBinaire(int nbEchantillonParSymbole, float amplitudeMin, float amplitudeMax) {
        this.nbEchParBit = nbEchantillonParSymbole;
        this.amplitudeMin = amplitudeMin;
        this.amplitudeMax = amplitudeMax;
    }

    public abstract Information<Float> encoder(Information<Boolean> information);

    public abstract Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException;

    protected void ajouterEchantillons(Information<Float> info, float amplitude, int nbEchantillons) {
        for (int i = 0; i < nbEchantillons; i++) {
            info.add(amplitude);
        }
    }
}
