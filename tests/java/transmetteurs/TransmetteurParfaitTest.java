package transmetteurs;

import destinations.DestinationFinale;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransmetteurParfaitTest {

    @Test
    @DisplayName("État initial du transmetteur parfait")
    void testEtatInitial() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        assertNull(transmetteur.getInformationRecue());
        assertNull(transmetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Transmission automatique sans altération de l'information")
    void testTransmissionParfaite() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale destination = new DestinationFinale();
        transmetteur.connecter(destination);
        Information<Boolean> message = new Information<>(new Boolean[]{true, false, true});
        transmetteur.recevoir(message);
        assertEquals(message, transmetteur.getInformationRecue());
        assertEquals(message, transmetteur.getInformationEmise());
        assertEquals(message, destination.getInformationRecue());
    }

    @Test
    @DisplayName("Réception sans destination connectée")
    void testReceptionSansDestination() throws Exception {
        TransmetteurParfait<String> transmetteur = new TransmetteurParfait<>();
        Information<String> message = new Information<>(new String[]{"a", "b"});
        transmetteur.recevoir(message);
        assertEquals(message, transmetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Traitement d'une information vide")
    void testInformationVide() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.recevoir(new Information<>());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
    }

    @Test
    @DisplayName("Rejet d'une information reçue nulle")
    void testRejetInformationNulle() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        Information<Boolean> message = new Information<>(new Boolean[]{true});
        transmetteur.recevoir(message);
        assertThrows(InformationNonConformeException.class, () -> transmetteur.recevoir(null));
        assertEquals(message, transmetteur.getInformationRecue());
        assertEquals(message, transmetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Diffusion multi-destinations et déconnexion")
    void testMultiDestinationsEtDeconnexion() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale a = new DestinationFinale();
        DestinationFinale b = new DestinationFinale();
        transmetteur.connecter(a);
        transmetteur.connecter(b);
        Information<Boolean> message = new Information<>(new Boolean[]{true});
        transmetteur.recevoir(message);
        assertEquals(message, a.getInformationRecue());
        assertEquals(message, b.getInformationRecue());
        transmetteur.deconnecter(b);
        transmetteur.deconnecter(b);
        a.recevoir(null);
        b.recevoir(null);
        transmetteur.emettre();
        assertEquals(message, a.getInformationRecue());
        assertNull(b.getInformationRecue());
    }

    @Test
    @DisplayName("Émission avant réception sans destination")
    void testEmettreAvantReceptionSansDestination() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.emettre();
        assertNull(transmetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Émission avant réception avec destination")
    void testEmettreAvantReception() throws Exception {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        DestinationFinale destination = new DestinationFinale();
        transmetteur.connecter(destination);
        transmetteur.emettre();
        assertNull(destination.getInformationRecue());
    }

    @Test
    @DisplayName("Interruption de l'émission en cas d'exception sur une destination")
    void testExceptionDestination() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.connecter(new Destination<Boolean>() {
            public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
                throw new InformationNonConformeException("rejet volontaire");
            }
        });
        DestinationFinale suivante = new DestinationFinale();
        transmetteur.connecter(suivante);
        Information<Boolean> message = new Information<>(new Boolean[]{true});
        assertThrows(InformationNonConformeException.class, () -> transmetteur.recevoir(message));
        assertEquals(message, transmetteur.getInformationEmise());
        assertNull(suivante.getInformationRecue());
    }

    @Test
    @DisplayName("Connexion d'une destination nulle")
    void testDestinationNulle() {
        TransmetteurParfait<Boolean> transmetteur = new TransmetteurParfait<>();
        transmetteur.connecter(null);
        assertThrows(NullPointerException.class, () -> transmetteur.recevoir(new Information<>(new Boolean[]{true})));
    }
}
