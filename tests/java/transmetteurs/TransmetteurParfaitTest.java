package transmetteurs;

import destinations.DestinationFinale;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransmetteurParfaitTest {
    /** Vérifie qu'aucune information n'est reçue ni émise à la construction. */
    @Test void constructeurInitialiseLesInformationsANull() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        assertNull(transmetteur.getInformationRecue());
        assertNull(transmetteur.getInformationEmise());
    }

    /** Vérifie qu'une réception est automatiquement retransmise sans modification. */
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

    /** Vérifie qu'une réception est mémorisée même sans destination connectée. */
    @Test void recevoirSansDestinationMemoriseLeMessage() throws Exception {
        TransmetteurParfait<String> transmetteur = new TransmetteurParfait<>();
        Information<String> message = new Information<>(new String[]{"a", "b"});
        transmetteur.recevoir(message);
        assertEquals(message, transmetteur.getInformationEmise());
    }

    /** Vérifie que le transmetteur accepte et émet une information vide. */
    @Test void recevoirUneInformationVide() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.recevoir(new Information<>());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
    }

    /** Vérifie qu'une réception nulle est rejetée sans effacer le message précédent. */
    @Test void recevoirNullRejetteSansEcraserLeMessagePrecedent() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        Information<Boolean> message = new Information<>(new Boolean[]{true});
        transmetteur.recevoir(message);
        assertThrows(InformationNonConformeException.class, () -> transmetteur.recevoir(null));
        assertEquals(message, transmetteur.getInformationRecue());
        assertEquals(message, transmetteur.getInformationEmise());
    }

    /** Vérifie l'envoi aux destinations connectées et l'effet d'une déconnexion. */
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

    /** Vérifie qu'émettre sans réception ni destination laisse l'état initial inchangé. */
    @Test void emettreAvantReceptionSansDestinationNeFaitRien() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.emettre();
        assertNull(transmetteur.getInformationEmise());
    }

    /** Caractérise l'envoi actuel de null avant la première réception. */
    @Test void emettreAvantReceptionTransmetActuellementNull() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale destination = new DestinationFinale();
        destination.recevoir(new Information<>(new Boolean[]{true}));
        transmetteur.connecter(destination);
        transmetteur.emettre();
        assertNull(destination.getInformationRecue());
    }

    /** Vérifie que le rejet d'une destination empêche l'envoi aux suivantes. */
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

    /** Caractérise l'exception actuelle quand une destination nulle est connectée. */
    @Test void destinationNulleProvoqueActuellementUneException() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.connecter(null);
        assertThrows(NullPointerException.class, () -> transmetteur.emettre());
    }
}
