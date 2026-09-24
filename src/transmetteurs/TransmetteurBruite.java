package transmetteurs;

import java.util.Random;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Transmetteur introduisant un bruit gaussien à l'émission
 */
public class TransmetteurBruite extends Transmetteur<Float, Float> {
    private Float puissanceBruit;
    private final Random random;
    private final Float snrpb;
    private final Integer nbEchParBit;

    /**
     * Constructeur du transmetteur bruité utilisant le SNR par bit
     * @param snrpb
     *      Rapport signal sur bruit par bit
     * @param nbEchParBit
     *      Nombre d'échantillon par bit
     * @param semence
     *      Semence utilisée pour générer les échantillons du bruit
     */
    public TransmetteurBruite(float snrpb, int nbEchParBit, Integer semence) {
        super();
        this.snrpb = snrpb;
        this.nbEchParBit = nbEchParBit;
        this.puissanceBruit = null;
        this.informationEmise = new Information<>();
        this.random = (semence != null) ? new Random(semence) : new Random();
    }

    public TransmetteurBruite(float snrpb, int nbEchParBit) {
        this(snrpb, nbEchParBit, null);
    }


    /**
     * Constructeur du Transmetteur bruité
     * @param puissanceBruit
     *      Puissance du bruit généré
     * @param semence
     *      Semence utilisée pour générer les échantillons du bruit
     *
     */
    public TransmetteurBruite(float puissanceBruit, Integer semence) {
        super();
        this.snrpb = null;
        this.nbEchParBit = null;
        this.puissanceBruit = puissanceBruit;
        this.informationEmise = new Information<>();
        this.random = (semence != null) ? new Random(semence) : new Random();
    }

    public TransmetteurBruite(float puissanceBruit) {
        this(puissanceBruit, null);
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
        this.informationEmise = new Information<>();
        this.calculPuissanceBruit();
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
        for (Float echantillon : this.informationRecue) {
            float bruit = this.genereEchantillonBruit();
            this.informationEmise.add(echantillon + bruit);
        }
    }

    private float genereEchantillonBruit() {
        float a1 = this.random.nextFloat();
        float a2 = this.random.nextFloat();
        return (float) (Math.sqrt(this.puissanceBruit) * Math.sqrt(-2 * Math.log(1 - a1)) * Math.cos(2 * Math.PI * a2));
    }

    private void calculPuissanceBruit() {
        if (this.snrpb != null && this.nbEchParBit != null) {
            float snrLin = (float) Math.pow(10.0f, snrpb / 10.0f);
            float pSignal = this.calculPuissanceSignal();
            this.puissanceBruit = (pSignal * (float) nbEchParBit) / (2.0f * snrLin);
        }
    }

    private float calculPuissanceSignal() {
        float sommeQuadratique = 0.0f;
        float nEchantillons = (float) this.informationRecue.nbElements();
        if (nEchantillons == 0.0f) {
            return 0.0f;
        }
        for (Float echantillon : this.informationRecue) {
            sommeQuadratique += echantillon * echantillon;
        }
        return sommeQuadratique / nEchantillons;
    }
}
