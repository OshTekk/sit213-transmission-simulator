package visualisations;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SondeTextuelleTest {
    private String sortie(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(octets, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return octets.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Nom et état initial à la construction")
    void testConstructeur() {
        SondeTextuelle<Boolean> sonde = new SondeTextuelle<>("Bits");
        assertEquals("Bits", sonde.nom);
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Affichage des valeurs et mémorisation de l'information")
    void testRecevoirAffichageEtMemorisation() {
        SondeTextuelle<Boolean> sonde = new SondeTextuelle<>("Bits");
        Information<Boolean> info = new Information<>(new Boolean[]{true, false});
        assertEquals("Bits :  true false" + System.lineSeparator(), sortie(() -> sonde.recevoir(info)));
        assertSame(info, sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Remplacement de l'information précédente et acceptation d'autres types")
    void testRecevoirRemplacementInformation() {
        SondeTextuelle<String> sonde = new SondeTextuelle<>("Texte");
        Information<String> premier = new Information<>(new String[]{"bonjour"});
        Information<String> second = new Information<>(new String[]{"été"});
        String texte = sortie(() -> { sonde.recevoir(premier); sonde.recevoir(second); });
        assertEquals("Texte :  bonjour" + System.lineSeparator()
                + "Texte :  été" + System.lineSeparator(), texte);
        assertSame(second, sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Affichage et mémorisation d'une information vide")
    void testRecevoirInformationVide() {
        SondeTextuelle<Float> sonde = new SondeTextuelle<>("Vide");
        Information<Float> info = new Information<>();
        assertEquals("Vide : " + System.lineSeparator(), sortie(() -> sonde.recevoir(info)));
        assertSame(info, sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Effacement de la réception et affichage de null lors d'une réception nulle")
    void testRecevoirNull() {
        SondeTextuelle<Boolean> sonde = new SondeTextuelle<>("Bits");
        sortie(() -> sonde.recevoir(new Information<>(new Boolean[]{true})));
        assertEquals("Bits : null" + System.lineSeparator(), sortie(() -> sonde.recevoir(null)));
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Acceptation d'un élément null dans l'information")
    void testRecevoirElementNull() {
        SondeTextuelle<String> sonde = new SondeTextuelle<>(null);
        Information<String> info = new Information<>(new String[]{null});
        assertEquals("null :  null" + System.lineSeparator(), sortie(() -> sonde.recevoir(info)));
        assertSame(info, sonde.getInformationRecue());
    }
}
