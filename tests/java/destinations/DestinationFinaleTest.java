package destinations;

import information.Information;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DestinationFinaleTest {
    @Test void constructeurInitialiseLaReceptionANull() {
        assertNull(new DestinationFinale().getInformationRecue());
    }

    @Test void memoriseLeDernierMessage() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        Information<Boolean> premier = new Information<>(new Boolean[]{true, false});
        Information<Boolean> second = new Information<>(new Boolean[]{false});
        destination.recevoir(premier);
        assertEquals(premier, destination.getInformationRecue());
        destination.recevoir(second);
        assertEquals(second, destination.getInformationRecue());
    }

    @Test void accepteUneInformationVide() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>());
        assertNotNull(destination.getInformationRecue());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    @Test void nullEffaceActuellementLeMessage() throws Exception {
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>(new Boolean[]{true}));
        destination.recevoir(null);
        assertNull(destination.getInformationRecue());
    }
}
