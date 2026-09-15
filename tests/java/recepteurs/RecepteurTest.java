package recepteurs;

import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecepteurTest {

    private static class DestinationBoolean extends Destination<Boolean> {
        @Override
        public void recevoir(Information<Boolean> information)
                throws InformationNonConformeException {
            this.informationRecue = information;
        }
    }

    @Test
    void constructeurInitialiseLesInformationsANullEtConserveLeCodeBinaire() {
        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);

        assertSame(code, recepteur.getCodeBinaire());
        assertNull(recepteur.getInformationRecue());
        assertNull(recepteur.getInformationEmise());
    }

    @Test
    void recevoirTransmetAuxDestinationsConnectees() throws Exception {
        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);
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
    void recevoirInformationVideProduitEmissionVide() throws Exception {
        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);
        DestinationBoolean destination = new DestinationBoolean();
        recepteur.connecter(destination);

        recepteur.recevoir(new Information<>());

        assertNotNull(recepteur.getInformationEmise());
        assertEquals(0, recepteur.getInformationEmise().nbElements());
        assertEquals(0, destination.getInformationRecue().nbElements());
    }

    @Test
    void recevoirNullRejetteAvecExceptionSansEcraserPrecedent() throws Exception {

        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);

        Information<Boolean> bits = new Information<>(new Boolean[]{true});

        Information<Float> signal = code.encoder(bits);

        recepteur.recevoir(signal);

        assertThrows(InformationNonConformeException.class, () -> recepteur.recevoir(null));

        assertEquals(signal, recepteur.getInformationRecue());
        assertEquals(bits, recepteur.getInformationEmise());
    }

    @Test
    void emettreAToutesLesDestinationsEtDeconnecter() throws Exception {
        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);

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

    @Test
    void emettreAvantReceptionSansDestinationNeFaitRien() throws Exception {

        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);
        recepteur.emettre();
        assertNull(recepteur.getInformationEmise());
    }

    @Test
    void exceptionDestinationInterromptLEmission() throws Exception {
        CodeBinaire code = new CodeNRZ(30, 0.0f, 1.0f);
        Recepteur recepteur = new Recepteur(code);

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