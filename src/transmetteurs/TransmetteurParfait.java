package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant un émetteur/récepteur parfait.
 * L'information émise en sortie est strictement identique à l'information reçue en entrée.
 *
 * @param <T> le type des données transmises
 * @author Antonin Pons
 * @version 1.0
 */
public class TransmetteurParfait<T> extends Transmetteur<T, T> {

    public TransmetteurParfait() {
        super();
    }

    /**
     * Reçoit une information en entrée du transmetteur.
     * Pour un transmetteur parfait, l'information émise devient l'information reçue.
     * Cette méthode déclenche automatiquement l'émission vers les destinations connectées.
     *
     * @param information l'information reçue
     * @throws InformationNonConformeException si l'information reçue est invalide
     */
    @Override
    public void recevoir(Information<T> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être null.");
        }
        this.informationRecue = information;
        this.informationEmise = this.informationRecue;
        this.emettre();
    }

    /**
     * Émet l'information vers toutes les destinations connectées en sortie.
     *
     * @throws InformationNonConformeException si une anomalie survient lors de l'émission
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<T> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }
}