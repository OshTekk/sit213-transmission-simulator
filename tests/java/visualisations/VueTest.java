package visualisations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

class VueTest {
    @AfterEach void nettoyerPositions() {
        Vue.resetPosition();
        Vue.setXPosition(0);
    }

    /** Vérifie que la position horizontale définie est relue sans incrément. */
    @ParameterizedTest @ValueSource(ints = {0, 125, -50})
    void setEtGetXPositionConserventLaValeurSansIncrement(int x) {
        Vue.setXPosition(x);
        assertEquals(x, Vue.getXPosition());
        assertEquals(x, Vue.getXPosition());
    }

    /** Vérifie que chaque lecture de la position verticale avance de deux cents pixels. */
    @Test void getYPositionAvanceDeDeuxCentsPixels() {
        Vue.resetPosition();
        assertEquals(0, Vue.getYPosition());
        assertEquals(200, Vue.getYPosition());
        assertEquals(400, Vue.getYPosition());
    }

    /** Vérifie que la remise à zéro réinitialise la position verticale et conserve l'horizontale. */
    @Test void resetPositionReinitialiseYEtConserveX() {
        Vue.setXPosition(75);
        Vue.getYPosition();
        Vue.getYPosition();
        Vue.resetPosition();
        Vue.resetPosition();
        assertEquals(0, Vue.getYPosition());
        assertEquals(75, Vue.getXPosition());
    }

    /** Vérifie qu'une fermeture répétée sans vue réinitialise la position verticale. */
    @Test void killSansVueEstRepetableEtReinitialiseY() {
        Vue.setXPosition(75);
        Vue.getYPosition();
        assertDoesNotThrow(Vue::kill);
        assertDoesNotThrow(Vue::kill);
        assertEquals(0, Vue.getYPosition());
        assertEquals(75, Vue.getXPosition());
    }

    /** Vérifie que le titre est conservé sans afficher la fenêtre à la construction. */
    @Test @Tag("graphique")
    void constructeurConserveLeTitreSansAfficherLaVue() throws Throwable {
        avecInterface(() -> {
            Vue vue = new Vue("Fenêtre test");
            assertEquals("Fenêtre test", vue.getTitle());
            assertFalse(vue.isVisible());
            assertNull(new Vue(null).getTitle());
        });
    }

    /** Vérifie que la fermeture supprime toutes les vues et vide leur registre. */
    @Test @Tag("graphique")
    void killFermeToutesLesVuesEtVideLeRegistre() throws Throwable {
        avecInterface(() -> {
            class VueComptee extends Vue {
                int fermetures;
                VueComptee() { super("Test fermeture"); pack(); }
                @Override public void dispose() { fermetures++; super.dispose(); }
            }
            VueComptee premiere = new VueComptee();
            VueComptee seconde = new VueComptee();
            assertTrue(premiere.isDisplayable());
            assertTrue(seconde.isDisplayable());
            Vue.setXPosition(50);
            Vue.getYPosition();
            Vue.kill();
            assertFalse(premiere.isDisplayable());
            assertFalse(seconde.isDisplayable());
            assertEquals(0, Vue.getYPosition());
            assertEquals(50, Vue.getXPosition());
            Vue.kill();
            assertEquals(1, premiere.fermetures);
            assertEquals(1, seconde.fermetures);
        });
    }
}
