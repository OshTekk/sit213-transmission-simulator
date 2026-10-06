package transmetteurs;

import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class RecepteurTest {

    private CodeBinaire code;
    private Recepteur recepteur;

    private static class DestinationBoolean extends Destination<Boolean> {
        @Override
        public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
            this.informationRecue = information;
        }
    }

    @BeforeEach
    void setUp() {
        code = new CodeNRZ(30, 0.0f, 1.0f);
        recepteur = new Recepteur(code);
    }

    @Test
    @DisplayName("État initial du récepteur et conservation du codeur")
    void testEtatInitial() {
        assertSame(code, recepteur.getCodeBinaire());
        assertNull(recepteur.getInformationRecue());
        assertNull(recepteur.getInformationEmise());
    }

    @Test
    @DisplayName("Décodage et transmission du signal aux destinations connectées")
    void testTransmissionDestinations() throws Exception {
        DestinationBoolean destination = new DestinationBoolean();
        recepteur.connecter(destination);

        Information<Boolean> bits = new Information<>(new Boolean[]{true, false});
        Information<Float> signal = code.encoder(bits);

        recepteur.recevoir(signal);

        assertEquals(signal, recepteur.getInformationRecue());
        assertNotNull(recepteur.getInformationEmise());
        assertEquals(2, recepteur.getInformationEmise().nbElements());
        assertEquals(bits, recepteur.getInformationEmise());
        assertEquals(recepteur.getInformationEmise(), destination.getInformationRecue());
    }

    @Test
    @DisplayName("Traitement d'une information vide")
    void testInformationVide() throws Exception {
        DestinationBoolean destination = new DestinationBoolean();
        recepteur.connecter(destination);

        recepteur.recevoir(new Information<>());

        assertNotNull(recepteur.getInformationEmise());
        assertEquals(0, recepteur.getInformationEmise().nbElements());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    @Test
    @DisplayName("Rejet d'une information reçue nulle")
    void testRejetInformationNulle() throws Exception {
        Information<Boolean> bits = new Information<>(new Boolean[]{true});
        Information<Float> signal = code.encoder(bits);
        recepteur.recevoir(signal);

        assertThrows(InformationNonConformeException.class, () -> recepteur.recevoir(null));
        assertEquals(signal, recepteur.getInformationRecue());
        assertEquals(bits, recepteur.getInformationEmise());
    }

    @Test
    @DisplayName("Diffusion multi-destinations et déconnexion")
    void testMultiDestinationsEtDeconnexion() throws Exception {
        DestinationBoolean d1 = new DestinationBoolean();
        DestinationBoolean d2 = new DestinationBoolean();

        recepteur.connecter(d1);
        recepteur.connecter(d2);

        Information<Boolean> bits1 = new Information<>(new Boolean[]{true});
        recepteur.recevoir(code.encoder(bits1));

        assertEquals(recepteur.getInformationEmise(), d1.getInformationRecue());
        assertEquals(recepteur.getInformationEmise(), d2.getInformationRecue());

        recepteur.deconnecter(d2);
        Information<Boolean> bits2 = new Information<>(new Boolean[]{false});
        recepteur.recevoir(code.encoder(bits2));

        assertEquals(recepteur.getInformationEmise(), d1.getInformationRecue());
        assertNotEquals(recepteur.getInformationEmise(), d2.getInformationRecue());
    }

    @Test
    @DisplayName("Émission avant réception sans destination")
    void testEmettreAvantReception() throws Exception {
        recepteur.emettre();
        assertNull(recepteur.getInformationEmise());
    }

    @Test
    @DisplayName("Interruption de l'émission en cas d'exception sur une destination")
    void testExceptionDestination() throws Exception {
        recepteur.connecter(new Destination<Boolean>() {
            @Override
            public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
                throw new InformationNonConformeException("Rejet volontaire pour test");
            }
        });

        DestinationBoolean destinationSuivante = new DestinationBoolean();
        recepteur.connecter(destinationSuivante);
        Information<Boolean> bits = new Information<>(new Boolean[]{true});
        Information<Float> signal = code.encoder(bits);
        assertThrows(InformationNonConformeException.class, () -> recepteur.recevoir(signal));
        assertNull(destinationSuivante.getInformationRecue());
    }
}