package emetteurs;

import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import transmetteurs.Emetteur;

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

    /** Vérifie que l'émetteur conserve son codeur et démarre sans information reçue ni émise. */
    @Test
    void constructeurInitialiseLesInformationsANullEtConserveLeCodeBinaire() {
        assertSame(code, emetteur.getCodeBinaire());
        assertNull(emetteur.getInformationRecue());
        assertNull(emetteur.getInformationEmise());
    }

    /** Vérifie que les bits reçus sont encodés puis transmis à la destination. */
    @Test
    void recevoirTransmetAuxDestinationsConnectees() throws Exception {
        DestinationFloat destination = new DestinationFloat();
        emetteur.connecter(destination);

        Information<Boolean> bits = new Information<>(new Boolean[]{true, false});
        emetteur.recevoir(bits);

        assertEquals(bits, emetteur.getInformationRecue());
        assertNotNull(emetteur.getInformationEmise());
        assertEquals(60, emetteur.getInformationEmise().nbElements());
        assertEquals(emetteur.getInformationEmise(), destination.getInformationRecue());
    }

    /** Vérifie qu'une information vide produit et transmet une émission vide. */
    @Test
    void recevoirInformationVideProduitEmissionVide() throws Exception {
        DestinationFloat destination = new DestinationFloat();
        emetteur.connecter(destination);

        emetteur.recevoir(new Information<>());
        assertNotNull(emetteur.getInformationEmise());
        assertEquals(0, emetteur.getInformationEmise().nbElements());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    /** Vérifie qu'une réception nulle est rejetée sans écraser le message précédent. */
    @Test
    void recevoirNullRejetteAvecExceptionSansEcraserPrecedent() throws Exception {
        Information<Boolean> bits = new Information<>(new Boolean[]{true});
        emetteur.recevoir(bits);

        assertThrows(InformationNonConformeException.class, () -> emetteur.recevoir(null));
        assertEquals(bits, emetteur.getInformationRecue());
        assertNotNull(emetteur.getInformationEmise());
    }

    /** Vérifie l'envoi aux destinations connectées et l'effet d'une déconnexion. */
    @Test
    void emettreAToutesLesDestinationsEtDeconnecter() throws Exception {
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

    /** Vérifie qu'émettre avant une réception et sans destination ne modifie pas l'état. */
    @Test
    void emettreAvantReceptionSansDestinationNeFaitRien() throws Exception {
        emetteur.emettre();
        assertNull(emetteur.getInformationEmise());
    }

    /** Vérifie que le rejet d'une destination interrompt l'envoi aux suivantes. */
    @Test
    void exceptionDestinationInterromptLEmission() {
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
