package transmetteurs;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import codesbinaires.NbEchantillonInvalideException;
import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Transmetteur introduisant un bruit gaussien et/ou des multitrajets à l'émission
 */
public class TransmetteurBruite extends Transmetteur<Float, Float> {
    private Float puissanceBruit;
    private final List<Trajet> multiTrajets;
    private final Random random;
    private final Float snrpb;
    private final Integer nbEchParBit;

    /**
     * Constructeur pour rétrocompatibilité (Étape 3) sans trajets indirects ni semence.
     *
     * @param snrpb rapport signal sur bruit par bit
     * @param nbEchParBit nombre d'échantillons par bit
     */
    public TransmetteurBruite(float snrpb, int nbEchParBit) {
        this(snrpb, nbEchParBit, Collections.emptyList(), null);
    }

    /**
     * Constructeur pour rétrocompatibilité (Étape 3) avec semence et sans trajets indirects.
     *
     * @param snrpb rapport signal sur bruit par bit
     * @param nbEchParBit nombre d'échantillons par bit
     * @param semence semence pour le générateur aléatoire
     */
    public TransmetteurBruite(float snrpb, int nbEchParBit, Integer semence) {
        this(snrpb, nbEchParBit, Collections.emptyList(), semence);
    }

    /**
     * Constructeur avec multitrajets sans semence.
     *
     * @param snrpb rapport signal sur bruit par bit (éventuellement null si pas de bruit)
     * @param nbEchParBit nombre d'échantillons par bit
     * @param multiTrajets liste des trajets indirects
     */
    public TransmetteurBruite(Float snrpb, int nbEchParBit, List<Trajet> multiTrajets) {
        this(snrpb, nbEchParBit, multiTrajets, null);
    }

    /**
     * Constructeur complet du transmetteur bruité utilisant le SNR par bit et les multitrajets.
     *
     * @param snrpb rapport signal sur bruit par bit (éventuellement null si pas de bruit)
     * @param nbEchParBit nombre d'échantillons par bit
     * @param multiTrajets liste des trajets indirects
     * @param semence semence utilisée pour générer les échantillons du bruit
     */
    public TransmetteurBruite(Float snrpb, int nbEchParBit, List<Trajet> multiTrajets, Integer semence) {
        super();
        if (nbEchParBit <= 0) {
            throw new NbEchantillonInvalideException("Erreur, le nombre d'échantillon par bit doit être strictement positif.");
        }
        this.snrpb = snrpb;
        this.nbEchParBit = nbEchParBit;
        this.multiTrajets = (multiTrajets != null) ? multiTrajets : Collections.emptyList();
        this.puissanceBruit = null;
        this.informationEmise = new Information<>();
        this.random = (semence != null) ? new Random(semence) : new Random();
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

        // création du signal à transmettre
        this.calculPuissanceBruit();
        this.creerInformationEmise();

        // transmission du signal
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
     * Création d'informationEmise
     */
    private void creerInformationEmise() {
        int i_max = this.informationRecue.nbElements(); // Évite de recalculer a chaque itération
        for (int i=0; i < i_max ; i++) {                // Iteration sur chaque bit reçu
            // Calcul du bruit
            float bruit = this.genereEchantillonBruit();

            // Calcul du multitrajet
            float multiTrajets = 0;
            for (Trajet trajet : this.multiTrajets) {   // Pour chaque trajet
                if (trajet.decalage() <= i) {           // on vérifie si le décalage concerne le bit actuel
                    // auquel cas le bit retardé est calculé
                    multiTrajets += trajet.amplitude() * this.informationRecue.iemeElement(i-trajet.decalage());
                }
            }

            // On publie le bit reçu + bruit + multitrajet
            this.informationEmise.add(
                    this.informationRecue.iemeElement(i) + bruit + multiTrajets
            );
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
        double sommeQuadratique = 0.0;
        double nEchantillons = (double) this.informationRecue.nbElements();
        if (nEchantillons == 0.0f) {
            return 0.0f;
        }
        for (Float echantillon : this.informationRecue) {
            sommeQuadratique += (double) echantillon * (double) echantillon;
        }
        return (float) (sommeQuadratique / nEchantillons);
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

    /**
     * Retourne la liste des trajets indirects configurés.
     *
     * @return liste des trajets indirects
     */
    public List<Trajet> getMultiTrajets() {
        return this.multiTrajets;
    }
}
