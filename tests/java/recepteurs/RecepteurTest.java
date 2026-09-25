package recepteurs;

import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class RecepteurTest {

    private CodeBinaire code;
    private Recepteur recepteur;

    private static class DestinationBoolean extends Destination<Boolean> {
        @Override
        public void recevoir(Information<Boolean> information)
                throws InformationNonConformeException {
            this.informationRecue = information;
        }
    }

    @BeforeEach
    void initialiser() {
        code = new CodeNRZ(30, 0.0f, 1.0f);
        recepteur = new Recepteur(code);
    }

    /** Vérifie que le récepteur conserve son codeur et démarre sans information reçue ni émise. */
    @Test
    void constructeurInitialiseLesInformationsANullEtConserveLeCodeBinaire() {
        assertSame(code, recepteur.getCodeBinaire());
        assertNull(recepteur.getInformationRecue());
        assertNull(recepteur.getInformationEmise());
    }

    /** Vérifie que le signal reçu est décodé en bits puis transmis à la destination. */
    @Test
    void recevoirTransmetAuxDestinationsConnectees() throws Exception {
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

    /** Vérifie qu'une information vide produit et transmet une émission vide. */
    @Test
    void recevoirInformationVideProduitEmissionVide() throws Exception {
        DestinationBoolean destination = new DestinationBoolean();
        recepteur.connecter(destination);

        recepteur.recevoir(new Information<>());

        assertNotNull(recepteur.getInformationEmise());
        assertEquals(0, recepteur.getInformationEmise().nbElements());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    /** Vérifie qu'une réception nulle est rejetée sans écraser le message précédent. */
    @Test
    void recevoirNullRejetteAvecExceptionSansEcraserPrecedent() throws Exception {
        Information<Boolean> bits = new Information<>(new Boolean[]{true});
        Information<Float> signal = code.encoder(bits);
        recepteur.recevoir(signal);
        assertThrows(InformationNonConformeException.class, () -> recepteur.recevoir(null));
        assertEquals(signal, recepteur.getInformationRecue());
        assertEquals(bits, recepteur.getInformationEmise());
    }

    /** Vérifie l'envoi aux destinations connectées et l'effet d'une déconnexion. */
    @Test
    void emettreAToutesLesDestinationsEtDeconnecter() throws Exception {
        DestinationBoolean d1 = new DestinationBoolean();
        DestinationBoolean d2 = new DestinationBoolean();

        recepteur.connecter(d1);
        recepteur.connecter(d2);

        Information<Boolean> bits1 = new Information<>(new Boolean[]{true});

        recepteur.recevoir(code.encoder(bits1));

        assertEquals(recepteur.getInformationEmise(),d1.getInformationRecue());
        assertEquals(recepteur.getInformationEmise(), d2.getInformationRecue());

        recepteur.deconnecter(d2);
        Information<Boolean> bits2 = new Information<>(new Boolean[]{false});
        recepteur.recevoir(code.encoder(bits2));

        assertEquals(recepteur.getInformationEmise(), d1.getInformationRecue());
        assertNotEquals(recepteur.getInformationEmise(), d2.getInformationRecue());
    }

    /** Vérifie qu'émettre avant une réception et sans destination ne modifie pas l'état. */
    @Test
    void emettreAvantReceptionSansDestinationNeFaitRien() throws Exception {
        recepteur.emettre();
        assertNull(recepteur.getInformationEmise());
    }

    /** Vérifie que le rejet d'une destination interrompt l'envoi aux suivantes. */
    @Test
    void exceptionDestinationInterromptLEmission() throws Exception {
        recepteur.connecter(new Destination<Boolean>() {
            @Override
            public void recevoir(Information<Boolean> information)
                    throws InformationNonConformeException {
                throw new InformationNonConformeException(
                        "Rejet volontaire pour test"
                );
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