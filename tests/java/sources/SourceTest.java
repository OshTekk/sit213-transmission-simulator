package sources;

import destinations.Destination;
import destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Méthodes fournies, conservées dans le périmètre car elles assurent l'émission réelle.
class SourceTest {
    @Test void emissionSansDestinationMetAJourInformationEmise() throws Exception {
        SourceFixe source = new SourceFixe("01");
        assertNull(source.getInformationEmise());
        source.emettre();
        assertEquals(new Information<>(new Boolean[]{false, true}), source.getInformationEmise());
    }

    @Test void emissionVersPlusieursDestinationsPuisDeconnexion() throws Exception {
        SourceFixe source = new SourceFixe("01");
        DestinationFinale a = new DestinationFinale();
        DestinationFinale b = new DestinationFinale();
        source.connecter(a);
        source.connecter(b);
        source.emettre();
        assertEquals(source.getInformationEmise(), a.getInformationRecue());
        assertEquals(source.getInformationEmise(), b.getInformationRecue());
        source.deconnecter(b);
        source.deconnecter(b); // Retirer une destination absente est sans effet.
        b.recevoir(null);
        source.emettre();
        assertNull(b.getInformationRecue());
        assertEquals(source.getInformationEmise(), a.getInformationRecue());
    }

    @Test void erreurDestinationInterromptEmissionEtLaisseInformationEmiseNulle() {
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

    @Test void connecterNullProvoqueUneExceptionLorsDeLEmission() {
        SourceFixe source = new SourceFixe("01");
        source.connecter(null);
        assertThrows(NullPointerException.class, source::emettre);
    }
}
