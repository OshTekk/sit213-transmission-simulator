package transmetteurs;

import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransmetteurBruiteTest {

    @Test
    void constructeurInitialiseLesInformations() {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0.5f);

        assertNull(transmetteur.getInformationRecue());
        assertNotNull(transmetteur.getInformationEmise());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
    }

    @Test
    void recevoirAvecPuissanceNulleConserveLeSignalEtLeTransmet() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0.0f);
        DestinationMemoire destination = new DestinationMemoire();
        Information<Float> signal = new Information<>(new Float[]{-1.0f, 0.0f, 1.0f});
        transmetteur.connecter(destination);

        transmetteur.recevoir(signal);

        assertSame(signal, transmetteur.getInformationRecue());
        assertEquals(signal, transmetteur.getInformationEmise());
        assertEquals(signal, destination.getInformationRecue());
    }

    @Test
    void recevoirAvecBruitConserveLeNombreDEchantillonsEtProduitDesValeursFinies() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(1.0f);
        Information<Float> signal = new Information<>(new Float[]{0.0f, 0.0f, 0.0f, 0.0f});

        transmetteur.recevoir(signal);

        Information<Float> signalBruit = transmetteur.getInformationEmise();
        assertEquals(signal.nbElements(), signalBruit.nbElements());
        for (Float echantillon : signalBruit) {
            assertTrue(Float.isFinite(echantillon));
        }
    }

    @Test
    void recevoirNullLeveUneExceptionSansEcraserLesInformationsPrecedentes() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0.0f);
        Information<Float> signal = new Information<>(new Float[]{0.5f});
        transmetteur.recevoir(signal);

        assertThrows(InformationNonConformeException.class, () -> transmetteur.recevoir(null));

        assertSame(signal, transmetteur.getInformationRecue());
        assertEquals(signal, transmetteur.getInformationEmise());
    }

    @Test
    void emettreEnvoieLeDernierSignalAuxDestinationsConnectees() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0.0f);
        DestinationMemoire premiereDestination = new DestinationMemoire();
        DestinationMemoire secondeDestination = new DestinationMemoire();
        Information<Float> signal = new Information<>(new Float[]{0.25f, -0.25f});
        transmetteur.connecter(premiereDestination);
        transmetteur.connecter(secondeDestination);

        transmetteur.recevoir(signal);

        assertEquals(signal, premiereDestination.getInformationRecue());
        assertEquals(signal, secondeDestination.getInformationRecue());
    }

    @Test
    void emettrePropageLExceptionDUneDestination() {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0.0f);
        transmetteur.connecter(new Destination<Float>() {
            @Override
            public void recevoir(Information<Float> information) throws InformationNonConformeException {
                throw new InformationNonConformeException("rejet volontaire du test");
            }
        });

        assertThrows(InformationNonConformeException.class,
                () -> transmetteur.recevoir(new Information<>(new Float[]{1.0f})));
    }

    @Test
    void deuxTransmetteursAvecMemeGraineGenerentLeMemeBruit() throws Exception {
        TransmetteurBruite transmetteur1 = new TransmetteurBruite(0.5f, 42);
        TransmetteurBruite transmetteur2 = new TransmetteurBruite(0.5f, 42);
        Information<Float> signal = new Information<>(new Float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f});

        transmetteur1.recevoir(signal);
        transmetteur2.recevoir(signal);

        assertEquals(transmetteur1.getInformationEmise(), transmetteur2.getInformationEmise());
    }

    /** Destination de test qui mémorise la dernière information reçue. */
    private static class DestinationMemoire extends Destination<Float> {
        @Override
        public void recevoir(Information<Float> information) {
            this.informationRecue = information;
        }
    }
}
