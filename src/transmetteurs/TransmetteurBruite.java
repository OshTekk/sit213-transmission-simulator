package transmetteurs;

import java.util.Random;

import codesbinaires.NbEchantillonInvalideException;
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
        if (nbEchParBit <= 0) {
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être strictement positif.");
        }
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
     * Reçoit une information en entrée et la retransmet en sortie en introduisant un bruit gaussien
     * @param information  l'information reçue
     * @throws InformationNonConformeException si l'information reçue n'est pas conforme
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
     * @throws InformationNonConformeException si une destination refuse l'information
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Float> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    /**
     * Ajoute un échantillon de bruit gaussien à chaque échantillon reçu.
     */
    private void ajoutBruitGaussien() {
        for (Float echantillon : this.informationRecue) {
            float bruit = this.genereEchantillonBruit();
            this.informationEmise.add(echantillon + bruit);
        }
    }

    /**
     * Génère un échantillon de bruit gaussien centré.
     *
     * @return échantillon de bruit dont la puissance correspond à la configuration
     */
    private float genereEchantillonBruit() {
        if (this.puissanceBruit == null || this.puissanceBruit <= 0.0f) {
            return 0.0f;
        }
        // Tirage de variables uniformes dans [0, 1[
        float a1 = this.random.nextFloat();
        float a2 = this.random.nextFloat();

        // Transformation de Box-Muller : u1 dans ]0, 1] avec sécurité contre ln(0)
        double u1 = 1.0 - (double) a1;
        if (u1 <= 0.0) {
            u1 = 1e-10;
        }

        // Échantillon gaussien centré dimensionné par l'écart-type
        return (float) (Math.sqrt(this.puissanceBruit) * Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * (double) a2));
    }

    /**
     * Méthode calculant la puissance du bruit en fonction du SNRPB et de la puissance du signal.
     * On réalise le calcul suivant :
     * (Ps * N) / (2 * SNR Linéaire).
     */
    private void calculPuissanceBruit() {
        if (this.snrpb != null && this.nbEchParBit != null) {
            float snrLin = (float) Math.pow(10.0f, snrpb / 10.0f);
            float pSignal = this.calculPuissanceSignal();
            this.puissanceBruit = (pSignal * (float) nbEchParBit) / (2.0f * snrLin);
        }
    }

    /**
     * Calcule la puissance du signal reçu en faisant la moyenne des carrés des échantillons.
     *
     * @return
     *      Puissance du signal
     */
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

    /**
     * Retourne la puissance du bruit calculée ou configurée.
     *
     * @return puissance du bruit
     */
    public Float getPuissanceBruit() {
        return this.puissanceBruit;
    }

    /**
     * Retourne le rapport signal sur bruit par bit configuré.
     *
     * @return rapport signal sur bruit par bit (en dB)
     */
    public Float getSnrpb() {
        return this.snrpb;
    }

    /**
     * Retourne le nombre d'échantillons par bit configuré.
     *
     * @return nombre d'échantillons par bit
     */
    public Integer getNbEchParBit() {
        return this.nbEchParBit;
    }
}
