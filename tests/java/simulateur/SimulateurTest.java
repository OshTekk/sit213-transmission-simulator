package simulateur;

import destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import sources.Source;
import sources.SourceFixe;
import sources.SourceAleatoire;
import transmetteurs.Transmetteur;
import transmetteurs.TransmetteurParfait;
import transmetteurs.TransmetteurBruite;
import transmetteurs.Trajet;
import emetteurs.Emetteur;
import recepteurs.Recepteur;
import visualisations.SondeLogique;
import visualisations.SondeAnalogique;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SimulateurTest {
    // Pas d'accesseurs publics : lecture limitée aux champs nécessaires aux assertions.
    private Object lireChamp(Object objet, Class<?> classe, String nom) throws Exception {
        Field champ = classe.getDeclaredField(nom);
        champ.setAccessible(true);
        return champ.get(objet);
    }

    private void remplacerChamp(Simulateur simulateur, String nom, Object valeur) throws Exception {
        Field champ = Simulateur.class.getDeclaredField(nom);
        champ.setAccessible(true);
        champ.set(simulateur, valeur);
    }

    @SuppressWarnings("unchecked")
    private Source<Boolean> source(Simulateur simulateur) throws Exception {
        return (Source<Boolean>) lireChamp(simulateur, Simulateur.class, "source");
    }

    private DestinationFinale destination(Simulateur simulateur) throws Exception {
        return (DestinationFinale) lireChamp(simulateur, Simulateur.class, "destination");
    }

    /** Vérifie les valeurs par défaut, la source aléatoire et l'état initial des informations. */
    @Test void constructeurParDefaut() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{});
        SourceAleatoire source = assertInstanceOf(SourceAleatoire.class, source(simulateur));
        assertEquals(100, source.getNbBits());
        assertNull(source.getSeed());
        assertNull(source.getInformationEmise());
        assertNull(destination(simulateur).getInformationRecue());
        assertEquals(false, lireChamp(simulateur, Simulateur.class, "affichage"));
    }

    /** Vérifie que l'option d'affichage relie une sonde à la source et au transmetteur. */
    @Test void constructeurAvecSondesConnecteDeuxSondes() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-s", "-mess", "1010101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        Object transmetteur = assertInstanceOf(Transmetteur.class, sortiesSource.get(0));
        List<?> sortiesTransmetteur = (List<?>) lireChamp(transmetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(2, sortiesSource.size());
        assertEquals(2, sortiesTransmetteur.size());
        assertInstanceOf(visualisations.SondeLogique.class, sortiesSource.get(1));
        assertInstanceOf(visualisations.SondeLogique.class, sortiesTransmetteur.get(1));
    }

    /** Vérifie qu'un message binaire fixé arrive inchangé à la destination et donne un TEB nul. */
    @ParameterizedTest @ValueSource(strings = {"0000000", "1111111", "01010101"})
    void executeTransmetLeMessageFixe(String message) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", message});
        assertInstanceOf(SourceFixe.class, source(simulateur));
        simulateur.execute();
        Information<Boolean> recu = destination(simulateur).getInformationRecue();
        assertNotNull(recu);
        assertEquals(message.length(), recu.nbElements());
        for (int i = 0; i < message.length(); i++) assertEquals(message.charAt(i) == '1', recu.iemeElement(i));
        assertEquals(source(simulateur).getInformationEmise(), recu);
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
    }

    /** Vérifie qu'un argument de message court est interprété comme un nombre de bits aléatoires. */
    @ParameterizedTest @ValueSource(strings = {"1", "000001", "100", "111111", "999999"})
    void argumentMessageCourtDesigneUneLongueur(String longueur) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", longueur, "-seed", "42"});
        assertInstanceOf(SourceAleatoire.class, source(simulateur));
        simulateur.execute();
        // Ne pas calculer le TEB sur 999999 éléments : Information utilise une LinkedList.
        assertEquals(Integer.parseInt(longueur), destination(simulateur).getInformationRecue().nbElements());
        assertSame(source(simulateur).getInformationEmise(), destination(simulateur).getInformationRecue());
    }

    /** Vérifie que les valeurs invalides de l'option message sont rejetées. */
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.5", "abcdefg", "1000002", "", " 7"})
    void argumentMessageInvalideEstRejete(String message) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess", message}));
    }

    /** Vérifie que les valeurs invalides de la semence sont rejetées. */
    @ParameterizedTest @ValueSource(strings = {"x", "1.5", "2147483648", "", " 42"})
    void argumentGraineInvalideEstRejete(String graine) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-seed", graine}));
    }

    /** Vérifie que les options inconnues sont refusées. */
    @ParameterizedTest @ValueSource(strings = {"-inconnu", "", "-S"})
    void optionInconnueEstRejetee(String option) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{option}));
    }

    /** Vérifie que les semences entières, y compris négatives et extrêmes, sont acceptées. */
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "2147483647", "-2147483648"})
    void graineEntiereEstAcceptee(String graine) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", graine});
        assertEquals(Integer.valueOf(graine), ((SourceAleatoire) source(simulateur)).getSeed());
    }

    /** Vérifie qu'une même semence reproduit le résultat quel que soit l'ordre des options. */
    @Test void graineEtOrdreDesOptionsReproduisentLaSimulation() throws Exception {
        Simulateur a = new Simulateur(new String[]{"-mess", "20", "-seed", "42"});
        Simulateur b = new Simulateur(new String[]{"-seed", "42", "-mess", "20"});
        a.execute();
        b.execute();
        assertEquals(destination(a).getInformationRecue(), destination(b).getInformationRecue());
        assertEquals(0.0f, a.calculTauxErreurBinaire());
    }

    /** Vérifie que la semence ne modifie pas un message fixe. */
    @Test void graineAvecMessageFixeNeChangePasLeMessage() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", "42", "-mess", "1010101"});
        simulateur.execute();
        assertInstanceOf(SourceFixe.class, source(simulateur));
        assertEquals(7, destination(simulateur).getInformationRecue().nbElements());
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
    }

    // Régressions attendues en échec sur etape_1 : ne pas adapter l'attente au défaut.
    /** Vérifie qu'une option message sans valeur lève une exception d'arguments. */
    @Test void messageAbsentDoitLeverArgumentsException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess"}));
    }

    /** Vérifie qu'une option semence sans valeur lève une exception d'arguments. */
    @Test void graineAbsenteDoitLeverArgumentsException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-seed"}));
    }

    /** Vérifie qu'une virgule dans un message binaire est refusée. */
    @Test void virguleDansMessageDoitEtreRejetee() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess", "101,010"}));
    }

    /** Caractérise la priorité actuelle de la dernière valeur lorsqu'une option est répétée. */
    @Test void derniereOptionRepeteeEstActuellementPrioritaire() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", "10", "-mess", "12", "-seed", "1", "-seed", "2"});
        SourceAleatoire source = (SourceAleatoire) source(simulateur);
        assertEquals(12, source.getNbBits());
        assertEquals(2, source.getSeed());
    }

    /** Caractérise les exceptions actuelles provoquées par un tableau ou un argument nul. */
    @Test void argumentsNulsProvoquentActuellementUneException() {
        assertThrows(NullPointerException.class, () -> new Simulateur(null));
        assertThrows(NullPointerException.class, () -> new Simulateur(new String[]{null}));
    }

    /** Vérifie que deux exécutions conservent le même message généré. */
    @Test void executeDeuxFoisConserveLeMessage() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", "42"});
        simulateur.execute();
        Information<Boolean> premier = destination(simulateur).getInformationRecue();
        simulateur.execute();
        assertEquals(premier, destination(simulateur).getInformationRecue());
    }

    /** Vérifie que l'exécution propage une exception émise par la source. */
    @Test void executePropageUneExceptionDeSource() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{});
        remplacerChamp(simulateur, "source", new Source<Boolean>() {
            @Override public void emettre() throws InformationNonConformeException {
                throw new InformationNonConformeException("rejet volontaire du test");
            }
        });
        assertThrows(InformationNonConformeException.class, simulateur::execute);
    }

    // Injection de messages indépendants : un canal parfait seul ne peut produire un TEB non nul.
    private Simulateur preparerCalcul(String emis, String recu) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{});
        SourceFixe source = new SourceFixe(emis);
        source.emettre();
        remplacerChamp(simulateur, "source", source);
        if (recu != null) {
            SourceFixe sourceRecue = new SourceFixe(recu);
            sourceRecue.emettre();
            destination(simulateur).recevoir(sourceRecue.getInformationEmise());
        }
        return simulateur;
    }

    /** Vérifie que le TEB tient compte des bits erronés et des différences de longueur. */
    @ParameterizedTest
    @CsvSource({"0101,0101,0.0", "0101,0111,0.25", "0101,1010,1.0", "0101,01,0.5", "01,010,0.5"})
    void calculTEBCompareLesBitsEtLesLongueurs(String emis, String recu, float attendu) throws Exception {
        assertEquals(attendu, preparerCalcul(emis, recu).calculTauxErreurBinaire(), 0.000001f);
    }

    /** Caractérise le TEB nul renvoyé avant toute exécution. */
    @Test void calculAvantExecutionRetourneActuellementZero() throws Exception {
        assertEquals(0.0f, new Simulateur(new String[]{}).calculTauxErreurBinaire());
    }

    /** Caractérise le TEB nul renvoyé lorsqu'aucun message n'a été reçu. */
    @Test void calculSansReceptionRetourneActuellementZero() throws Exception {
        assertEquals(0.0f, preparerCalcul("01", null).calculTauxErreurBinaire());
    }

    /** Caractérise le TEB nul renvoyé pour une émission vide. */
    @Test void calculAvecEmissionVideRetourneActuellementZero() throws Exception {
        assertEquals(0.0f, preparerCalcul("", "1").calculTauxErreurBinaire());
    }

    /** Vérifie que tous les bits émis mais non reçus sont comptés comme erronés. */
    @Test void calculAvecReceptionVideCompteTousLesBitsPerdus() throws Exception {
        assertEquals(1.0f, preparerCalcul("01", "").calculTauxErreurBinaire());
    }

    /** Caractérise un TEB supérieur à un lorsque la réception dépasse l'émission. */
    @Test void receptionTropLonguePeutActuellementDepasserUn() throws Exception {
        assertEquals(3.0f, preparerCalcul("0", "0111").calculTauxErreurBinaire());
    }

    /** Caractérise l'exception actuelle si un bit émis est nul. */
    @Test void bitEmisNullProvoqueActuellementUneException() throws Exception {
        Simulateur simulateur = preparerCalcul("0", "0");
        source(simulateur).getInformationEmise().setIemeElement(0, null);
        assertThrows(NullPointerException.class, simulateur::calculTauxErreurBinaire);
    }

    // -------------------------------------------------------------
    // Tests Étape 2 (Nathan) : Arguments et chaîne analogique
    // -------------------------------------------------------------

    /** Vérifie que les formes d'onde valides activent la transmission analogique. */
    @ParameterizedTest
    @ValueSource(strings = {"RZ", "NRZ", "NRZT"})
    void argumentFormeValideActiveTransmissionAnalogique(String forme) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", forme});
        assertTrue((boolean) lireChamp(simulateur, Simulateur.class, "transmissionAnalogique"));
        assertEquals(forme, lireChamp(simulateur, Simulateur.class, "formeOnde"));
    }

    /** Vérifie que le nombre d'échantillons et les amplitudes fournis sont mémorisés. */
    @Test
    void argumentsNbEchEtAmplitudesValidesSontPrisEnCompte() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-nbEch", "60", "-ampl", "-2.5", "3.5"});
        assertEquals(60, (int) lireChamp(simulateur, Simulateur.class, "nbEch"));
        assertEquals(-2.5f, (float) lireChamp(simulateur, Simulateur.class, "amplMin"));
        assertEquals(3.5f, (float) lireChamp(simulateur, Simulateur.class, "amplMax"));
    }

    /** Vérifie que les formes d'onde inconnues sont refusées. */
    @ParameterizedTest
    @ValueSource(strings = {"TOTO", "nrz", "rz", "123", ""})
    void argumentFormeInvalideEstRejete(String forme) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", forme}));
    }

    /** Vérifie qu'une option de forme sans valeur est refusée. */
    @Test
    void argumentFormeOrphelineEstRejetee() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form"}));
    }

    /** Vérifie que les nombres d'échantillons invalides sont refusés. */
    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "abc", "1.5"})
    void argumentNbEchInvalideEstRejete(String ech) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-nbEch", ech}));
    }

    /** Vérifie qu'une option de nombre d'échantillons sans valeur est refusée. */
    @Test
    void argumentNbEchOrphelinEstRejete() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-nbEch"}));
    }

    /** Vérifie que les amplitudes mal formées ou incohérentes sont refusées. */
    @ParameterizedTest
    @CsvSource({"1.0,1.0", "2.0,1.0", "a,b", "1.0,b"})
    void argumentAmplIncoherentEstRejete(String min, String max) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-ampl", min, max}));
    }

    /** Vérifie qu'une option amplitude incomplète est refusée. */
    @Test
    void argumentAmplOrphelinEstRejete() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-ampl", "1.0"}));
    }

    /** Vérifie que la forme RZ impose une amplitude minimale nulle. */
    @Test
    void contraintesRzAvecAmplMinNonNulleRejetee() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "RZ", "-ampl", "-1.0", "1.0"}));
    }

    /** Vérifie les connexions de la chaîne source, émetteur, canal, récepteur et destination. */
    @Test
    void chaineAnalogiqueConnecteEmetteurTransmetteurRecepteur() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-mess", "101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        assertEquals(1, sortiesSource.size());
        assertInstanceOf(Emetteur.class, sortiesSource.get(0));

        Emetteur emetteur = (Emetteur) sortiesSource.get(0);
        List<?> sortiesEmetteur = (List<?>) lireChamp(emetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesEmetteur.size());
        assertInstanceOf(Transmetteur.class, sortiesEmetteur.get(0));

        Transmetteur<?, ?> canal = (Transmetteur<?, ?>) sortiesEmetteur.get(0);
        List<?> sortiesCanal = (List<?>) lireChamp(canal, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesCanal.size());
        assertInstanceOf(Recepteur.class, sortiesCanal.get(0));

        Recepteur recepteur = (Recepteur) sortiesCanal.get(0);
        List<?> sortiesRecepteur = (List<?>) lireChamp(recepteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesRecepteur.size());
        assertInstanceOf(DestinationFinale.class, sortiesRecepteur.get(0));
    }

    /** Vérifie le raccordement des quatre sondes dans la chaîne analogique. */
    @Test
    void constructeurAnalogiqueAvecSondesConnecteQuatreSondes() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-s", "-form", "NRZ", "-mess", "101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        Emetteur emetteur = (Emetteur) sortiesSource.get(0);
        List<?> sortiesEmetteur = (List<?>) lireChamp(emetteur, Transmetteur.class, "destinationsConnectees");
        Transmetteur<?, ?> canal = (Transmetteur<?, ?>) sortiesEmetteur.get(0);
        List<?> sortiesCanal = (List<?>) lireChamp(canal, Transmetteur.class, "destinationsConnectees");
        Recepteur recepteur = (Recepteur) sortiesCanal.get(0);
        List<?> sortiesRecepteur = (List<?>) lireChamp(recepteur, Transmetteur.class, "destinationsConnectees");

        assertEquals(2, sortiesSource.size());
        assertInstanceOf(SondeLogique.class, sortiesSource.get(1));

        assertEquals(2, sortiesEmetteur.size());
        assertInstanceOf(SondeAnalogique.class, sortiesEmetteur.get(1));

        assertEquals(2, sortiesCanal.size());
        assertInstanceOf(SondeAnalogique.class, sortiesCanal.get(1));

        assertEquals(2, sortiesRecepteur.size());
        assertInstanceOf(SondeLogique.class, sortiesRecepteur.get(1));
    }

    /** Vérifie que les formes analogiques testées transmettent le message sans erreur. */
    @ParameterizedTest
    @ValueSource(strings = {"NRZ", "RZ"})
    void executeTransmissionAnalogiqueTransmetSansErreur(String forme) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", forme, "-mess", "1010101"});
        simulateur.execute();
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
        assertEquals(destination(simulateur).getInformationRecue(), source(simulateur).getInformationEmise());
    }

    /** Vérifie qu'une option SNR par bit sans valeur est refusée. */
    @Test
    void snrpbAbsentDoitLeverArgumentsException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-snrpb"}));
    }

    /** Vérifie qu'une valeur non numérique du SNR par bit est refusée. */
    @Test
    void snrpbInvalideDoitLeverArgumentsException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-snrpb", "invalide"}));
    }

    /** Vérifie qu'un SNR par bit configure un transmetteur bruité dans la chaîne analogique. */
    @Test
    void chaineAnalogiqueAvecSnrpbConnecteTransmetteurBruite() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "10.0", "-mess", "101"});
        Emetteur emetteur = (Emetteur) ((List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees")).get(0);
        List<?> sortiesEmetteur = (List<?>) lireChamp(emetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesEmetteur.size());
        assertInstanceOf(TransmetteurBruite.class, sortiesEmetteur.get(0));
    }

    /** Vérifie qu'un SNR élevé donne un TEB nul pour le message et la semence du test. */
    @Test
    void executeAvecSnrpbFortDonneTebNul() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "30.0", "-mess", "1000", "-seed", "42"});
        simulateur.execute();
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
    }

    /** Vérifie qu'un SNR très faible produit un TEB non nul pour le message et la semence du test. */
    @Test
    void executeAvecSnrpbTresFaibleDonneTebNonNul() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "-10.0", "-mess", "1000", "-seed", "42"});
        simulateur.execute();
        assertTrue(simulateur.calculTauxErreurBinaire() > 0.1f);
    }

    /** Vérifie la lecture de plusieurs trajets et de l'option qui suit. */
    @Test
    void tiLitLesTrajets() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-snrpb", "10", "-ti", "2", "0.5", "4", "1.0", "-seed", "42"});
        assertEquals(List.of(new Trajet(2, 0.5f), new Trajet(4, 1.0f)),
                lireChamp(simulateur, Simulateur.class, "multiTrajets"));
        assertEquals(42, ((SourceAleatoire) source(simulateur)).getSeed());
    }

    /** Vérifie qu'un trajet absent ou incomplet est refusé. */
    @Test
    void tiIncompletEstRejete() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-ti"}));
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-ti", "2"}));
    }

    /** Vérifie que les valeurs invalides d'un trajet sont refusées. */
    @ParameterizedTest
    @CsvSource({"abc,0.5", "2,abc", "2,1.5"})
    void tiInvalideEstRejete(String retard, String attenuation) {
        assertThrows(ArgumentsException.class,
                () -> new Simulateur(new String[]{"-ti", retard, attenuation}));
    }

    /** Vérifie que cinq trajets sont acceptés, mais pas six. */
    @Test
    void tiLimiteLesTrajets() throws Exception {
        String[] cinq = {"-ti", "1", "0.1", "2", "0.2", "3", "0.3", "4", "0.4", "5", "0.5"};
        Simulateur simulateur = new Simulateur(cinq);
        assertEquals(5, ((List<?>) lireChamp(simulateur, Simulateur.class, "multiTrajets")).size());
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{
                "-ti", "1", "0.1", "2", "0.2", "3", "0.3", "4", "0.4", "5", "0.5", "6", "0.6"}));
    }
}
