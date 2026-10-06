package transmetteurs;

import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class DecodeurTest {

    private Decodeur decodeur;

    private static class DestinationTest extends Destination<Boolean> {
        @Override
        public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
            this.informationRecue = information;
        }
    }

    @BeforeEach
    void setUp() {
        decodeur = new Decodeur();
    }

    @Test
    @DisplayName("Le décodeur démarre sans information reçue ni émise")
    void testEtatInitial() {
        assertNull(decodeur.getInformationRecue());
        assertNotNull(decodeur.getInformationEmise());
        assertEquals(0, decodeur.getInformationEmise().nbElements());
    }

    @Test
    @DisplayName("Décodage parfait des triplets valides 010 -> 0 et 101 -> 1")
    void testDecodageSansErreur() throws Exception {
        Information<Boolean> sansErreur = new Information<>(new Boolean[]{
                false, true, false,  // 010 -> 0
                true, false, true    // 101 -> 1
        });
        decodeur.recevoir(sansErreur);

        Information<Boolean> emission = decodeur.getInformationEmise();
        assertEquals(2, emission.nbElements());
        assertFalse(emission.iemeElement(0));
        assertTrue(emission.iemeElement(1));
    }

    @ParameterizedTest(name = "Triplet {0} -> bit attendu {1}")
    @DisplayName("Validation des 8 cas de distance minimale de Hamming")
    @CsvSource({
            "000, false", // proche de 010
            "001, true",  // proche de 101
            "010, false", // exact 010
            "011, false", // proche de 010
            "100, true",  // proche de 101
            "101, true",  // exact 101
            "110, false", // proche de 010
            "111, true"   // proche de 101
    })
    void testTableDecodageHuitCas(String triplet, boolean attendu) throws Exception {
        Information<Boolean> info = new Information<>(new Boolean[]{
                triplet.charAt(0) == '1',
                triplet.charAt(1) == '1',
                triplet.charAt(2) == '1'
        });
        decodeur.recevoir(info);

        assertEquals(1, decodeur.getInformationEmise().nbElements());
        assertEquals(attendu, decodeur.getInformationEmise().iemeElement(0));
    }

    @Test
    @DisplayName("Rejet d'une séquence dont la taille n'est pas un multiple de 3")
    void testRejetTailleNonMultipleDeTrois() {
        assertThrows(InformationNonConformeException.class,
                () -> decodeur.recevoir(new Information<>(new Boolean[]{true})));
        assertThrows(InformationNonConformeException.class,
                () -> decodeur.recevoir(new Information<>(new Boolean[]{true, false})));
        assertThrows(InformationNonConformeException.class,
                () -> decodeur.recevoir(new Information<>(new Boolean[]{true, false, true, false})));
    }

    @Test
    @DisplayName("Rejet d'une information reçue nulle")
    void testRejetInformationNulle() {
        assertThrows(InformationNonConformeException.class, () -> decodeur.recevoir(null));
    }

    @Test
    @DisplayName("Décodage d'une information vide")
    void testInformationVide() throws Exception {
        decodeur.recevoir(new Information<>());
        assertEquals(0, decodeur.getInformationEmise().nbElements());
    }

    @Test
    @DisplayName("Transmission automatique aux destinations connectées")
    void testTransmissionAutomatique() throws Exception {
        DestinationTest destination = new DestinationTest();
        decodeur.connecter(destination);

        decodeur.recevoir(new Information<>(new Boolean[]{false, true, false}));
        assertEquals(decodeur.getInformationEmise(), destination.getInformationRecue());
        assertEquals(1, destination.getInformationRecue().nbElements());
        assertFalse(destination.getInformationRecue().iemeElement(0));
    }
}
