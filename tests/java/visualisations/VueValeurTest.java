package visualisations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

@Tag("graphique")
class VueValeurTest {
    @Test
    @DisplayName("Affichage de la valeur, du titre et des dimensions de la fenêtre")
    void testConstructeurValeurTitreDimensions() throws Throwable {
        avecInterface(() -> {
            Vue.setXPosition(80);
            Vue.getYPosition(); // Place la fenêtre sous la barre de menus du système.
            VueValeur vue = new VueValeur(2.5, "Mesure");
            assertEquals(" 2.5", texte(vue));
            assertEquals("Mesure", vue.getTitle());
            assertEquals(300, vue.getWidth());
            assertEquals(100, vue.getHeight());
            assertEquals(80, vue.getX());
            assertEquals(200, vue.getY());
            assertTrue(vue.isVisible());
            assertEquals(Vue.EXIT_ON_CLOSE, vue.getDefaultCloseOperation());
            VueValeur suivante = new VueValeur("fin", "Autre mesure");
            assertEquals(" fin", texte(suivante));
            assertEquals(400, suivante.getY());
        });
    }

    @Test
    @DisplayName("Comportement avec une valeur et un nom null")
    void testConstructeurValeurEtNomNull() throws Throwable {
        avecInterface(() -> {
            VueValeur vue = assertDoesNotThrow(() -> new VueValeur(null, null));
            assertEquals(" null", texte(vue));
            assertNull(vue.getTitle());
        });
    }

    @Test
    @DisplayName("Propagation d'une erreur de conversion de valeur en texte")
    void testConstructeurErreurConversionTexte() throws Throwable {
        avecInterface(() -> {
            IllegalStateException erreur = new IllegalStateException("Conversion impossible");
            Object valeur = new Object() {
                @Override public String toString() { throw erreur; }
            };
            assertSame(erreur, assertThrows(IllegalStateException.class,
                    () -> new VueValeur(valeur, "Erreur")));
        });
    }
}
