package codesbinaires;

import information.Information;
import information.InformationNonConformeException;

/**
 * Interface permettant d'encoder une suite binaire ou de décoder un signal en RZ, NRZ ou NRZT
 */
public interface CodeBinaireInterface {

    /**
     * Encode une suite de bits en un signal échantillonné.
     *
     * @param information Les bits à émettre.
     * @return Le signal échantillonné sous forme de flottants.
     */
    Information<Float> encoder(Information<Boolean> information);

    /**
     * Décode un signal échantillonné pour restituer la suite de bits émise.
     *
     * @param information Le signal reçu échantillonné.
     * @return Les bits décodés.
     * @throws InformationNonConformeException si le signal ne respecte pas le format attendu
     */
    Information<Boolean> decoder(Information<Float> information) throws InformationNonConformeException;
}
