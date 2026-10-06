package visualisations;

import information.Information;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Vérifie la limitation d'environnement, sans la confondre avec un test du dessin. */
@Tag("sans-ecran")
class VisualisationsSansEcranTest {
    @BeforeEach void sansEcranUniquement() {
        assumeTrue(GraphicsEnvironment.isHeadless(), "Ce test nécessite le mode sans écran");
    }

    @Test
    @DisplayName("Échec des constructeurs de vues en environnement sans écran (HeadlessException)")
    void testConstructeursVuesSansEcran() {
        assertThrows(HeadlessException.class, () -> new Vue("Vue"));
        assertThrows(HeadlessException.class, () -> new VueValeur(1, "Valeur"));
        assertThrows(HeadlessException.class, () -> new VueCourbe(new float[]{0, 1}, "Analogique"));
        assertThrows(HeadlessException.class, () -> new VueCourbe(new boolean[]{true}, 20, "Logique"));
    }

    @Test
    @DisplayName("Mémorisation de l'information par les sondes avant l'échec d'affichage sans écran")
    void testSondesMemorisentSansEcran() {
        Information<Float> analogique = new Information<>(new Float[]{-1f, 1f});
        Information<Boolean> logique = new Information<>(new Boolean[]{true, false});
        SondeAnalogique sondeAnalogique = new SondeAnalogique("A");
        SondeLogique sondeLogique = new SondeLogique("L", 20);
        SondePuissance sondePuissance = new SondePuissance("P");
        assertThrows(HeadlessException.class, () -> sondeAnalogique.recevoir(analogique));
        assertThrows(HeadlessException.class, () -> sondeLogique.recevoir(logique));
        assertThrows(HeadlessException.class, () -> sondePuissance.recevoir(analogique));
        assertSame(analogique, sondeAnalogique.getInformationRecue());
        assertSame(logique, sondeLogique.getInformationRecue());
        assertSame(analogique, sondePuissance.getInformationRecue());
    }
}
