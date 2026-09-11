package simulateur;

import destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import sources.Source;
import sources.SourceFixe;
import sources.SourceAleatoire;
import transmetteurs.Transmetteur;
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

    @Test void constructeurParDefaut() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{});
        SourceAleatoire source = assertInstanceOf(SourceAleatoire.class, source(simulateur));
        assertEquals(100, source.getNbBits());
        assertNull(source.getSeed());
        assertNull(source.getInformationEmise());
        assertNull(destination(simulateur).getInformationRecue());
        assertEquals(false, lireChamp(simulateur, Simulateur.class, "affichage"));
    }

    @Test void constructeurAvecSondesConnecteDeuxSondes() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-s", "-mess", "1010101"});
        List<?> sortiesSource = (List<?>) lireChamp(source(simulateur), Source.class, "destinationsConnectees");
        Object transmetteur = lireChamp(simulateur, Simulateur.class, "transmetteurLogique");
        List<?> sortiesTransmetteur = (List<?>) lireChamp(transmetteur, Transmetteur.class, "destinationsConnectees");
        assertEquals(2, sortiesSource.size());
        assertEquals(2, sortiesTransmetteur.size());
        assertInstanceOf(visualisations.SondeLogique.class, sortiesSource.get(1));
        assertInstanceOf(visualisations.SondeLogique.class, sortiesTransmetteur.get(1));
    }

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

    @ParameterizedTest @ValueSource(strings = {"1", "000001", "100", "111111", "999999"})
    void argumentMessageCourtDesigneUneLongueur(String longueur) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", longueur, "-seed", "42"});
        assertInstanceOf(SourceAleatoire.class, source(simulateur));
        simulateur.execute();
        // Ne pas calculer le TEB sur 999999 éléments : Information utilise une LinkedList.
        assertEquals(Integer.parseInt(longueur), destination(simulateur).getInformationRecue().nbElements());
        assertSame(source(simulateur).getInformationEmise(), destination(simulateur).getInformationRecue());
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.5", "abcdefg", "1000002", "", " 7"})
    void argumentMessageInvalideEstRejete(String message) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess", message}));
    }

    @ParameterizedTest @ValueSource(strings = {"x", "1.5", "2147483648", "", " 42"})
    void argumentGraineInvalideEstRejete(String graine) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-seed", graine}));
    }

    @ParameterizedTest @ValueSource(strings = {"-inconnu", "", "-S"})
    void optionInconnueEstRejetee(String option) {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{option}));
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "2147483647", "-2147483648"})
    void graineEntiereEstAcceptee(String graine) throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", graine});
        assertEquals(Integer.valueOf(graine), ((SourceAleatoire) source(simulateur)).getSeed());
    }

    @Test void graineEtOrdreDesOptionsReproduisentLaSimulation() throws Exception {
        Simulateur a = new Simulateur(new String[]{"-mess", "20", "-seed", "42"});
        Simulateur b = new Simulateur(new String[]{"-seed", "42", "-mess", "20"});
        a.execute();
        b.execute();
        assertEquals(destination(a).getInformationRecue(), destination(b).getInformationRecue());
        assertEquals(0.0f, a.calculTauxErreurBinaire());
    }

    @Test void graineAvecMessageFixeNeChangePasLeMessage() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", "42", "-mess", "1010101"});
        simulateur.execute();
        assertInstanceOf(SourceFixe.class, source(simulateur));
        assertEquals(7, destination(simulateur).getInformationRecue().nbElements());
        assertEquals(0.0f, simulateur.calculTauxErreurBinaire());
    }

    // Régressions attendues en échec sur etape_1 : ne pas adapter l'attente au défaut.
    @Test void messageAbsentDoitLeverArgumentsException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess"}));
    }

    @Test void graineAbsenteDoitLeverArgumentsException() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-seed"}));
    }

    @Test void virguleDansMessageDoitEtreRejetee() {
        assertThrows(ArgumentsException.class, () -> new Simulateur(new String[]{"-mess", "101,010"}));
    }

    @Test void derniereOptionRepeteeEstActuellementPrioritaire() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-mess", "10", "-mess", "12", "-seed", "1", "-seed", "2"});
        SourceAleatoire source = (SourceAleatoire) source(simulateur);
        assertEquals(12, source.getNbBits());
        assertEquals(2, source.getSeed());
    }

    @Test void argumentsNulsProvoquentActuellementUneException() {
        assertThrows(NullPointerException.class, () -> new Simulateur(null));
        assertThrows(NullPointerException.class, () -> new Simulateur(new String[]{null}));
    }

    @Test void executeDeuxFoisConserveLeMessage() throws Exception {
        Simulateur simulateur = new Simulateur(new String[]{"-seed", "42"});
        simulateur.execute();
        Information<Boolean> premier = destination(simulateur).getInformationRecue();
        simulateur.execute();
        assertEquals(premier, destination(simulateur).getInformationRecue());
    }

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

    @ParameterizedTest
    @CsvSource({"0101,0101,0.0", "0101,0111,0.25", "0101,1010,1.0", "0101,01,0.5", "01,010,0.5"})
    void calculTEBCompareLesBitsEtLesLongueurs(String emis, String recu, float attendu) throws Exception {
        assertEquals(attendu, preparerCalcul(emis, recu).calculTauxErreurBinaire(), 0.000001f);
    }

    @Test void calculAvantExecutionRetourneActuellementZero() throws Exception {
        assertEquals(0.0f, new Simulateur(new String[]{}).calculTauxErreurBinaire());
    }

    @Test void calculSansReceptionRetourneActuellementZero() throws Exception {
        assertEquals(0.0f, preparerCalcul("01", null).calculTauxErreurBinaire());
    }

    @Test void calculAvecEmissionVideRetourneActuellementZero() throws Exception {
        assertEquals(0.0f, preparerCalcul("", "1").calculTauxErreurBinaire());
    }

    @Test void calculAvecReceptionVideCompteTousLesBitsPerdus() throws Exception {
        assertEquals(1.0f, preparerCalcul("01", "").calculTauxErreurBinaire());
    }

    @Test void receptionTropLonguePeutActuellementDepasserUn() throws Exception {
        assertEquals(3.0f, preparerCalcul("0", "0111").calculTauxErreurBinaire());
    }

    @Test void bitEmisNullProvoqueActuellementUneException() throws Exception {
        Simulateur simulateur = preparerCalcul("0", "0");
        source(simulateur).getInformationEmise().setIemeElement(0, null);
        assertThrows(NullPointerException.class, simulateur::calculTauxErreurBinaire);
    }
}
