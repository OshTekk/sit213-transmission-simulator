package simulateur;

import destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import sources.Source;
import sources.SourceFixe;
import sources.SourceAleatoire;
import transmetteurs.Transmetteur;
import transmetteurs.TransmetteurBruite;
import transmetteurs.Trajet;
import transmetteurs.Emetteur;
import transmetteurs.Recepteur;
import transmetteurs.Codeur;
import transmetteurs.Decodeur;
import transmetteurs.TransmetteurParfait;
import visualisations.SondeLogique;
import visualisations.SondeAnalogique;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("Valeurs par défaut, source aléatoire et état initial")
    void testConstructeurParDefaut() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{});
        SourceAleatoire source = assertInstanceOf(SourceAleatoire.class, source(simulateur));
        assertEquals(100, source.getNbBits());
        assertNull(source.getSeed());
        assertNull(source.getInformationEmise());
        assertNull(destination(simulateur).getInformationRecue());
        assertEquals(false, lireChamp(simulateur, Simulateur.class, "affichage"));
    }

    @Test
    @DisplayName("Connexion des sondes en transmission logique (-s)")
    void testConstructeurAvecSondes() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-s", "-mess", "1010101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        Object transmetteur = assertInstanceOf(Transmetteur.class, sortiesSource.get(0));
        List<?> sortiesTransmetteur = (List<?>) lireChamp(transmetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(2, sortiesSource.size());
        assertEquals(2, sortiesTransmetteur.size());
        assertInstanceOf(visualisations.SondeLogique.class, sortiesSource.get(1));
        assertInstanceOf(visualisations.SondeLogique.class, sortiesTransmetteur.get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000000", "1111111", "01010101"})
    @DisplayName("Transmission sans erreur d'un message binaire fixe")
    void testTransmissionMessageFixe(String message) throws Exception {
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

    @ParameterizedTest
    @ValueSource(strings = {"1", "000001", "100", "111111", "999999"})
    @DisplayName("Interprétation d'un message court comme longueur aléatoire")
    void testMessageCourtLongueur(String longueur) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", longueur, "-seed", "42"});
        assertInstanceOf(SourceAleatoire.class, source(simulateur));
        simulateur.execute();
        // Ne pas calculer le TEB sur 999999 éléments : Information utilise une LinkedList.
        assertEquals(Integer.parseInt(longueur), destination(simulateur).getInformationRecue().nbElements());
        assertSame(source(simulateur).getInformationEmise(), destination(simulateur).getInformationRecue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "abcdefg", "1000002", "", " 7"})
    @DisplayName("Rejet des valeurs invalides de l'option -mess")
    void testMessageInvalideRejete(String message) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess", message}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "1.5", "2147483648", "", " 42"})
    @DisplayName("Rejet des valeurs invalides de l'option -seed")
    void testGraineInvalideRejetee(String graine) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-seed", graine}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-inconnu", "", "-S"})
    @DisplayName("Rejet des options inconnues")
    void testOptionInconnueRejetee(String option) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{option}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2147483647", "-2147483648"})
    @DisplayName("Prise en compte des semences entières valides")
    void testGraineEntiereAcceptee(String graine) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", graine});
        assertEquals(Integer.valueOf(graine), ((SourceAleatoire) source(simulateur)).getSeed());
    }

    @Test
    @DisplayName("Reproductibilité avec la même semence quel que soit l'ordre des options")
    void testGraineEtOrdreOptions() throws Exception {
        Simulateur a = new Simulateur(new String[]{"-mess", "20", "-seed", "42"});
        Simulateur b = new Simulateur(new String[]{"-seed", "42", "-mess", "20"});
        a.execute();
        b.execute();
        assertEquals(destination(a).getInformationRecue(), destination(b).getInformationRecue());
        assertEquals(0.0f, a.calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("Conservation du message fixe même en présence d'une semence")
    void testGraineAvecMessageFixe() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", "42", "-mess", "1010101"});
        simulateur.execute();
        assertInstanceOf(SourceFixe.class, source(simulateur));
        assertEquals(7, destination(simulateur).getInformationRecue().nbElements());
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
    }

    // Régressions attendues en échec sur etape_1 : ne pas adapter l'attente au défaut.
    @Test
    @DisplayName("Rejet de l'option -mess sans valeur")
    void testMessageAbsentException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess"}));
    }

    @Test
    @DisplayName("Rejet de l'option -seed sans valeur")
    void testGraineAbsenteException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-seed"}));
    }

    @Test
    @DisplayName("Rejet d'une virgule dans un message binaire")
    void testVirguleDansMessageRejetee() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess", "101,010"}));
    }

    @Test
    @DisplayName("Priorité à la dernière valeur en cas d'options répétées")
    void testDerniereOptionRepeteePrioritaire() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", "10", "-mess", "12", "-seed", "1", "-seed", "2"});
        SourceAleatoire source = (SourceAleatoire) source(simulateur);
        assertEquals(12, source.getNbBits());
        assertEquals(2, source.getSeed());
    }

    @Test
    @DisplayName("Levée de NullPointerException en cas d'arguments nuls")
    void testArgumentsNulsException() {
        assertThrows(NullPointerException.class, () -> new Simulateur(null));
        assertThrows(NullPointerException.class, () -> new Simulateur(new String[]{null}));
    }

    @Test
    @DisplayName("Conservation du message généré sur deux exécutions consécutives")
    void testExecuteDeuxFoisConserveMessage() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", "42"});
        simulateur.execute();
        Information<Boolean> premier = destination(simulateur).getInformationRecue();
        simulateur.execute();
        assertEquals(premier, destination(simulateur).getInformationRecue());
    }

    @Test
    @DisplayName("Propagation d'une exception levée par la source")
    void testExecutePropageExceptionSource() throws Exception {
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

    @ParameterizedTest
    @CsvSource({"0101,0101,0.0", "0101,0111,0.25", "0101,1010,1.0", "0101,01,0.5", "01,010,0.5"})
    @DisplayName("Calcul du TEB comparant bits émis, reçus et longueurs")
    void testCalculTebComparaison(String emis, String recu, float attendu) throws Exception {
        assertEquals(attendu, preparerCalcul(emis, recu).calculTauxErreurBinaire(), 0.000001f);
    }

    @Test
    @DisplayName("TEB nul avant toute exécution")
    void testCalculTebAvantExecution() throws Exception {
        assertEquals(0.0f, new Simulateur(new String[]{}).calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("TEB nul lorsqu'aucun message n'a été reçu")
    void testCalculTebSansReception() throws Exception {
        assertEquals(0.0f, preparerCalcul("01", null).calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("TEB nul pour une émission vide")
    void testCalculTebEmissionVide() throws Exception {
        assertEquals(0.0f, preparerCalcul("", "1").calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("TEB égal à 1.0 lorsque tous les bits sont perdus")
    void testCalculTebReceptionVide() throws Exception {
        assertEquals(1.0f, preparerCalcul("01", "").calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("Calcul du TEB lorsque la réception dépasse l'émission")
    void testCalculTebReceptionTropLongue() throws Exception {
        assertEquals(3.0f, preparerCalcul("0", "0111").calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("Levée de NullPointerException si un bit émis est null")
    void testCalculTebBitEmisNull() throws Exception {
        Simulateur simulateur = preparerCalcul("0", "0");
        source(simulateur).getInformationEmise().setIemeElement(0, null);
        assertThrows(NullPointerException.class, simulateur::calculTauxErreurBinaire);
    }

    // -------------------------------------------------------------
    // Tests Étape 2 (Nathan) : Arguments et chaîne analogique
    // -------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"RZ", "NRZ", "NRZT"})
    @DisplayName("Activation de la transmission analogique avec forme d'onde valide")
    void testFormeValideActiveAnalogique(String forme) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", forme});
        assertTrue((boolean) lireChamp(simulateur, Simulateur.class, "transmissionAnalogique"));
        assertEquals(forme, lireChamp(simulateur, Simulateur.class, "formeOnde"));
    }

    @Test
    @DisplayName("Prise en compte du nombre d'échantillons et des amplitudes")
    void testNbEchEtAmplitudesValides() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-nbEch", "60", "-ampl", "-2.5", "3.5"});
        assertEquals(60, (int) lireChamp(simulateur, Simulateur.class, "nbEch"));
        assertEquals(-2.5f, (float) lireChamp(simulateur, Simulateur.class, "amplMin"));
        assertEquals(3.5f, (float) lireChamp(simulateur, Simulateur.class, "amplMax"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOTO", "nrz", "rz", "123", ""})
    @DisplayName("Rejet des formes d'onde invalides")
    void testFormeInvalideRejetee(String forme) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", forme}));
    }

    @Test
    @DisplayName("Rejet de l'option -form sans valeur")
    void testFormeOrphelineRejetee() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form"}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "abc", "1.5"})
    @DisplayName("Rejet des nombres d'échantillons invalides")
    void testNbEchInvalideRejete(String ech) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-nbEch", ech}));
    }

    @Test
    @DisplayName("Rejet de l'option -nbEch sans valeur")
    void testNbEchOrphelinRejete() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-nbEch"}));
    }

    @ParameterizedTest
    @CsvSource({"1.0,1.0", "2.0,1.0", "a,b", "1.0,b"})
    @DisplayName("Rejet des couples d'amplitudes incohérents")
    void testAmplIncoherentRejete(String min, String max) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-ampl", min, max}));
    }

    @Test
    @DisplayName("Rejet de l'option -ampl incomplète")
    void testAmplOrphelinRejete() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "NRZ", "-ampl", "1.0"}));
    }

    @Test
    @DisplayName("Rejet de l'amplitude minimale non nulle pour la forme RZ")
    void testContraintesRzAmplMinNonNulle() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-form", "RZ", "-ampl", "-1.0", "1.0"}));
    }

    @Test
    @DisplayName("Connexion de la chaîne analogique complète (Source -> Emetteur -> Canal -> Recepteur -> Destination)")
    void testChaineAnalogiqueConnexions() throws Exception {
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

    @Test
    @DisplayName("Connexion des quatre sondes en transmission analogique (-s)")
    void testChaineAnalogiqueAvecSondes() throws Exception {
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

    @ParameterizedTest
    @ValueSource(strings = {"NRZ", "RZ"})
    @DisplayName("Transmission analogique sans erreur en canal parfait")
    void testTransmissionAnalogiqueSansErreur(String forme) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", forme, "-mess", "1010101"});
        simulateur.execute();
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
        assertEquals(destination(simulateur).getInformationRecue(), source(simulateur).getInformationEmise());
    }

    @Test
    @DisplayName("Rejet de l'option -snrpb sans valeur")
    void testSnrpbAbsentException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-snrpb"}));
    }

    @Test
    @DisplayName("Rejet d'une valeur non numérique pour -snrpb")
    void testSnrpbInvalideException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-snrpb", "invalide"}));
    }

    @Test
    @DisplayName("Connexion d'un TransmetteurBruite lors de l'utilisation de -snrpb")
    void testChaineAnalogiqueAvecSnrpb() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "10.0", "-mess", "101"});
        Emetteur emetteur = (Emetteur) ((List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees")).get(0);
        List<?> sortiesEmetteur = (List<?>) lireChamp(emetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesEmetteur.size());
        assertInstanceOf(TransmetteurBruite.class, sortiesEmetteur.get(0));
    }

    @Test
    @DisplayName("Transmission à fort SNR donnant un TEB nul")
    void testTransmissionSnrpbFortTebNul() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "30.0", "-mess", "1000", "-seed", "42"});
        simulateur.execute();
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
    }

    @Test
    @DisplayName("Transmission à très faible SNR donnant un TEB non nul")
    void testTransmissionSnrpbFaibleTebNonNul() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "-10.0", "-mess", "1000", "-seed", "42"});
        simulateur.execute();
        assertTrue(simulateur.calculTauxErreurBinaire() > 0.1f);
    }

    @Test
    @DisplayName("Lecture et stockage de plusieurs trajets multiples (-ti)")
    void testTiLectureTrajets() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-snrpb", "10", "-ti", "2", "0.5", "4", "1.0", "-seed", "42"});
        assertEquals(List.of(new Trajet(2, 0.5f), new Trajet(4, 1.0f)),
                lireChamp(simulateur, Simulateur.class, "multiTrajets"));
        assertEquals(42, ((SourceAleatoire) source(simulateur)).getSeed());
    }

    @Test
    @DisplayName("Rejet de l'option -ti sans valeurs ou incomplète")
    void testTiIncompletRejete() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-ti"}));
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-ti", "2"}));
    }

    @ParameterizedTest
    @CsvSource({"abc,0.5", "2,abc", "2,1.5"})
    @DisplayName("Rejet des valeurs invalides de trajets multiples")
    void testTiInvalideRejete(String retard, String attenuation) {
        assertThrows(ArgumentsException.class,
                () -> new Simulateur(new String[]{"-ti", retard, attenuation}));
    }

    @Test
    @DisplayName("Plafonnement du nombre de trajets multiples à 5")
    void testTiLimiteNombreTrajets() throws Exception {
        String[] cinq = {"-ti", "1", "0.1", "2", "0.2", "3", "0.3", "4", "0.4", "5", "0.5"};
        Simulateur simulateur = new Simulateur(cinq);
        assertEquals(5, ((List<?>) lireChamp(simulateur, Simulateur.class, "multiTrajets")).size());
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{
                "-ti", "1", "0.1", "2", "0.2", "3", "0.3", "4", "0.4", "5", "0.5", "6", "0.6"}));
    }

    @Test
    @DisplayName("Connexion du canal avec l'option -ti seule sans bruit")
    void testTiSeulConnecteCanalSansBruit() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-ti", "2", "0.5"});
        Emetteur emetteur = (Emetteur) ((List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees")).get(0);
        List<?> sortiesEmetteur = (List<?>) lireChamp(emetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesEmetteur.size());
        assertInstanceOf(TransmetteurBruite.class, sortiesEmetteur.get(0));
        TransmetteurBruite canal = (TransmetteurBruite) sortiesEmetteur.get(0);
        assertNull(lireChamp(canal, TransmetteurBruite.class, "snrpb"), "Le bruit ne doit pas être activé si -snrpb est absent");
        assertEquals(List.of(new Trajet(2, 0.5f)), lireChamp(canal, TransmetteurBruite.class, "multiTrajets"));
    }

    @Test
    @DisplayName("Exécution sans erreur avec l'option -ti seule")
    void testExecuteAvecTiSeulSansErreur() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", "0101010101", "-ti", "1", "0.1"});
        assertDoesNotThrow(simulateur::execute);
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire(), 1e-6f);
    }

    @Test
    @DisplayName("Exécution combinant trajets multiples (-ti) et bruit (-snrpb)")
    void testExecuteAvecTiEtSnrpbCombine() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", "1000", "-snrpb", "20", "-ti", "2", "0.2", "-seed", "42"});
        assertDoesNotThrow(simulateur::execute);
        assertNotNull(destination(simulateur).getInformationRecue());
    }

    @Test
    @DisplayName("Calcul du TEB en présence d'interférences de multitrajets sévères")
    void testExecuteAvecTiSevere() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", "01010101010101010101", "-nbEch", "30", "-ti", "15", "1.0"});
        assertDoesNotThrow(simulateur::execute);
        float teb = simulateur.calculTauxErreurBinaire();
        assertTrue(teb >= 0.0f && teb <= 1.0f);
    }

    // -------------------------------------------------------------
    // Tests Étape 5 (Nathan) : Codage canal (-codeur)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Prise en compte de l'option -codeur dans les arguments")
    void testOptionCodeurReconnue() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-codeur"});
        assertTrue((boolean) lireChamp(simulateur, Simulateur.class, "codageCanal"));
    }

    @Test
    @DisplayName("Connexion de la chaîne logique avec codeur et décodeur (-codeur)")
    void testChaineLogiqueAvecCodeur() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-codeur", "-mess", "101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        assertEquals(1, sortiesSource.size());
        assertInstanceOf(Codeur.class, sortiesSource.get(0));

        Codeur codeur = (Codeur) sortiesSource.get(0);
        List<?> sortiesCodeur = (List<?>) lireChamp(codeur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesCodeur.size());
        assertInstanceOf(TransmetteurParfait.class, sortiesCodeur.get(0));

        Transmetteur<?, ?> canal = (Transmetteur<?, ?>) sortiesCodeur.get(0);
        List<?> sortiesCanal = (List<?>) lireChamp(canal, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesCanal.size());
        assertInstanceOf(Decodeur.class, sortiesCanal.get(0));

        Decodeur decodeur = (Decodeur) sortiesCanal.get(0);
        List<?> sortiesDecodeur = (List<?>) lireChamp(decodeur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesDecodeur.size());
        assertInstanceOf(DestinationFinale.class, sortiesDecodeur.get(0));
    }

    @Test
    @DisplayName("Transmission sans erreur avec codage canal en canal parfait (-codeur)")
    void testExecuteAvecCodeurSansErreur() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-codeur", "-mess", "1010101"});
        simulateur.execute();
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
        assertEquals(destination(simulateur).getInformationRecue(), source(simulateur).getInformationEmise());
    }

    @Test
    @DisplayName("Connexion de la chaîne analogique avec codeur et décodeur (-codeur -form)")
    void testChaineAnalogiqueAvecCodeur() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-codeur", "-form", "NRZ", "-mess", "101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        assertEquals(1, sortiesSource.size());
        assertInstanceOf(Codeur.class, sortiesSource.get(0));

        Codeur codeur = (Codeur) sortiesSource.get(0);
        List<?> sortiesCodeur = (List<?>) lireChamp(codeur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesCodeur.size());
        assertInstanceOf(Emetteur.class, sortiesCodeur.get(0));

        Emetteur emetteur = (Emetteur) sortiesCodeur.get(0);
        List<?> sortiesEmetteur = (List<?>) lireChamp(emetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesEmetteur.size());
        assertInstanceOf(TransmetteurParfait.class, sortiesEmetteur.get(0));

        Transmetteur<?, ?> canal = (Transmetteur<?, ?>) sortiesEmetteur.get(0);
        List<?> sortiesCanal = (List<?>) lireChamp(canal, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesCanal.size());
        assertInstanceOf(Recepteur.class, sortiesCanal.get(0));

        Recepteur recepteur = (Recepteur) sortiesCanal.get(0);
        List<?> sortiesRecepteur = (List<?>) lireChamp(recepteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesRecepteur.size());
        assertInstanceOf(Decodeur.class, sortiesRecepteur.get(0));

        Decodeur decodeur = (Decodeur) sortiesRecepteur.get(0);
        List<?> sortiesDecodeur = (List<?>) lireChamp(decodeur, Transmetteur.class, "destinationsConnectees");
        assertEquals(1, sortiesDecodeur.size());
        assertInstanceOf(DestinationFinale.class, sortiesDecodeur.get(0));
    }

    @Test
    @DisplayName("Transmission analogique sans erreur avec codeur en canal parfait (-codeur -form)")
    void testExecuteAvecCodeurEtAnalogiqueSansErreur() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-codeur", "-form", "NRZ", "-mess", "1010101"});
        simulateur.execute();
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
        assertEquals(destination(simulateur).getInformationRecue(), source(simulateur).getInformationEmise());
    }

    @Test
    @DisplayName("Réduction du TEB en canal bruité grâce au codeur de canal (-codeur)")
    void testCodeurReduitTebEnCanalBruite() throws Exception {
        Simulateur simuSansCodeur = new Simulateur(new String[]{"-mess", "5000", "-form", "NRZ", "-snrpb", "2", "-seed", "42"});
        simuSansCodeur.execute();
        float tebSans = simuSansCodeur.calculTauxErreurBinaire();

        Simulateur simuAvecCodeur = new Simulateur(new String[]{"-mess", "5000", "-form", "NRZ", "-snrpb", "2", "-seed", "42", "-codeur"});
        simuAvecCodeur.execute();
        float tebAvec = simuAvecCodeur.calculTauxErreurBinaire();

        assertTrue(tebSans > 0.05f, "Le TEB sans codeur doit présenter des erreurs à ce SNR");
        assertTrue(tebAvec < tebSans, "Le codeur de canal doit réduire significativement le TEB");
    }

    @Test
    @DisplayName("Configuration avec sondes et codage canal (-s -codeur)")
    void testChaineAvecSondesEtCodeur() {
        assertDoesNotThrow(() -> {
            Simulateur simuLogique = new Simulateur(new String[]{"-s", "-codeur", "-mess", "1010101"});
            assertNotNull(simuLogique);
            Simulateur simuAnalogique = new Simulateur(new String[]{"-s", "-codeur", "-form", "RZ", "-mess", "1010101"});
            assertNotNull(simuAnalogique);
        });
    }
}

