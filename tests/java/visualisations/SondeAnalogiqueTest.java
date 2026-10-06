package visualisations;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

class SondeAnalogiqueTest {
    @Test
    @DisplayName("Nom et état initial à la construction")
    void testConstructeur() {
        SondeAnalogique sonde = new SondeAnalogique("Signal");
        assertEquals("Signal", sonde.nom);
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @Tag("graphique")
    @DisplayName("Conversion des échantillons en courbe et mise à jour de la réception")
    void testRecevoirSignal() throws Throwable {
        avecInterface(() -> {
            SondeAnalogique sonde = new SondeAnalogique("Signal");
            Information<Float> premier = new Information<>(new Float[]{-2f, 0f, 3f});
            sonde.recevoir(premier);
            assertSame(premier, sonde.getInformationRecue());
            VueCourbe vue = vueUnique(VueCourbe.class);
            assertEquals("Signal", vue.getTitle());
            verifierCourbe(vue, -2, 3, 0, -2, 1, 0, 2, 3);
            Vue.kill();
            Information<Float> second = new Information<>(new Float[]{1f, -1f});
            sonde.recevoir(second);
            assertSame(second, sonde.getInformationRecue());
            verifierCourbe(vueUnique(VueCourbe.class), -1, 1, 0, 1, 1, -1);
        });
    }

    @Test
    @Tag("graphique")
    @DisplayName("Création d'une courbe sur signal vide")
    void testRecevoirSignalVide() throws Throwable {
        avecInterface(() -> {
            SondeAnalogique sonde = new SondeAnalogique("Vide");
            Information<Float> vide = new Information<>();
            sonde.recevoir(vide);
            assertSame(vide, sonde.getInformationRecue());
            VueCourbe vue = vueUnique(VueCourbe.class);
            verifierCourbe(vue, 0, 0);
            // Appel synchrone : l'erreur ne doit pas se perdre dans un repaint asynchrone.
            assertThrows(ArrayIndexOutOfBoundsException.class, vue::paint);
        });
    }

    @Test
    @DisplayName("Rejet d'une réception nulle")
    void testRecevoirNull() {
        SondeAnalogique sonde = new SondeAnalogique("Signal");
        assertThrows(NullPointerException.class, () -> sonde.recevoir(null));
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Échec sur un élément nul dans l'information")
    void testRecevoirElementNull() {
        SondeAnalogique sonde = new SondeAnalogique("Signal");
        Information<Float> info = new Information<>(new Float[]{1f, null});
        assertThrows(NullPointerException.class, () -> sonde.recevoir(info));
        assertSame(info, sonde.getInformationRecue());
    }
}
