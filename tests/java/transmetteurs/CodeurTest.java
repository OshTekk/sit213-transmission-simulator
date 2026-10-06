package transmetteurs;

import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeurTest {

    private Codeur codeur;

    private static class DestinationTest extends Destination<Boolean> {
        @Override
        public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
            this.informationRecue = information;
        }
    }

    @BeforeEach
    void setUp() {
        codeur = new Codeur();
    }

    @Test
    @DisplayName("Le codeur démarre sans information reçue ni émise")
    void testEtatInitial() {
        assertNull(codeur.getInformationRecue());
        assertNotNull(codeur.getInformationEmise());
        assertEquals(0, codeur.getInformationEmise().nbElements());
    }

    @Test
    @DisplayName("Codage d'un bit 0 en triplet 010")
    void testCodageBitZero() throws Exception {
        Information<Boolean> entree = new Information<>(new Boolean[]{false});
        codeur.recevoir(entree);

        Information<Boolean> emission = codeur.getInformationEmise();
        assertEquals(3, emission.nbElements());
        assertEquals(false, emission.iemeElement(0));
        assertEquals(true, emission.iemeElement(1));
        assertEquals(false, emission.iemeElement(2));
    }

    @Test
    @DisplayName("Codage d'un bit 1 en triplet 101")
    void testCodageBitUn() throws Exception {
        Information<Boolean> entree = new Information<>(new Boolean[]{true});
        codeur.recevoir(entree);

        Information<Boolean> emission = codeur.getInformationEmise();
        assertEquals(3, emission.nbElements());
        assertEquals(true, emission.iemeElement(0));
        assertEquals(false, emission.iemeElement(1));
        assertEquals(true, emission.iemeElement(2));
    }

    @Test
    @DisplayName("Codage d'une séquence de bits et dimensionnement exact (3 * N)")
    void testCodageSequenceComplete() throws Exception {
        Information<Boolean> entree = new Information<>(new Boolean[]{true, false, true});
        codeur.recevoir(entree);

        Information<Boolean> emission = codeur.getInformationEmise();
        assertEquals(9, emission.nbElements());
        Boolean[] attendu = {true, false, true, false, true, false, true, false, true};
        for (int i = 0; i < attendu.length; i++) {
            assertEquals(attendu[i], emission.iemeElement(i));
        }
    }

    @Test
    @DisplayName("Propriété de synchronisation : jamais plus de 2 bits consécutifs identiques")
    void testProprieteSynchronisation() throws Exception {
        Information<Boolean> entree = new Information<>(new Boolean[]{false, false, false, true, true, true});
        codeur.recevoir(entree);

        Information<Boolean> emission = codeur.getInformationEmise();
        int consecutifs = 1;
        Boolean precedent = null;
        for (Boolean b : emission) {
            if (b.equals(precedent)) {
                consecutifs++;
                assertTrue(consecutifs <= 2, "Erreur : plus de 2 bits consécutifs identiques trouvés");
            } else {
                consecutifs = 1;
            }
            precedent = b;
        }
    }

    @Test
    @DisplayName("Rejet d'une information reçue nulle")
    void testRejetInformationNulle() {
        assertThrows(InformationNonConformeException.class, () -> codeur.recevoir(null));
    }

    @Test
    @DisplayName("Codage d'une information vide")
    void testInformationVide() throws Exception {
        codeur.recevoir(new Information<>());
        assertEquals(0, codeur.getInformationEmise().nbElements());
    }

    @Test
    @DisplayName("Transmission automatique aux destinations connectées")
    void testTransmissionAutomatique() throws Exception {
        DestinationTest destination = new DestinationTest();
        codeur.connecter(destination);

        Information<Boolean> entree = new Information<>(new Boolean[]{false, true});
        codeur.recevoir(entree);

        assertEquals(codeur.getInformationEmise(), destination.getInformationRecue());
        assertEquals(6, destination.getInformationRecue().nbElements());
    }
}
