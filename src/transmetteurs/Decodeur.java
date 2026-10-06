package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

public class Decodeur extends Transmetteur<Boolean, Boolean> {
    public Decodeur() {
        super();
        this.informationEmise = new Information<>();
    }

    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être nulle.");
        }
        if (information.nbElements() % 3 != 0) {
            throw new InformationNonConformeException("Il doit y avoir un nombre de bits multiple de trois.");
        }
        this.informationRecue = information;
        this.informationEmise = new Information<>();
        this.creerInformationEmise();
        this.emettre();
    }

    /**
     * Emet l'information émise vers toutes les destinations connectées
     * @throws InformationNonConformeException si une destination refuse l'information
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Boolean> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    /**
     * Traitement des bits reçus : chaque triplet de bit est
     * décodé à l'aide de la decoderBits(a, b, c).
     */
    private void creerInformationEmise() {
        boolean a, b, c, bitDecode;
        Iterator<Boolean> it = this.informationRecue.iterator();
        while (it.hasNext()) {
            a = it.next();
            b = it.next();
            c = it.next();
            bitDecode = this.decoderBits(a, b, c);
            this.informationEmise.add(bitDecode);
        }
    }

    /**
     * Décode un triplet de bits
     * 101 -> 1
     * 010 -> 0
     * Si un bit sur trois est faux, le résultat du décodeur
     * sera quand même bon
     * @param a premier bit du triplet
     * @param b deuxième bit du triplet
     * @param c troisième bit du triplet
     * @return bit décodé
     */
    private boolean decoderBits(boolean a, boolean b, boolean c) {
        return (a && !b) || (a && c) || (!b && c);
    }
}
