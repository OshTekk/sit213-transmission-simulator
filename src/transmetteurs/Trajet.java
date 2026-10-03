package transmetteurs;

/**
 * Représente un trajet indirect caractérisé par son décalage temporel
 * (en nombre d'échantillons) et son amplitude relative.
 *
 * @param decalage décalage temporel en nombre d'échantillons ({@code dt >= 0})
 * @param amplitude amplitude relative par rapport au trajet direct ({@code 0.0 <= ar <= 1.0})
 * @author Nathan Weymeels
 * @version 1.0
 */
public record Trajet(int decalage, float amplitude) {}
