package sources;

import information.Information;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceFixeTest {
    @Test void conserveLesBitsEtLeurOrdre() throws Exception {
        SourceFixe source = new SourceFixe("0101100");
        assertNull(source.getInformationEmise());
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true, false, true, true, false, false}), source.getInformationEmise());
    }

    @Test void accepteUnSeulBit() throws Exception {
        SourceFixe source = new SourceFixe("1");
        source.emettre();
        assertEquals(1, source.getInformationEmise().nbElements());
        assertTrue(source.getInformationEmise().iemeElement(0));
    }

    // Caractérisation : le sujet ne précise pas ces entrées du constructeur.
    @Test void chaineVideProduitUneInformationVide() throws Exception {
        SourceFixe source = new SourceFixe("");
        source.emettre();
        assertEquals(0, source.getInformationEmise().nbElements());
    }

    @Test void caracteresNonBinairesSontIgnoresActuellement() throws Exception {
        SourceFixe source = new SourceFixe("0x1, ");
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true}), source.getInformationEmise());
    }

    @Test void chaineNulleProvoqueActuellementUneException() {
        assertThrows(NullPointerException.class, () -> new SourceFixe(null));
    }
}
