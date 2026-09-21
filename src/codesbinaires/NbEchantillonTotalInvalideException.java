package codesbinaires;

/**
 * Signale que le nombre total d'échantillons ne permet pas un décodage valide.
 *
 * @author Antonin Pons
 */
public class NbEchantillonTotalInvalideException extends RuntimeException {
    /**
     * Construit l'exception avec un message explicatif.
     *
     * @param message description du nombre total d'échantillons invalide
     */
    public NbEchantillonTotalInvalideException(String message) {
        super(message);
    }
}
