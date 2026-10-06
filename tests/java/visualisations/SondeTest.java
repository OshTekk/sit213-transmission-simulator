package visualisations;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SondeTest {

    @Test
    @DisplayName("Conservation du nom fourni à la construction")
    void testConstructeurNom() {
        Sonde<Boolean> sonde = new SondeDeTest<>("Sonde entrée");
        assertEquals("Sonde entrée", sonde.nom);
    }

    @Test
    @DisplayName("Initialisation de l'information reçue à null")
    void testConstructeurInformationRecueNulle() {
        Sonde<Boolean> sonde = new SondeDeTest<>("Sonde entrée");
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Comportement avec un nom null")
    void testConstructeurNomNull() {
        Sonde<Boolean> sonde = assertDoesNotThrow(() -> new SondeDeTest<Boolean>(null));
        assertNull(sonde.nom);
    }

    @Test
    @DisplayName("Comportement avec un nom vide")
    void testConstructeurNomVide() {
        Sonde<Boolean> sonde = assertDoesNotThrow(() -> new SondeDeTest<Boolean>(""));
        assertEquals("", sonde.nom);
    }

    @Test
    @DisplayName("Conservation des espaces dans le nom")
    void testConstructeurNomAvecEspaces() {
        Sonde<Boolean> sonde = new SondeDeTest<>("  Sonde entrée  ");
        assertEquals("  Sonde entrée  ", sonde.nom);
    }

    /** Permet d'instancier Sonde sans ajouter de comportement de réception. */
    private static class SondeDeTest<T> extends Sonde<T> {
        SondeDeTest(String nom) {
            super(nom);
        }

        @Override
        public void recevoir(Information<T> information) {
            // Hors périmètre : Sonde ne fournit aucune implémentation de recevoir.
        }
    }
}
