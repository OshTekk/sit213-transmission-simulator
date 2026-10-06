package transmetteurs;

import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransmetteurBruiteTest {
    static int NBECHPARBIT_COMMUN = 30;
    static int SEMENCE_COMMUNE = 42;
    static List<Trajet> MULTI_TRAJET_NUL = List.of();

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 42, -7})
    @DisplayName("Initialisation avec ou sans graine aléatoire")
    void testConstructeurSemence(Integer semence) throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, semence);

        assertNull(transmetteur.getInformationRecue());
        assertNotNull(transmetteur.getInformationEmise());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
        assertTrue(transmetteur.destinationsConnectees.isEmpty());

        transmetteur.recevoir(new Information<>(new Float[]{-1f, 0f, 1f}));
        assertEquals(3, transmetteur.getInformationEmise().nbElements());
        for (float valeur : transmetteur.getInformationEmise()) {
            assertTrue(Float.isFinite(valeur));
        }
    }

    @Test
    @DisplayName("Signal d'amplitude nulle transmis sans bruit")
    void testSignalNul() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        DestinationMemoire destination = new DestinationMemoire();
        Information<Float> signal = new Information<>(new Float[]{0f, 0f, 0f});
        transmetteur.connecter(destination);

        transmetteur.recevoir(signal);

        assertSame(signal, transmetteur.getInformationRecue());
        assertNotSame(signal, transmetteur.getInformationEmise());
        assertEquals(signal, transmetteur.getInformationEmise());
        assertSame(transmetteur.getInformationEmise(), destination.getInformationRecue());
        assertEquals(1, destination.receptions);
    }

    @Test
    @DisplayName("Ajout du bruit sans altération du signal initial")
    void testAjoutBruit() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0f, 2, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        Information<Float> signal = new Information<>(new Float[]{-1f, 0f, 1f, 2f});

        transmetteur.recevoir(signal);

        Information<Float> emission = transmetteur.getInformationEmise();
        assertSame(signal, transmetteur.getInformationRecue());
        assertNotSame(signal, emission);
        assertEquals(4, emission.nbElements());
        assertNotEquals(signal, emission, "0 dB ne signifie pas une puissance de bruit nulle");
        assertEquals(new Information<>(new Float[]{-1f, 0f, 1f, 2f}), signal);
        for (float valeur : emission) {
            assertTrue(Float.isFinite(valeur));
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 2, 1, 5", "10, 2, 1, 0.5", "-10, 2, 1, 50",
            "0, 8, 1, 20", "0, 2, 2, 20"})
    @DisplayName("Vérification statistique de la variance et de la moyenne nulle du bruit")
    void testStatistiquesBruit(float snrDb, int nbEchParBit, float amplitude, double varianceAttendue) throws Exception {
        int taille = 20_000;
        Information<Float> signal = new Information<>();
        for (int i = 0; i < taille; i++) {
            signal.add((i % 2 == 0 ? -1f : 3f) * amplitude);
        }
        TransmetteurBruite transmetteur = new TransmetteurBruite(snrDb, nbEchParBit, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        transmetteur.recevoir(signal);

        assertEquals(taille, transmetteur.getInformationEmise().nbElements());
        Iterator<Float> entrees = signal.iterator();
        double somme = 0;
        double sommeCarres = 0;
        for (float sortie : transmetteur.getInformationEmise()) {
            assertTrue(Float.isFinite(sortie));
            double bruit = (double) sortie - entrees.next();
            somme += bruit;
            sommeCarres += bruit * bruit;
        }
        double moyenne = somme / taille;
        double variance = sommeCarres / taille - moyenne * moyenne;

        assertEquals(0.0, moyenne, 6 * Math.sqrt(varianceAttendue / taille));
        assertEquals(varianceAttendue, variance, varianceAttendue * 0.05);
    }

    @Test
    @DisplayName("Réception d'une information vide")
    void testInformationVide() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        DestinationMemoire destination = new DestinationMemoire();
        transmetteur.connecter(destination);
        transmetteur.recevoir(new Information<>(new Float[]{1f, -1f}));
        Information<Float> ancienneEmission = transmetteur.getInformationEmise();
        Information<Float> vide = new Information<>();

        transmetteur.recevoir(vide);

        assertSame(vide, transmetteur.getInformationRecue());
        assertNotSame(ancienneEmission, transmetteur.getInformationEmise());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
        assertSame(transmetteur.getInformationEmise(), destination.getInformationRecue());
        assertEquals(2, destination.receptions);
    }

    @Test
    @DisplayName("Rejet d'une information reçue nulle")
    void testRejetInformationNulle() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        DestinationMemoire destination = new DestinationMemoire();
        transmetteur.connecter(destination);
        Information<Float> signal = new Information<>(new Float[]{0.5f});
        transmetteur.recevoir(signal);
        Information<Float> emission = transmetteur.getInformationEmise();

        assertThrows(InformationNonConformeException.class, () -> transmetteur.recevoir(null));

        assertSame(signal, transmetteur.getInformationRecue());
        assertSame(emission, transmetteur.getInformationEmise());
        assertSame(emission, destination.getInformationRecue());
        assertEquals(1, destination.receptions);
    }

    @Test
    @DisplayName("Échec sur un élément de signal nul")
    void testElementNull() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        DestinationMemoire destination = new DestinationMemoire();
        transmetteur.connecter(destination);
        transmetteur.recevoir(new Information<>(new Float[]{1f}));
        Information<Float> emissionPrecedente = transmetteur.getInformationEmise();
        Information<Float> invalide = new Information<>(new Float[]{1f, null});

        assertThrows(NullPointerException.class, () -> transmetteur.recevoir(invalide));

        assertSame(invalide, transmetteur.getInformationRecue());
        assertNotSame(emissionPrecedente, transmetteur.getInformationEmise());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
        assertSame(emissionPrecedente, destination.getInformationRecue());
        assertEquals(1, destination.receptions);
    }

    @Test
    @DisplayName("Recalcul de la puissance à chaque réception")
    void testRecalculPuissance() throws Exception {
        TransmetteurBruite reference = new TransmetteurBruite(0f, 2, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        TransmetteurBruite amplifie = new TransmetteurBruite(0f, 2, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        Information<Float> premier = new Information<>(new Float[]{-1f, 3f});
        reference.recevoir(premier);
        amplifie.recevoir(premier);
        Information<Float> ancienneEmission = amplifie.getInformationEmise();
        Information<Float> second = new Information<>(new Float[]{-3f, 9f, -3f, 9f});

        reference.recevoir(new Information<>(new Float[]{-1f, 3f, -1f, 3f}));
        amplifie.recevoir(second);

        assertSame(second, amplifie.getInformationRecue());
        assertNotSame(ancienneEmission, amplifie.getInformationEmise());
        assertEquals(2, ancienneEmission.nbElements());
        assertEquals(4, amplifie.getInformationEmise().nbElements());
        Iterator<Float> valeursReference = reference.getInformationEmise().iterator();
        for (float valeur : amplifie.getInformationEmise()) {
            assertEquals(3 * valeursReference.next(), valeur, 1e-5f);
        }
    }

    @Test
    @DisplayName("Reproductibilité du bruit généré avec une graine identique")
    void testReproductibiliteMemeGraine() throws Exception {
        TransmetteurBruite premier = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        TransmetteurBruite second = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        Information<Float> signal = new Information<>(new Float[]{-1f, 1f, -1f, 1f});

        premier.recevoir(signal);
        second.recevoir(signal);
        assertEquals(premier.getInformationEmise(), second.getInformationEmise());
        Information<Float> premiereEmission = premier.getInformationEmise();

        premier.recevoir(signal);
        second.recevoir(signal);
        assertEquals(premier.getInformationEmise(), second.getInformationEmise());
        assertNotEquals(premiereEmission, premier.getInformationEmise(),
                "La séquence aléatoire doit avancer entre deux réceptions");
    }

    @Test
    @DisplayName("Génération de bruits différents avec des graines différentes")
    void testGrainesDifferentes() throws Exception {
        TransmetteurBruite premier = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        TransmetteurBruite second = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE + 1);
        Information<Float> signal = new Information<>(new Float[]{-1f, 1f, -1f, 1f});

        premier.recevoir(signal);
        second.recevoir(signal);

        assertNotEquals(premier.getInformationEmise(), second.getInformationEmise());
    }

    @Test
    @DisplayName("Émission vers les destinations sans recalculer le bruit")
    void testEmettreConserveSignal() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        DestinationMemoire premiere = new DestinationMemoire();
        DestinationMemoire seconde = new DestinationMemoire();
        transmetteur.connecter(premiere);
        transmetteur.connecter(seconde);
        transmetteur.recevoir(new Information<>(new Float[]{0.25f, -0.25f}));
        Information<Float> emission = transmetteur.getInformationEmise();

        assertSame(emission, premiere.getInformationRecue());
        assertSame(emission, seconde.getInformationRecue());
        assertEquals(1, premiere.receptions);
        assertEquals(1, seconde.receptions);

        transmetteur.emettre();

        assertSame(emission, transmetteur.getInformationEmise());
        assertSame(emission, premiere.getInformationRecue());
        assertSame(emission, seconde.getInformationRecue());
        assertEquals(2, premiere.receptions);
        assertEquals(2, seconde.receptions);
    }

    @Test
    @DisplayName("Émission avant réception")
    void testEmettreAvantReception() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        assertDoesNotThrow(transmetteur::emettre);
        DestinationMemoire destination = new DestinationMemoire();
        transmetteur.connecter(destination);

        transmetteur.emettre();

        assertSame(transmetteur.getInformationEmise(), destination.getInformationRecue());
        assertEquals(0, destination.getInformationRecue().nbElements());
        assertEquals(1, destination.receptions);
        assertNull(transmetteur.getInformationRecue());
    }

    @Test
    @DisplayName("Interruption de l'émission en cas d'exception sur une destination")
    void testExceptionDestination() {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        DestinationMemoire avant = new DestinationMemoire();
        DestinationMemoire apres = new DestinationMemoire();
        InformationNonConformeException rejet = new InformationNonConformeException("rejet du test");
        transmetteur.connecter(avant);
        transmetteur.connecter(new Destination<Float>() {
            @Override
            public void recevoir(Information<Float> information) throws InformationNonConformeException {
                throw rejet;
            }
        });
        transmetteur.connecter(apres);

        assertSame(rejet, assertThrows(InformationNonConformeException.class,
                () -> transmetteur.recevoir(new Information<>(new Float[]{1f}))));
        assertSame(transmetteur.getInformationEmise(), avant.getInformationRecue());
        assertEquals(1, avant.receptions);
        assertEquals(0, apres.receptions);
        assertNull(apres.getInformationRecue());
        assertSame(rejet, assertThrows(InformationNonConformeException.class, transmetteur::emettre));
    }

    @Test
    @DisplayName("Comportement avec un SNR NaN")
    void testSnrNan() throws Exception {
        TransmetteurBruite transmetteur = assertDoesNotThrow(
                () -> new TransmetteurBruite(Float.NaN, 2, MULTI_TRAJET_NUL, SEMENCE_COMMUNE));

        transmetteur.recevoir(new Information<>(new Float[]{-1f, 1f}));

        assertEquals(2, transmetteur.getInformationEmise().nbElements());
        for (float valeur : transmetteur.getInformationEmise()) {
            assertTrue(Float.isNaN(valeur));
        }
    }

    @Test
    @DisplayName("Décalage et atténuation d'un trajet indirect sans bruit")
    void testTrajetUniqueSansBruit() throws Exception {
        List<Trajet> trajets = List.of(new Trajet(2, 0.5f));
        TransmetteurBruite transmetteur = new TransmetteurBruite(null, NBECHPARBIT_COMMUN, trajets, null);
        Information<Float> signal = new Information<>(new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f});

        transmetteur.recevoir(signal);

        Information<Float> emission = transmetteur.getInformationEmise();
        assertEquals(5, emission.nbElements());
        assertEquals(1.0f, emission.iemeElement(0), 1e-6f);
        assertEquals(2.0f, emission.iemeElement(1), 1e-6f);
        assertEquals(3.0f + 0.5f * 1.0f, emission.iemeElement(2), 1e-6f);
        assertEquals(4.0f + 0.5f * 2.0f, emission.iemeElement(3), 1e-6f);
        assertEquals(5.0f + 0.5f * 3.0f, emission.iemeElement(4), 1e-6f);
    }

    @Test
    @DisplayName("Superposition additive de plusieurs trajets indirects")
    void testPlusieursTrajets() throws Exception {
        List<Trajet> trajets = List.of(new Trajet(1, 0.5f), new Trajet(2, 0.25f));
        TransmetteurBruite transmetteur = new TransmetteurBruite(null, NBECHPARBIT_COMMUN, trajets, null);
        Information<Float> signal = new Information<>(new Float[]{2.0f, 4.0f, 6.0f, 8.0f});

        transmetteur.recevoir(signal);

        Information<Float> emission = transmetteur.getInformationEmise();
        assertEquals(4, emission.nbElements());
        assertEquals(2.0f, emission.iemeElement(0), 1e-6f);
        assertEquals(4.0f + 0.5f * 2.0f, emission.iemeElement(1), 1e-6f);
        assertEquals(6.0f + 0.5f * 4.0f + 0.25f * 2.0f, emission.iemeElement(2), 1e-6f);
        assertEquals(8.0f + 0.5f * 6.0f + 0.25f * 4.0f, emission.iemeElement(3), 1e-6f);
    }

    @Test
    @DisplayName("Trajet indirect avec atténuation nulle")
    void testTrajetAttenuationNulle() throws Exception {
        List<Trajet> trajets = List.of(new Trajet(1, 0.0f));
        TransmetteurBruite transmetteur = new TransmetteurBruite(null, NBECHPARBIT_COMMUN, trajets, null);
        Information<Float> signal = new Information<>(new Float[]{1.0f, -2.0f, 3.5f});

        transmetteur.recevoir(signal);

        assertEquals(signal, transmetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Trajet indirect avec retard supérieur à la taille du signal")
    void testTrajetRetardSuperieurTaille() throws Exception {
        List<Trajet> trajets = List.of(new Trajet(10, 0.8f));
        TransmetteurBruite transmetteur = new TransmetteurBruite(null, NBECHPARBIT_COMMUN, trajets, null);
        Information<Float> signal = new Information<>(new Float[]{1.0f, 2.0f, 3.0f});

        transmetteur.recevoir(signal);

        assertEquals(signal, transmetteur.getInformationEmise());
    }

    @Test
    @DisplayName("Linéarité et superposition du bruit gaussien et des trajets indirects")
    void testTrajetsEtBruitCombines() throws Exception {
        List<Trajet> trajets = List.of(new Trajet(1, 0.5f));
        TransmetteurBruite sansTrajets = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, MULTI_TRAJET_NUL, SEMENCE_COMMUNE);
        TransmetteurBruite avecTrajets = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, trajets, SEMENCE_COMMUNE);
        Information<Float> signal = new Information<>(new Float[]{2.0f, 4.0f, 6.0f});

        sansTrajets.recevoir(signal);
        avecTrajets.recevoir(signal);

        Information<Float> sortieSans = sansTrajets.getInformationEmise();
        Information<Float> sortieAvec = avecTrajets.getInformationEmise();

        assertEquals(0.0f, sortieAvec.iemeElement(0) - sortieSans.iemeElement(0), 1e-6f);
        assertEquals(0.5f * 2.0f, sortieAvec.iemeElement(1) - sortieSans.iemeElement(1), 1e-6f);
        assertEquals(0.5f * 4.0f, sortieAvec.iemeElement(2) - sortieSans.iemeElement(2), 1e-6f);
    }

    @Test
    @DisplayName("Initialisation et tolérance d'une liste de trajets nulle")
    void testConstructeurTrajets() {
        assertDoesNotThrow(() -> new TransmetteurBruite(10f, 30, List.of(new Trajet(2, 0.5f)), null));
        assertDoesNotThrow(() -> new TransmetteurBruite(10f, 30, null, null));
    }

    private static class DestinationMemoire extends Destination<Float> {
        private int receptions;

        @Override
        public void recevoir(Information<Float> information) {
            this.informationRecue = information;
            receptions++;
        }
    }
}
