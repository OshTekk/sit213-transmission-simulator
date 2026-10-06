package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

public class Codeur extends Transmetteur<Boolean, Boolean> {
    public Codeur() {
        super();
        this.informationEmise = new Information<Boolean>();
    }

    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        this.informationRecue = information;
        encoder();
    }

    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Boolean> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    private void encoder() {
        for (Boolean bit : this.informationRecue) {
            this.informationEmise.add(bit);
            this.informationEmise.add(!bit);
            this.informationEmise.add(bit);
        }
    }
}
