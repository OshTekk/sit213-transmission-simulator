package visualisations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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

    @ParameterizedTest
    @ValueSource(ints = {0, 125, -50})
    @DisplayName("Position horizontale définie relue sans incrément")
    void testSetEtGetXPosition(int x) {
        Vue.setXPosition(x);
        assertEquals(x, Vue.getXPosition());
        assertEquals(x, Vue.getXPosition());
    }

    @Test
    @DisplayName("Incrémentation de la position verticale de 200 pixels à chaque lecture")
    void testGetYPositionIncremente() {
        Vue.resetPosition();
        assertEquals(0, Vue.getYPosition());
        assertEquals(200, Vue.getYPosition());
        assertEquals(400, Vue.getYPosition());
    }

    @Test
    @DisplayName("Réinitialisation de la position verticale avec conservation de la position horizontale")
    void testResetPosition() {
        Vue.setXPosition(75);
        Vue.getYPosition();
        Vue.getYPosition();
        Vue.resetPosition();
        Vue.resetPosition();
        assertEquals(0, Vue.getYPosition());
        assertEquals(75, Vue.getXPosition());
    }

    @Test
    @DisplayName("Fermeture répétée sans vue réinitialisant la position verticale")
    void testKillSansVue() {
        Vue.setXPosition(75);
        Vue.getYPosition();
        assertDoesNotThrow(Vue::kill);
        assertDoesNotThrow(Vue::kill);
        assertEquals(0, Vue.getYPosition());
        assertEquals(75, Vue.getXPosition());
    }

    @Test
    @Tag("graphique")
    @DisplayName("Titre conservé sans affichage à la construction")
    void testConstructeurTitre() throws Throwable {
        avecInterface(() -> {
            Vue vue = new Vue("Fenêtre test");
            assertEquals("Fenêtre test", vue.getTitle());
            assertFalse(vue.isVisible());
            assertNull(new Vue(null).getTitle());
        });
    }

    @Test
    @Tag("graphique")
    @DisplayName("Fermeture de toutes les vues et vidage du registre")
    void testKillFermeToutesLesVues() throws Throwable {
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
