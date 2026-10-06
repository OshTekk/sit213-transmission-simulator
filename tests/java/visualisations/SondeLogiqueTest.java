package visualisations;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

class SondeLogiqueTest {
    @Test
    @DisplayName("Nom et état initial à la construction")
    void testConstructeur() {
        SondeLogique sonde = new SondeLogique("Bits", 20);
        assertEquals("Bits", sonde.nom);
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @Tag("graphique")
    @DisplayName("Conversion des bits en courbe avec la largeur demandée")
    void testRecevoirBits() throws Throwable {
        avecInterface(() -> {
            SondeLogique sonde = new SondeLogique("Bits", 50);
            Information<Boolean> premier = new Information<>(new Boolean[]{true, false});
            sonde.recevoir(premier);
            assertSame(premier, sonde.getInformationRecue());
            VueCourbe vue = vueUnique(VueCourbe.class);
            assertEquals("Bits", vue.getTitle());
            assertEquals(110, vue.getWidth());
            verifierCourbe(vue, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 2, 0);
            Vue.kill();
            Information<Boolean> second = new Information<>(new Boolean[]{false, true});
            sonde.recevoir(second);
            assertSame(second, sonde.getInformationRecue());
            verifierCourbe(vueUnique(VueCourbe.class), 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 2, 1);
        });
    }

    @Test
    @Tag("graphique")
    @DisplayName("Réception vide ne conservant que le point initial")
    void testRecevoirBitsVide() throws Throwable {
        avecInterface(() -> {
            SondeLogique sonde = new SondeLogique("Vide", 20);
            Information<Boolean> vide = new Information<>();
            sonde.recevoir(vide);
            assertSame(vide, sonde.getInformationRecue());
            verifierCourbe(vueUnique(VueCourbe.class), 0, 1, 0, 0);
        });
    }

    @Test
    @DisplayName("Rejet d'une réception nulle")
    void testRecevoirNull() {
        SondeLogique sonde = new SondeLogique("Bits", 20);
        assertThrows(NullPointerException.class, () -> sonde.recevoir(null));
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Échec sur un bit nul dans l'information")
    void testRecevoirElementNull() {
        SondeLogique sonde = new SondeLogique("Bits", 20);
        Information<Boolean> info = new Information<>(new Boolean[]{true, null});
        assertThrows(NullPointerException.class, () -> sonde.recevoir(info));
        assertSame(info, sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Comportement avec une largeur nulle ou négative")
    void testConstructeurLargeurNulleOuNegative() {
        assertDoesNotThrow(() -> new SondeLogique("Bits", 0));
        assertDoesNotThrow(() -> new SondeLogique("Bits", -1));
    }
}
