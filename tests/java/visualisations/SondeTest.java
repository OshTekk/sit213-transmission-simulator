package visualisations;

import information.Information;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SondeTest {

    @Test
    void constructeurConserveLeNom() {
        Sonde<Boolean> sonde = new SondeDeTest<>("Sonde entrée");

        assertEquals("Sonde entrée", sonde.nom);
    }

    @Test
    void constructeurInitialiseInformationRecueANull() {
        Sonde<Boolean> sonde = new SondeDeTest<>("Sonde entrée");

        assertNull(sonde.getInformationRecue());
    }

    @Test
    void constructeurAccepteActuellementUnNomNull() {
        Sonde<Boolean> sonde = assertDoesNotThrow(() -> new SondeDeTest<Boolean>(null));

        assertNull(sonde.nom);
    }

    @Test
    void constructeurAccepteUnNomVide() {
        Sonde<Boolean> sonde = assertDoesNotThrow(() -> new SondeDeTest<Boolean>(""));

        assertEquals("", sonde.nom);
    }

    @Test
    void constructeurConserveLesEspacesDuNom() {
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
