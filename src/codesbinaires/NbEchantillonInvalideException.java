package codesbinaires;

/**
 * Signale qu'un nombre d'échantillons par bit est invalide.
 *
 * @author Antonin Pons
 */
public class NbEchantillonInvalideException extends RuntimeException {
    /**
     * Construit l'exception avec un message explicatif.
     *
     * @param message description du nombre d'échantillons invalide
     */
    public NbEchantillonInvalideException(String message) {
        super(message);
    }
}
