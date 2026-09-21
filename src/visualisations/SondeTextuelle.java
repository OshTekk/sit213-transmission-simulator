package visualisations;
	
import information.Information;

/** 
 * Classe réalisant l'affichage (textuel) d'information composée
 * d'éléments de type T
 * @author prou
 */
public class SondeTextuelle<T> extends Sonde<T> {
   
    /**
     * pour construire une sonde textuelle
     * @param nom  le nom de la fenêtre d'affichage
     */
    public SondeTextuelle(String nom) {
	super(nom);
    }
   
    /**
     * Reçoit une information et l'affiche dans la sortie textuelle.
     *
     * @param information information à afficher
     */
    public void recevoir (Information<T> information) {
        informationRecue = information;
        System.out.println(nom + " : " + information);
    }
}
