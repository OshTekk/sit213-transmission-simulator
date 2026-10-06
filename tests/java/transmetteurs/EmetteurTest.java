package transmetteurs;

import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmetteurTest {

    private CodeBinaire code;
    private Emetteur emetteur;

    private static class DestinationFloat extends Destination<Float> {
        @Override
        public void recevoir(Information<Float> information) throws InformationNonConformeException {
            this.informationRecue = information;
        }
    }

    @BeforeEach
    void setUp() {
        code = new CodeNRZ(30, 0.0f, 1.0f);
        emetteur = new Emetteur(code);
    }

    @Test
    @DisplayName("État initial de l'émetteur et conservation du codeur")
    void testEtatInitial() {
        assertSame(code, emetteur.getCodeBinaire());
        assertNull(emetteur.getInformationRecue());
        assertNull(emetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Transmission du signal encodé aux destinations connectées")
    void testTransmissionDestinations() throws Exception {
        DestinationFloat destination = new DestinationFloat();
        emetteur.connecter(destination);

        Information<Boolean> bits = new Information<>(new Boolean[]{true, false});
        emetteur.recevoir(bits);

        assertEquals(bits, emetteur.getInformationRecue());
        assertNotNull(emetteur.getInformationEmise());
        assertEquals(60, emetteur.getInformationEmise().nbElements());
        assertEquals(emetteur.getInformationEmise(), destination.getInformationRecue());
    }

    @Test
    @DisplayName("Traitement d'une information vide")
    void testInformationVide() throws Exception {
        DestinationFloat destination = new DestinationFloat();
        emetteur.connecter(destination);

        emetteur.recevoir(new Information<>());
        assertNotNull(emetteur.getInformationEmise());
        assertEquals(0, emetteur.getInformationEmise().nbElements());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    @Test
    @DisplayName("Rejet d'une information reçue nulle")
    void testRejetInformationNulle() throws Exception {
        Information<Boolean> bits = new Information<>(new Boolean[]{true});
        emetteur.recevoir(bits);

        assertThrows(InformationNonConformeException.class, () -> emetteur.recevoir(null));
        assertEquals(bits, emetteur.getInformationRecue());
        assertNotNull(emetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Diffusion multi-destinations et déconnexion")
    void testMultiDestinationsEtDeconnexion() throws Exception {
        DestinationFloat d1 = new DestinationFloat();
        DestinationFloat d2 = new DestinationFloat();
        emetteur.connecter(d1);
        emetteur.connecter(d2);

        Information<Boolean> bits1 = new Information<>(new Boolean[]{true});
        emetteur.recevoir(bits1);
        assertEquals(emetteur.getInformationEmise(), d1.getInformationRecue());
        assertEquals(emetteur.getInformationEmise(), d2.getInformationRecue());

        emetteur.deconnecter(d2);
        Information<Boolean> bits2 = new Information<>(new Boolean[]{false});
        emetteur.recevoir(bits2);
        assertEquals(emetteur.getInformationEmise(), d1.getInformationRecue());
        assertNotEquals(emetteur.getInformationEmise(), d2.getInformationRecue());
    }

    @Test
    @DisplayName("Émission avant réception sans destination")
    void testEmettreAvantReception() throws Exception {
        emetteur.emettre();
        assertNull(emetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Interruption de l'émission en cas d'exception sur une destination")
    void testExceptionDestination() {
        emetteur.connecter(new Destination<Float>() {
            @Override
            public void recevoir(Information<Float> information) throws InformationNonConformeException {
                throw new InformationNonConformeException("Rejet volontaire pour test");
            }
        });
        DestinationFloat destinationSuivante = new DestinationFloat();
        emetteur.connecter(destinationSuivante);

        assertThrows(InformationNonConformeException.class,
                () -> emetteur.recevoir(new Information<>(new Boolean[]{true})));
        assertNull(destinationSuivante.getInformationRecue());
    }
}
