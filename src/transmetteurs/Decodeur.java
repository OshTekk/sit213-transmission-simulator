package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

import java.util.Iterator;

/**
 * Décode les bits logiques reçus du canal en appliquant la règle de distance
 * de Hamming minimale sur chaque triplet de bits.
 *
 * <p>Les triplets proches de {@code 010} (avec au plus une erreur) sont décodés
 * en {@code 0}, et ceux proches de {@code 101} sont décodés en {@code 1}.</p>
 *
 * @author Antonin Pons
 * @version 1.0
 */
public class Decodeur extends Transmetteur<Boolean, Boolean> {
    /**
     * Construit un décodeur avec une information émise initialement vide.
     */
    public Decodeur() {
        super();
        this.informationEmise = new Information<>();
    }

    /**
     * Reçoit une information codée sous forme de triplets, décode chaque triplet
     * et émet l'information décodée vers toutes les destinations connectées.
     *
     * @param information information codée reçue (doit être un multiple de 3)
     * @throws InformationNonConformeException si l'information est nulle ou si sa taille n'est pas un multiple de 3
     */
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
