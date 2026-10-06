package sources;

import destinations.Destination;
import destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceTest {

    @Test
    @DisplayName("Émission sans destination connectée")
    void testEmissionSansDestination() throws Exception {
        SourceFixe source = new SourceFixe("01");
        assertNull(source.getInformationEmise());
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true}), source.getInformationEmise());
    }

    @Test
    @DisplayName("Émission vers plusieurs destinations et déconnexion")
    void testEmissionMultiDestinationsEtDeconnexion() throws Exception {
        SourceFixe source = new SourceFixe("01");
        DestinationFinale a = new DestinationFinale();
        DestinationFinale b = new DestinationFinale();
        source.connecter(a);
        source.connecter(b);
        source.emettre();
        assertEquals(source.getInformationEmise(), a.getInformationRecue());
        assertEquals(source.getInformationEmise(), b.getInformationRecue());
        source.deconnecter(b);
        source.deconnecter(b); // Retirer une destination absente est sans effet
        b.recevoir(null);
        source.emettre();
        assertNull(b.getInformationRecue());
        assertEquals(source.getInformationEmise(), a.getInformationRecue());
    }

    @Test
    @DisplayName("Interruption de l'émission en cas d'exception sur une destination")
    void testErreurDestinationInterromptEmission() {
        SourceFixe source = new SourceFixe("01");
        source.connecter(new Destination<Boolean>() {
            public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
                throw new InformationNonConformeException("rejet volontaire du test");
            }
        });
        DestinationFinale suivante = new DestinationFinale();
        source.connecter(suivante);
        assertThrows(InformationNonConformeException.class, source::emettre);
        assertNull(source.getInformationEmise());
        assertNull(suivante.getInformationRecue());
    }

    @Test
    @DisplayName("Connexion d'une destination nulle")
    void testConnecterNull() {
        SourceFixe source = new SourceFixe("01");
        source.connecter(null);
        assertThrows(NullPointerException.class, source::emettre);
    }
}
