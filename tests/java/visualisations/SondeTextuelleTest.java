package visualisations;

import information.Information;
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

    /** Vérifie le nom et l'absence d'information reçue à la construction. */
    @Test void constructeurConserveNomEtEtatInitial() {
        SondeTextuelle<Boolean> sonde = new SondeTextuelle<>("Bits");
        assertEquals("Bits", sonde.nom);
        assertNull(sonde.getInformationRecue());
    }

    /** Vérifie l'affichage des valeurs et la mémorisation de la même information. */
    @Test void recevoirAfficheLesValeursEtMemoriseLaMemeInformation() {
        SondeTextuelle<Boolean> sonde = new SondeTextuelle<>("Bits");
        Information<Boolean> info = new Information<>(new Boolean[]{true, false});
        assertEquals("Bits :  true false" + System.lineSeparator(), sortie(() -> sonde.recevoir(info)));
        assertSame(info, sonde.getInformationRecue());
    }

    /** Vérifie qu'une nouvelle réception remplace l'ancienne, même avec un autre type. */
    @Test void recevoirRemplaceLaDerniereInformationEtAccepteUnAutreType() {
        SondeTextuelle<String> sonde = new SondeTextuelle<>("Texte");
        Information<String> premier = new Information<>(new String[]{"bonjour"});
        Information<String> second = new Information<>(new String[]{"été"});
        String texte = sortie(() -> { sonde.recevoir(premier); sonde.recevoir(second); });
        assertEquals("Texte :  bonjour" + System.lineSeparator()
                + "Texte :  été" + System.lineSeparator(), texte);
        assertSame(second, sonde.getInformationRecue());
    }

    /** Vérifie qu'une information vide peut être affichée et mémorisée. */
    @Test void recevoirAccepteUneInformationVide() {
        SondeTextuelle<Float> sonde = new SondeTextuelle<>("Vide");
        Information<Float> info = new Information<>();
        assertEquals("Vide : " + System.lineSeparator(), sortie(() -> sonde.recevoir(info)));
        assertSame(info, sonde.getInformationRecue());
    }

    /** Caractérise l'effacement et l'affichage de null lors d'une réception nulle. */
    @Test void recevoirNullEffaceActuellementLaReceptionEtAfficheNull() {
        SondeTextuelle<Boolean> sonde = new SondeTextuelle<>("Bits");
        sortie(() -> sonde.recevoir(new Information<>(new Boolean[]{true})));
        assertEquals("Bits : null" + System.lineSeparator(), sortie(() -> sonde.recevoir(null)));
        assertNull(sonde.getInformationRecue());
    }

    /** Caractérise l'acceptation actuelle d'un élément nul dans l'information. */
    @Test void recevoirAccepteActuellementUnElementNull() {
        SondeTextuelle<String> sonde = new SondeTextuelle<>(null);
        Information<String> info = new Information<>(new String[]{null});
        assertEquals("null :  null" + System.lineSeparator(), sortie(() -> sonde.recevoir(info)));
        assertSame(info, sonde.getInformationRecue());
    }
}
