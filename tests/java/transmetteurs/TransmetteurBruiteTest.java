package transmetteurs;

import destinations.Destination;
import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class TransmetteurBruiteTest {
    static int NBECHPARBIT_COMMUN = 30;
    static int SEMENCE_COMMUNE = 42;

    /**
     * Vérifie l'état initial du transmetteur et la production de valeurs finies,
     * qu'une semence soit fournie ou non.
     *
     * @param semence semence du générateur, éventuellement absente
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 42, -7})
    void constructeurInitialiseLesInformationsAvecOuSansSemence(Integer semence) throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, semence);

        assertNull(transmetteur.getInformationRecue());
        assertNotNull(transmetteur.getInformationEmise());
        assertEquals(0, transmetteur.getInformationEmise().nbElements());
        assertTrue(transmetteur.destinationsConnectees.isEmpty());

        // Vérifie aussi le générateur, y compris sans graine imposée.
        transmetteur.recevoir(new Information<>(new Float[]{-1f, 0f, 1f}));
        assertEquals(3, transmetteur.getInformationEmise().nbElements());
        for (float valeur : transmetteur.getInformationEmise()) {
            assertTrue(Float.isFinite(valeur));
        }
    }

    /** Vérifie qu'un signal de puissance nulle est transmis sans bruit à la destination. */
    @Test
    void recevoirSignalNulEnAmplitudeNeProduitPasDeBruitEtLeTransmet() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
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

    /** Vérifie que le bruit modifie l'émission sans altérer l'information reçue. */
    @Test
    void recevoirAjouteDuBruitSansModifierLeSignalInitial() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(0f, 2, SEMENCE_COMMUNE);
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

    /**
     * Vérifie statistiquement que le bruit est centré et que sa variance suit
     * le SNR, le nombre d'échantillons par bit et la puissance du signal.
     *
     * @param snrDb rapport signal sur bruit en décibels
     * @param nbEchParBit nombre d'échantillons par bit
     * @param amplitude facteur d'amplitude du signal
     * @param varianceAttendue variance théorique du bruit
     */
    @ParameterizedTest
    @CsvSource({"0, 2, 1, 5", "10, 2, 1, 0.5", "-10, 2, 1, 50",
            "0, 8, 1, 20", "0, 2, 2, 20"})
    void bruitEstCentreEtSaVarianceRespecteLeSnrEtLaPuissanceDuSignal(
            float snrDb, int nbEchParBit, float amplitude, double varianceAttendue) throws Exception {
        // [-1, 3] : puissance 5, différente de la variance (4).
        // Puissance du bruit : 5 * amplitude² * nbEch / (2 * 10^(SNR/10)).
        // Les références ci-dessus sont indépendantes de la génération du bruit.
        int taille = 20_000;
        Information<Float> signal = new Information<>();
        for (int i = 0; i < taille; i++) {
            signal.add((i % 2 == 0 ? -1f : 3f) * amplitude);
        }
        TransmetteurBruite transmetteur = new TransmetteurBruite(snrDb, nbEchParBit, SEMENCE_COMMUNE);
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

        // Graine fixe, moyenne à six erreurs-types et variance à 5 %.
        assertEquals(0.0, moyenne, 6 * Math.sqrt(varianceAttendue / taille));
        assertEquals(varianceAttendue, variance, varianceAttendue * 0.05);
    }

    /** Vérifie qu'une réception vide remplace l'émission précédente et est propagée. */
    @Test
    void recevoirUneInformationVideEmetUneInformationVide() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
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

    /** Vérifie qu'une réception nulle est rejetée sans modifier ni retransmettre l'état précédent. */
    @Test
    void recevoirNullLeveUneExceptionSansEcraserNiRetransmettreLesInformations() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
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

    /**
     * Caractérise l'échec actuel sur un élément nul : l'émission est vidée,
     * mais la destination conserve l'émission précédente.
     */
    @Test
    void recevoirUnElementNullEchoueActuellementApresRemiseAZeroDeLEmission() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
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

    /** Vérifie que chaque réception remplace l'émission et recalcule le bruit selon la puissance reçue. */
    @Test
    void chaqueReceptionRecalculeLaPuissanceEtRemplaceLEmission() throws Exception {
        TransmetteurBruite reference = new TransmetteurBruite(0f, 2, SEMENCE_COMMUNE);
        TransmetteurBruite amplifie = new TransmetteurBruite(0f, 2, SEMENCE_COMMUNE);
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
            // Amplitude x3 => puissance x9 => écart-type du bruit x3.
            assertEquals(3 * valeursReference.next(), valeur, 1e-5f);
        }
    }

    /** Vérifie qu'une même semence reproduit la suite de bruits sur plusieurs réceptions. */
    @Test
    void deuxTransmetteursAvecMemeGraineGenerentLeMemeBruitSurPlusieursReceptions() throws Exception {
        TransmetteurBruite premier = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
        TransmetteurBruite second = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
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

    /** Vérifie que des semences différentes produisent des émissions différentes. */
    @Test
    void deuxGrainesDifferentesProduisentDesBruitsDifferents() throws Exception {
        TransmetteurBruite premier = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
        TransmetteurBruite second = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE+1);
        Information<Float> signal = new Information<>(new Float[]{-1f, 1f, -1f, 1f});

        premier.recevoir(signal);
        second.recevoir(signal);

        assertNotEquals(premier.getInformationEmise(), second.getInformationEmise());
    }

    /** Vérifie que l'émission est renvoyée aux destinations sans recalculer le bruit. */
    @Test
    void emettreEnvoieLeMemeSignalAuxDestinationsSansRegenererLeBruit() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
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

    /** Vérifie qu'une émission avant toute réception transmet une information vide. */
    @Test
    void emettreAvantReceptionTransmetLEmissionVideEtAccepteAucuneDestination() throws Exception {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
        assertDoesNotThrow(transmetteur::emettre);
        DestinationMemoire destination = new DestinationMemoire();
        transmetteur.connecter(destination);

        transmetteur.emettre();

        assertSame(transmetteur.getInformationEmise(), destination.getInformationRecue());
        assertEquals(0, destination.getInformationRecue().nbElements());
        assertEquals(1, destination.receptions);
        assertNull(transmetteur.getInformationRecue());
    }

    /** Vérifie qu'un rejet par une destination interrompt la propagation et remonte l'exception. */
    @Test
    void emettrePropageLExceptionEtInterromptLesDestinationsSuivantes() {
        TransmetteurBruite transmetteur = new TransmetteurBruite(10f, NBECHPARBIT_COMMUN, SEMENCE_COMMUNE);
        DestinationMemoire avant = new DestinationMemoire();
        DestinationMemoire apres = new DestinationMemoire();
        InformationNonConformeException rejet = new InformationNonConformeException("rejet du test");
        transmetteur.connecter(avant);
        transmetteur.connecter(new Destination<Float>() {
            /** Simule une destination qui rejette l'information pour tester l'arrêt de la propagation. */
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

    /** Caractérise le comportement actuel avec un SNR NaN : les échantillons émis sont NaN. */
    @Test
    void parametresInvalidesProduisentActuellementNaN() throws Exception {
        TransmetteurBruite transmetteur = assertDoesNotThrow(
                () -> new TransmetteurBruite(Float.NaN, 2, SEMENCE_COMMUNE));

        transmetteur.recevoir(new Information<>(new Float[]{-1f, 1f}));

        assertEquals(2, transmetteur.getInformationEmise().nbElements());
        for (float valeur : transmetteur.getInformationEmise()) {
            assertTrue(Float.isNaN(valeur));
        }
    }

    /** Destination qui mémorise la dernière information et compte les réceptions. */
    private static class DestinationMemoire extends Destination<Float> {
        private int receptions;

        /**
         * Mémorise la dernière information reçue et incrémente le compteur de réceptions.
         *
         * @param information information transmise à cette destination
         */
        @Override
        public void recevoir(Information<Float> information) {
            this.informationRecue = information;
            receptions++;
        }
    }
}
