package visualisations;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

@Tag("graphique")
class VueCourbeTest {
    @Test void constructeurAnalogiquePreparePointsBornesEtFenetre() throws Throwable {
        avecInterface(() -> {
            Vue.setXPosition(70);
            Vue.getYPosition();
            VueCourbe vue = new VueCourbe(new float[]{-2, 0, 3}, "Analogique");
            verifierCourbe(vue, -2, 3, 0, -2, 1, 0, 2, 3);
            assertEquals("Analogique", vue.getTitle());
            // La largeur minimale d'une fenêtre visible dépend du système.
            // La formule exacte est vérifiée sans affichage dans les tests paramétrés.
            assertEquals(200, vue.getHeight());
            assertEquals(70, vue.getX());
            assertEquals(200, vue.getY());
            assertTrue(vue.isVisible());
            assertEquals(Vue.EXIT_ON_CLOSE, vue.getDefaultCloseOperation());
        });
    }

    @Test void constructeurLogiquePrepareLesPaliersEtLaFenetre() throws Throwable {
        avecInterface(() -> {
            Vue.getYPosition(); // Évite le déplacement imposé par la barre de menus macOS.
            VueCourbe vue = new VueCourbe(new boolean[]{false, true}, 40, "Logique");
            verifierCourbe(vue, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 2, 1);
            assertEquals("Logique", vue.getTitle());
            assertEquals(90, vue.getWidth());
            assertEquals(200, vue.getHeight());
            assertEquals(0, vue.getX());
            assertEquals(200, vue.getY());
            assertTrue(vue.isVisible());
            assertEquals(Vue.EXIT_ON_CLOSE, vue.getDefaultCloseOperation());
        });
    }

    @ParameterizedTest @CsvSource({"989, 999", "990, 1000", "991, 1000"})
    void constructeurAnalogiquePlafonneLaLargeur(int longueur, int largeur) throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[longueur])) {
                assertEquals(largeur, vue.getWidth());
            }
        });
    }

    @ParameterizedTest @CsvSource({"98, 10, 990", "99, 10, 1000", "100, 10, 1000", "2, 0, 10"})
    void constructeurLogiqueCalculeEtPlafonneLaLargeur(int longueur, int pixels, int largeur) throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new boolean[longueur], pixels)) {
                assertEquals(largeur, vue.getWidth());
            }
        });
    }

    @Test void constructeursRejettentActuellementUnTableauNull() throws Throwable {
        avecInterface(() -> {
            assertThrows(NullPointerException.class, () -> new VueCourbe((float[]) null, "Erreur"));
            assertThrows(NullPointerException.class, () -> new VueCourbe((boolean[]) null, 20, "Erreur"));
        });
    }

    @Test void changerAnalogiqueRemplaceLesPointsEtRecalculeLesBornes() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{-10, 20})) {
                vue.changer(new float[]{1, 2, 1});
                verifierCourbe(vue, 0, 2, 0, 1, 1, 2, 2, 1);
                vue.changer(new float[]{-1, -3});
                verifierCourbe(vue, -3, 0, 0, -1, 1, -3);
            }
        });
    }

    @Test void changerLogiqueRemplaceLesPaliersEtReinitialiseLesBornes() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{-10, 20})) {
                vue.changer(new boolean[]{true, false});
                verifierCourbe(vue, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 2, 0);
                vue.changer(new boolean[]{false});
                verifierCourbe(vue, 0, 1, 0, 0, 0, 0, 1, 0);
            }
        });
    }

    @Test void changerNullEchoueEtConserveLaCourbePrecedente() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{-1, 1})) {
                assertThrows(NullPointerException.class, () -> vue.changer((float[]) null));
                verifierCourbe(vue, -1, 1, 0, -1, 1, 1);
                assertThrows(NullPointerException.class, () -> vue.changer((boolean[]) null));
                verifierCourbe(vue, -1, 1, 0, -1, 1, 1);
            }
        });
    }

    @Test void changerLogiqueAccepteUnTableauVide() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new boolean[]{true}, 20)) {
                assertDoesNotThrow(() -> vue.changer(new boolean[0]));
                verifierCourbe(vue, 0, 1, 0, 0);
            }
        });
    }

    @Test void changerAnalogiqueVideEchoueActuellementLorsDuDessin() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{-1, 1})) {
                assertThrows(ArrayIndexOutOfBoundsException.class, () -> vue.changer(new float[0]));
                verifierCourbe(vue, 0, 0);
            }
        });
    }

    @Test void paintSurUneCourbeAnalogiqueVideEchoueActuellement() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[0])) {
                verifierCourbe(vue, 0, 0);
                assertThrows(ArrayIndexOutOfBoundsException.class, vue::paint);
            }
        });
    }

    @Test void paintAvecGraphicsNullNeDessineRien() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{-1, 1})) {
                vue.fond.setRGB(0, 0, Color.MAGENTA.getRGB());
                assertDoesNotThrow(() -> vue.paint(null));
                assertEquals(Color.MAGENTA.getRGB(), vue.fond.getRGB(0, 0));
                assertEquals(Color.WHITE.getRGB(), vue.trace.getRGB(10, 10));
            }
        });
    }

    @Test void paintDessineLeFondLesAxesEtLaCourbeMixte() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{-1, 1, -1})) {
                vue.paint(vue.getGraphics());
                assertEquals(Color.WHITE.getRGB(), vue.fond.getRGB(0, 0));
                assertEquals(Color.BLACK.getRGB(), vue.trace.getRGB(50, 60), "Axe horizontal");
                assertEquals(Color.BLACK.getRGB(), vue.trace.getRGB(10, 30), "Axe vertical");
                assertEquals(Color.BLACK.getRGB(), vue.trace.getRGB(35, 85), "Segment montant");
                assertEquals(Color.BLACK.getRGB(), vue.trace.getRGB(185, 85), "Segment descendant");
                assertEquals(Color.WHITE.getRGB(), vue.trace.getRGB(35, 80), "En dehors de la courbe");
            }
        });
    }

    @ParameterizedTest @CsvSource({"1, 2, 0, 2, 10", "-1, -2, -2, 0, 110", "0, 0, 0, 0, 10"})
    void paintSansArgumentDessineLesSignauxPositifsNegatifsEtNuls(
            float premier, float milieu, float min, float max, int yMilieu) throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran vue = new CourbeHorsEcran(new float[]{premier, milieu, premier})) {
                verifierCourbe(vue, min, max, 0, premier, 1, milieu, 2, premier);
                assertDoesNotThrow(() -> { vue.paint(); });
                assertEquals(Color.BLACK.getRGB(), vue.trace.getRGB(110, yMilieu));
            }
        });
    }

    @Test void paintAccepteActuellementUnSeulEchantillonEtUneCourbeLogiqueVide() throws Throwable {
        avecInterface(() -> {
            try (CourbeHorsEcran analogique = new CourbeHorsEcran(new float[]{2});
                 CourbeHorsEcran logique = new CourbeHorsEcran(new boolean[0], 20)) {
                verifierCourbe(analogique, 0, 2, 0, 2);
                verifierCourbe(logique, 0, 1, 0, 0);
                assertDoesNotThrow(() -> { analogique.paint(); });
                assertDoesNotThrow(() -> { logique.paint(); });
            }
        });
    }
}
