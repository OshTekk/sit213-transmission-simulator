package visualisations;

import information.Information;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

class SondeLogiqueTest {
    @Test void constructeurConserveNomEtEtatInitial() {
        SondeLogique sonde = new SondeLogique("Bits", 20);
        assertEquals("Bits", sonde.nom);
        assertNull(sonde.getInformationRecue());
    }

    @Test @Tag("graphique")
    void recevoirConvertitLesBitsEtTransmetLaLargeur() throws Throwable {
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

    @Test @Tag("graphique")
    void recevoirVideConserveSeulementLePointInitial() throws Throwable {
        avecInterface(() -> {
            SondeLogique sonde = new SondeLogique("Vide", 20);
            Information<Boolean> vide = new Information<>();
            sonde.recevoir(vide);
            assertSame(vide, sonde.getInformationRecue());
            verifierCourbe(vueUnique(VueCourbe.class), 0, 1, 0, 0);
        });
    }

    @Test void recevoirNullLeveUneNullPointerException() {
        SondeLogique sonde = new SondeLogique("Bits", 20);
        assertThrows(NullPointerException.class, () -> sonde.recevoir(null));
        assertNull(sonde.getInformationRecue());
    }

    @Test void recevoirUnElementNullEchoueApresMemorisation() {
        SondeLogique sonde = new SondeLogique("Bits", 20);
        Information<Boolean> info = new Information<>(new Boolean[]{true, null});
        assertThrows(NullPointerException.class, () -> sonde.recevoir(info));
        assertSame(info, sonde.getInformationRecue());
    }

    @Test void constructeurAccepteActuellementUneLargeurNulleOuNegative() {
        assertDoesNotThrow(() -> new SondeLogique("Bits", 0));
        assertDoesNotThrow(() -> new SondeLogique("Bits", -1));
    }
}
