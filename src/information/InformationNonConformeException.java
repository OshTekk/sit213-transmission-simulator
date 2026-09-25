package information;

/**
 * Signale qu'une information ne respecte pas le format attendu par un composant.
 */
public class InformationNonConformeException extends Exception {
   
    private static final long serialVersionUID = 1917L;
    
    /**
     * Construit une exception sans motif détaillé.
     */
    public InformationNonConformeException() {
	super();
    }
   
    /**
     * Construit une exception avec le motif de la non-conformité.
     *
     * @param motif description de la non-conformité
     */
    public InformationNonConformeException(String motif) {
	super(motif);
    }
}
