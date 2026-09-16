package transmetteurs;

import java.util.Random;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Transmetteur introduisant un bruit gaussien à l'émission
 */
public class TransmetteurBruite extends Transmetteur<Float, Float> {
    private final float puissanceBruit;
    private final Random random;

    /**
     * Constructeur du Transmetteur bruité
     * @param puissanceBruit
     *      Puissance du bruit à introduire
     */
    public TransmetteurBruite(float puissanceBruit) {
        super();

        this.puissanceBruit = puissanceBruit;
        this.informationEmise = new Information<>();
        this.random = new Random();
    }

    /**
     * Reçoit une information en entrée et la retransmet en sortie en introduisant un bruit gaussien
     * @param information  l'information reçue
     * @throws InformationNonConformeException
     *      L'information reçue n'est pas conforme
     */
    @Override
    public void recevoir(Information<Float> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être nulle.");
        }
        // réception de l'information reçue
        this.informationRecue = information;
        this.ajoutBruitGaussien();
        this.emettre();
    }

    /**
     * Emet l'information émise vers toutes les destinations connectées
     * @throws InformationNonConformeException
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Float> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    private void ajoutBruitGaussien() {
        int nbElem = this.informationRecue.nbElements();
        for (int i = 0; i < nbElem; i++) {
            float echantillonBruite = this.informationRecue.iemeElement(i)+this.genereEchantillonBruit();
            this.informationEmise.add(echantillonBruite);
        }
    }

    private float genereEchantillonBruit() {
        float a1 = this.random.nextFloat();
        float a2 = this.random.nextFloat();
        return (float) (Math.sqrt(this.puissanceBruit) * Math.sqrt(-2*Math.log(1-a1)) * Math.cos(2 * Math.PI * a2));
    }
}
