package destinations;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DestinationFinaleTest {

    @Test
    @DisplayName("Initialisation sans information reçue")
    void testEtatInitial() {
        assertNull(new DestinationFinale().getInformationRecue());
    }

    @Test
    @DisplayName("Mémorisation de la dernière information reçue")
    void testMemorisationMessage() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        Information<Boolean> premier = new Information<>(new Boolean[]{true, false});
        Information<Boolean> second = new Information<>(new Boolean[]{false});
        destination.recevoir(premier);
        assertEquals(premier, destination.getInformationRecue());
        destination.recevoir(second);
        assertEquals(second, destination.getInformationRecue());
    }

    @Test
    @DisplayName("Réception d'une information vide")
    void testInformationVide() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>());
        assertNotNull(destination.getInformationRecue());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    @Test
    @DisplayName("Réception d'une information nulle")
    void testReceptionNull() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>(new Boolean[]{true}));
        destination.recevoir(null);
        assertNull(destination.getInformationRecue());
    }
}
