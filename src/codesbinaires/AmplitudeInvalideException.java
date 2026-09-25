package codesbinaires;

/**
 * Signale qu'une amplitude fournie à un code binaire est invalide.
 */
public class AmplitudeInvalideException extends RuntimeException {
    /**
     * Construit l'exception avec un message explicatif.
     *
     * @param message description de l'amplitude invalide
     */
    public AmplitudeInvalideException(String message) {
        super(message);
    }
}
