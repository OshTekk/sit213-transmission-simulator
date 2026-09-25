package visualisations;

import information.Information;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SondeTest {

    /** Vérifie que la sonde conserve le nom fourni. */
    @Test
    void constructeurConserveLeNom() {
        Sonde<Boolean> sonde = new SondeDeTest<>("Sonde entrée");

        assertEquals("Sonde entrée", sonde.nom);
    }

    /** Vérifie qu'une sonde neuve n'a reçu aucune information. */
    @Test
    void constructeurInitialiseInformationRecueANull() {
        Sonde<Boolean> sonde = new SondeDeTest<>("Sonde entrée");

        assertNull(sonde.getInformationRecue());
    }

    /** Caractérise l'acceptation actuelle d'un nom nul. */
    @Test
    void constructeurAccepteActuellementUnNomNull() {
        Sonde<Boolean> sonde = assertDoesNotThrow(() -> new SondeDeTest<Boolean>(null));

        assertNull(sonde.nom);
    }

    /** Vérifie qu'un nom vide est conservé. */
    @Test
    void constructeurAccepteUnNomVide() {
        Sonde<Boolean> sonde = assertDoesNotThrow(() -> new SondeDeTest<Boolean>(""));

        assertEquals("", sonde.nom);
    }

    /** Vérifie que les espaces présents dans le nom sont conservés. */
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
