package emetteurs;

import codesbinaires.CodeBinaire;
import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;
import transmetteurs.Transmetteur;

/**
 * Composant émetteur qui convertit une information logique (bits)
 * en une information analogique (échantillons) selon un code en ligne.
 *
 * @author Nathan Weymeels
 * @version 1.0
 */
public class Emetteur extends Transmetteur<Boolean, Float> {

    /** Le code binaire utilisé pour la modulation (NRZ, RZ, NRZT) */
    private CodeBinaire codeBinaire;

    /**
     * Construit un émetteur avec un code binaire spécifique.
     *
     * @param codeBinaire le code à appliquer
     */
    public Emetteur(CodeBinaire codeBinaire) {
        super();
        this.codeBinaire = codeBinaire;
    }

    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information reçue ne peut pas être nulle.");
        }
        this.informationRecue = information;

        // Codage de l'information binaire en échantillons analogiques
        this.informationEmise = this.codeBinaire.coder(this.informationRecue);

        // Émission vers les composants suivants
        this.emettre();
    }

    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Float> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    public CodeBinaire getCodeBinaire() {
        return codeBinaire;
    }
}