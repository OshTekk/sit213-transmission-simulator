package recepteurs;

import codesbinaires.CodeBinaire;
import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;
import transmetteurs.Transmetteur;

/**
 * Composant récepteur qui convertit une information analogique
 * (échantillons Float) en une information logique (bits Boolean)
 * selon un code en ligne.
 *
 * @author Amandine TOUCHAIS
 * @version 1.0
 */
public class Recepteur extends Transmetteur<Float, Boolean> {

    /** Le code binaire utilisé pour le décodage. */
    private CodeBinaire codeBinaire;

    /**
     * Construit un récepteur avec un code binaire spécifique.
     *
     * @param codeBinaire le code utilisé pour le décodage
     */
    public Recepteur(CodeBinaire codeBinaire) {
        super();
        this.codeBinaire = codeBinaire;
    }

    @Override
    public void recevoir(Information<Float> information)
            throws InformationNonConformeException {

        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être nulle.");
        }

        // On mémorise l'information reçue
        this.informationRecue = information;

        // Décodage des échantillons en bits
        this.informationEmise = this.codeBinaire.decoder(this.informationRecue);

        // Émission vers le composant suivant
        this.emettre();
    }

    @Override
    public void emettre()
            throws InformationNonConformeException {

        for (DestinationInterface<Boolean> destination
                : this.destinationsConnectees) {

            destination.recevoir(this.informationEmise);
        }
    }

    /**
     * Retourne le code binaire utilisé par le récepteur.
     *
     * @return le code binaire
     */
    public CodeBinaire getCodeBinaire() {
        return codeBinaire;
    }
}
