package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant un transceiver parfait.
 * L'information reçue en entrée est copiée en entrée.
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
     * Pour un transmetteur parfait, l'information émise est la même que l'information reçue.
     * Cette méthode déclenche automatiquement l'émission vers les destinations connectées.
     *
     * @param information information reçue
     * @throws InformationNonConformeException si l'information reçue est invalide
     */
    @Override
    public void recevoir(Information<T> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être nulle.");
        }
        // réception de l'information reçue
        this.informationRecue = information;
        // On copie l'information reçue en émission
        this.informationEmise = this.informationRecue;
        this.emettre();
    }

    /**
     * Émet l'information vers toutes les destinations connectées en sortie.
     *
     * @throws InformationNonConformeException si la destination reçoit une information invalide
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<T> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }
}