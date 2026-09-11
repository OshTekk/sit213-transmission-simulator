package sources;

import information.*;

/**
 * Composant Source qui émet une séquence booléenne fixe passée en paramètre.
 * 
 * @author Amandine Touchais
 * @version 1.0
 */
public class SourceFixe extends Source<Boolean> {

    /**
     * Construit une source fixe à partir d'une chaîne de 0 et de 1.
     * 
     * @param chaine la suite de bits sous forme de String
     */
    public SourceFixe(String chaine) {
        super();

        Information<Boolean> information = new Information<Boolean>();

        for (int i = 0; i < chaine.length(); i++) {
            if (chaine.charAt(i) == '0') {
                information.add(false);
            } else if (chaine.charAt(i) == '1') {
                information.add(true);
            }
        }

        this.informationGeneree = information;
    }
}
