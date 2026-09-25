package simulateur;

/**
 * Signale qu'un argument fourni au simulateur est invalide.
 */
public class ArgumentsException extends Exception {
    
    private static final long serialVersionUID = 1789L; 
		
    /**
     * Construit l'exception avec un message explicatif.
     *
     * @param s description de l'argument invalide
     */
    public ArgumentsException(String s) {
	super(s);
    }
}
