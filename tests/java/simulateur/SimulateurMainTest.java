package simulateur;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import static org.junit.jupiter.api.Assertions.*;

class SimulateurMainTest {
    // Un processus séparé empêche System.exit d'arrêter JUnit.
    private record Resultat(int code, String sortie) {}

    private Resultat lancer(String... arguments) throws Exception {
        List<String> commande = new ArrayList<>();
        commande.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        // Inclure les processus enfants dans la couverture Maven, si JaCoCo est actif.
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.startsWith("-javaagent:") && argument.contains("jacoco")) commande.add(argument);
        }
        commande.add("-Djava.awt.headless=true");
        commande.add("-cp");
        commande.add(Path.of(Simulateur.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        commande.add("simulateur.Simulateur");
        commande.addAll(List.of(arguments));
        Path sortie = Files.createTempFile("simulateur-test-", ".txt");
        Process processus = null;
        try {
            processus = new ProcessBuilder(commande).redirectErrorStream(true).redirectOutput(sortie.toFile()).start();
            assertTrue(processus.waitFor(20, TimeUnit.SECONDS), "La simulation doit terminer en moins de 20 secondes");
            return new Resultat(processus.exitValue(), Files.readString(sortie));
        } finally {
            if (processus != null && processus.isAlive()) {
                processus.destroyForcibly();
                processus.waitFor(5, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(sortie);
        }
    }

    @Test void mainParDefautAfficheTEBZero() throws Exception {
        Resultat resultat = lancer();
        assertEquals(0, resultat.code());
        assertTrue(resultat.sortie().contains("TEB : 0.0"), resultat.sortie());
    }

    @Test void mainAvecArgumentsAfficheLaCommandeEtLeTEB() throws Exception {
        Resultat resultat = lancer("-mess", "1010101", "-seed", "42");
        assertEquals(0, resultat.code());
        assertTrue(resultat.sortie().contains("-mess  1010101"), resultat.sortie());
        assertTrue(resultat.sortie().contains("-seed  42"), resultat.sortie());
        assertTrue(resultat.sortie().contains("TEB : 0.0"), resultat.sortie());
    }

    @Test void mainErreurArgumentsTermineAvecCodeMoinsUn() throws Exception {
        Resultat resultat = lancer("-inconnu");
        assertEquals(System.getProperty("os.name").startsWith("Windows") ? -1 : 255, resultat.code());
        assertTrue(resultat.sortie().contains("ArgumentsException"), resultat.sortie());
        assertFalse(resultat.sortie().contains("TEB :"));
    }

    @Test void mainErreurExecutionTermineAvecCodeMoinsDeux() throws Exception {
        // L'affichage est impossible en mode sans écran : couvre le second catch de main.
        Resultat resultat = lancer("-s");
        assertEquals(System.getProperty("os.name").startsWith("Windows") ? -2 : 254, resultat.code());
        assertTrue(resultat.sortie().contains("HeadlessException"), resultat.sortie());
        assertFalse(resultat.sortie().contains("TEB :"));
    }
}
