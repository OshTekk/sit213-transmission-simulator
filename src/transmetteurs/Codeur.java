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
 * <p>Les triplets produits lors de chaque réception sont transmis
 * automatiquement aux destinations connectées.</p>
 *
 * @author Aymeric Schaeffer
 * @version 1.0
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
     * Mémorise l'information reçue, encode chaque bit sous forme de triplet
     * et émet l'information codée vers toutes les destinations connectées.
     *
     * @param information les bits à coder
     * @throws InformationNonConformeException si l'information reçue est nulle ou invalide
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être nulle.");
        }
        this.informationRecue = information;
        this.informationEmise = new Information<>();
        encoder();
        this.emettre();
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
