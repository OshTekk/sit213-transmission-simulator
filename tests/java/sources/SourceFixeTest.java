package sources;

import information.Information;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceFixeTest {
    /** Vérifie que la source restitue les bits du message dans leur ordre initial. */
    @Test void conserveLesBitsEtLeurOrdre() throws Exception {
        SourceFixe source = new SourceFixe("0101100");
        assertNull(source.getInformationEmise());
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true, false, true, true, false, false}), source.getInformationEmise());
    }

    /** Vérifie qu'un message d'un seul bit est émis correctement. */
    @Test void accepteUnSeulBit() throws Exception {
        SourceFixe source = new SourceFixe("1");
        source.emettre();
        assertEquals(1, source.getInformationEmise().nbElements());
        assertTrue(source.getInformationEmise().iemeElement(0));
    }

    // Caractérisation : le sujet ne précise pas ces entrées du constructeur.
    /** Vérifie qu'une chaîne vide produit une information vide. */
    @Test void chaineVideProduitUneInformationVide() throws Exception {
        SourceFixe source = new SourceFixe("");
        source.emettre();
        assertEquals(0, source.getInformationEmise().nbElements());
    }

    /** Caractérise l'ignorance actuelle des caractères autres que zéro et un. */
    @Test void caracteresNonBinairesSontIgnoresActuellement() throws Exception {
        SourceFixe source = new SourceFixe("0x1, ");
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true}), source.getInformationEmise());
    }

    /** Caractérise l'exception actuelle provoquée par une chaîne nulle. */
    @Test void chaineNulleProvoqueActuellementUneException() {
        assertThrows(NullPointerException.class, () -> new SourceFixe(null));
    }
}
