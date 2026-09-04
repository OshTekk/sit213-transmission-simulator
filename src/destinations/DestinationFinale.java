package destinations;

import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant la destination finale.
 *
 * <p>La destination se contente de recevoir le signal du composant
 * auquel elle est connectée.</p>
 *
 * @author Aymeric Schaeffer
 * @version 1.0
 */
public class DestinationFinale extends Destination<Boolean> {
    /**
     * Reçoit et mémorise une information booléenne.
     *
     * @param information l'information reçue
     * @throws InformationNonConformeException si l'information n'est pas conforme
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        informationRecue = information;
    }
}
