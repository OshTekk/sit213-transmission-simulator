package destinations;

import information.Information;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DestinationFinaleTest {
    /** Vérifie qu'aucune information n'est reçue avant la première transmission. */
    @Test void constructeurInitialiseLaReceptionANull() {
        assertNull(new DestinationFinale().getInformationRecue());
    }

    /** Vérifie que la destination conserve la dernière information reçue. */
    @Test void memoriseLeDernierMessage() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        Information<Boolean> premier = new Information<>(new Boolean[]{true, false});
        Information<Boolean> second = new Information<>(new Boolean[]{false});
        destination.recevoir(premier);
        assertEquals(premier, destination.getInformationRecue());
        destination.recevoir(second);
        assertEquals(second, destination.getInformationRecue());
    }

    /** Vérifie qu'une information vide peut remplacer le message reçu. */
    @Test void accepteUneInformationVide() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>());
        assertNotNull(destination.getInformationRecue());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    /** Caractérise le comportement actuel : recevoir null efface le message précédent. */
    @Test void nullEffaceActuellementLeMessage() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>(new Boolean[]{true}));
        destination.recevoir(null);
        assertNull(destination.getInformationRecue());
    }
}
