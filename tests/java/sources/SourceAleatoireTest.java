package sources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SourceAleatoireTest {
    @Test void constructeurParDefautGenereCentBits() throws Exception {
        SourceAleatoire source = new SourceAleatoire();
        assertEquals(100, source.getNbBits());
        assertNull(source.getSeed());
        assertNull(source.getInformationEmise());
        source.emettre();
        assertEquals(100, source.getInformationEmise().nbElements());
        for (Boolean bit : source.getInformationEmise()) assertNotNull(bit);
    }

    @ParameterizedTest @ValueSource(ints = {1, 7, 100, 1000})
    void respecteLaLongueur(int taille) throws Exception {
        SourceAleatoire source = new SourceAleatoire(taille);
        source.emettre();
        assertEquals(taille, source.getNbBits());
        assertNull(source.getSeed());
        assertEquals(taille, source.getInformationEmise().nbElements());
        for (Boolean bit : source.getInformationEmise()) assertNotNull(bit);
    }

    @ParameterizedTest @ValueSource(ints = {0, 42, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void memeGraineReproduitLeMessage(int graine) throws Exception {
        SourceAleatoire a = new SourceAleatoire(128, graine);
        SourceAleatoire b = new SourceAleatoire(128, graine);
        a.emettre();
        b.emettre();
        assertEquals(graine, a.getSeed());
        assertEquals(128, a.getNbBits());
        assertEquals(128, a.getInformationEmise().nbElements());
        assertEquals(a.getInformationEmise(), b.getInformationEmise());
    }

    @Test void graineNulleEstAcceptee() throws Exception {
        SourceAleatoire source = new SourceAleatoire(7, null);
        source.emettre();
        assertNull(source.getSeed());
        assertEquals(7, source.getInformationEmise().nbElements());
    }

    @ParameterizedTest @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void longueurNonPositiveProduitActuellementUnMessageVide(int taille) throws Exception {
        SourceAleatoire source = new SourceAleatoire(taille, 42);
        source.emettre();
        assertEquals(taille, source.getNbBits());
        assertEquals(0, source.getInformationEmise().nbElements());
    }

    @Test void deuxEmissionsConserventLeMessageGenere() throws Exception {
        SourceAleatoire source = new SourceAleatoire(20, 42);
        source.emettre();
        var premiereEmission = source.getInformationEmise();
        source.emettre();
        assertEquals(premiereEmission, source.getInformationEmise());
    }
}
