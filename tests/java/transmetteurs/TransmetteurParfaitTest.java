package transmetteurs;

import destinations.DestinationFinale;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransmetteurParfaitTest {
    @Test void constructeurInitialiseLesInformationsANull() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        assertNull(transmetteur.getInformationRecue());
        assertNull(transmetteur.getInformationEmise());
    }

    @Test void recevoirTransmetAutomatiquementSansAlteration() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale destination = new DestinationFinale();
        transmetteur.connecter(destination);
        Information<Boolean> message = new Information<>(new Boolean[]{true, false, true});
        transmetteur.recevoir(message);
        assertEquals(message, transmetteur.getInformationRecue());
        assertEquals(message, transmetteur.getInformationEmise());
        assertEquals(message, destination.getInformationRecue());
    }

    @Test void recevoirSansDestinationMemoriseLeMessage() throws Exception {
        TransmetteurParfait<String> transmetteur = new TransmetteurParfait<>();
        Information<String> message = new Information<>(new String[]{"a", "b"});
        transmetteur.recevoir(message);
        assertEquals(message, transmetteur.getInformationEmise());
    }

    @Test void recevoirUneInformationVide() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.recevoir(new Information<>());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
    }

    @Test void recevoirNullRejetteSansEcraserLeMessagePrecedent() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        Information<Boolean> message = new Information<>(new Boolean[]{true});
        transmetteur.recevoir(message);
        assertThrows(InformationNonConformeException.class, () -> transmetteur.recevoir(null));
        assertEquals(message, transmetteur.getInformationRecue());
        assertEquals(message, transmetteur.getInformationEmise());
    }

    @Test void emettreAToutesLesDestinationsEtDeconnecter() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale a = new DestinationFinale();
        DestinationFinale b = new DestinationFinale();
        transmetteur.connecter(a);
        transmetteur.connecter(b);
        Information<Boolean> premier = new Information<>(new Boolean[]{true});
        transmetteur.recevoir(premier);
        assertEquals(premier, a.getInformationRecue());
        assertEquals(premier, b.getInformationRecue());
        transmetteur.deconnecter(b);
        Information<Boolean> second = new Information<>(new Boolean[]{false});
        transmetteur.recevoir(second);
        assertEquals(second, a.getInformationRecue());
        assertEquals(premier, b.getInformationRecue());
        a.recevoir(null);
        transmetteur.emettre();
        assertEquals(second, a.getInformationRecue());
    }

    @Test void emettreAvantReceptionSansDestinationNeFaitRien() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.emettre();
        assertNull(transmetteur.getInformationEmise());
    }

    @Test void emettreAvantReceptionTransmetActuellementNull() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>(new Boolean[]{true}));
        transmetteur.connecter(destination);
        transmetteur.emettre();
        assertNull(destination.getInformationRecue());
    }

    @Test void exceptionDestinationInterromptLEmission() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.connecter(new Destination<Boolean>() {
            public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
                throw new InformationNonConformeException("rejet volontaire du test");
            }
        });
        DestinationFinale suivante = new DestinationFinale();
        transmetteur.connecter(suivante);
        assertThrows(InformationNonConformeException.class,
                () -> transmetteur.recevoir(new Information<>(new Boolean[]{true})));
        assertNull(suivante.getInformationRecue());
    }

    @Test void destinationNulleProvoqueActuellementUneException() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.connecter(null);
        assertThrows(NullPointerException.class, () -> transmetteur.emettre());
    }
}
