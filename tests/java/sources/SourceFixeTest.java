package sources;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceFixeTest {

    @Test
    @DisplayName("Conservation des bits et de leur ordre d'émission")
    void testOrdreDesBits() throws Exception {
        SourceFixe source = new SourceFixe("0101100");
        assertNull(source.getInformationEmise());
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true, false, true, true, false, false}), source.getInformationEmise());
    }

    @Test
    @DisplayName("Émission d'un message d'un seul bit")
    void testMessageUnSeulBit() throws Exception {
        SourceFixe source = new SourceFixe("1");
        source.emettre();
        assertEquals(1, source.getInformationEmise().nbElements());
        assertTrue(source.getInformationEmise().iemeElement(0));
    }

    @Test
    @DisplayName("Émission d'une chaîne vide")
    void testChaineVide() throws Exception {
        SourceFixe source = new SourceFixe("");
        source.emettre();
        assertEquals(0, source.getInformationEmise().nbElements());
    }

    @Test
    @DisplayName("Traitement des caractères non binaires")
    void testCaracteresNonBinaires() throws Exception {
        SourceFixe source = new SourceFixe("0x1, ");
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true}), source.getInformationEmise());
    }

    @Test
    @DisplayName("Rejet d'une chaîne de message nulle")
    void testChaineNulle() {
        assertThrows(NullPointerException.class, () -> new SourceFixe(null));
    }
}
