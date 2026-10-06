package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Encode les bits logiques avant leur transmission sur le canal.
 * Chaque bit est remplacé par un triplet : {@code 0 -> 010} et
 * {@code 1 -> 101}. Ces triplets permettent au décodeur de corriger
 * une erreur portant sur un seul bit par triplet.
 *
 * <p>Les triplets produits lors de chaque réception sont ajoutés à
 * l'information émise par ce codeur. L'envoi aux destinations connectées
 * s'effectue lors de l'appel à {@link #emettre()}.</p>
 */
public class Codeur extends Transmetteur<Boolean, Boolean> {
    /**
     * Construit un codeur avec une information émise initialement vide.
     */
    public Codeur() {
        super();
        this.informationEmise = new Information<Boolean>();
    }

    /**
     * Mémorise l'information reçue et ajoute son codage à l'information émise.
     * Les bits déjà codés sont conservés.
     *
     * @param information les bits à coder
     * @throws InformationNonConformeException déclarée par l'interface des
     *         destinations ; cette implémentation ne la déclenche pas directement
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        this.informationRecue = information;
        encoder();
    }

    /**
     * Transmet l'information codée à chaque destination connectée.
     *
     * @throws InformationNonConformeException si une destination refuse
     *         l'information codée
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Boolean> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    /**
     * Ajoute à l'information émise les triplets correspondant aux bits reçus.
     */
    private void encoder() {
        for (Boolean bit : this.informationRecue) {
            this.informationEmise.add(bit);
            this.informationEmise.add(!bit);
            this.informationEmise.add(bit);
        }
    }
}
