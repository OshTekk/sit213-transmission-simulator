package visualisations;

import org.junit.jupiter.api.function.Executable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Outillage de test uniquement : EDT, fermeture des vues et observation des courbes. */
final class SupportVisualisations {
    private SupportVisualisations() { }

    static void avecInterface(Executable test) throws Throwable {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Ce test nécessite une interface graphique");
        AtomicReference<Throwable> erreur = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Vue.kill();
                Vue.setXPosition(0);
                test.execute();
            } catch (Throwable t) {
                erreur.set(t);
            } finally {
                // dispose(), sans envoyer WINDOW_CLOSING (les vues utilisent EXIT_ON_CLOSE).
                Vue.kill();
                Vue.setXPosition(0);
            }
        });
        if (erreur.get() != null) throw erreur.get();
    }

    static <T extends Vue> T vueUnique(Class<T> type) {
        var vues = Arrays.stream(Frame.getFrames())
                .filter(Frame::isDisplayable).filter(type::isInstance).toList();
        assertEquals(1, vues.size(), "Une seule vue de ce type doit être ouverte");
        return type.cast(vues.get(0));
    }

    static String texte(VueValeur vue) {
        assertEquals(1, vue.getContentPane().getComponentCount());
        return assertInstanceOf(JLabel.class, vue.getContentPane().getComponent(0)).getText();
    }

    static Object champ(VueCourbe vue, String nom) throws ReflectiveOperationException {
        Field champ = VueCourbe.class.getDeclaredField(nom);
        champ.setAccessible(true);
        return champ.get(vue);
    }

    static void verifierCourbe(VueCourbe vue, float min, float max, float... xy)
            throws ReflectiveOperationException {
        Point2D.Float[] points = (Point2D.Float[]) champ(vue, "coordonnees");
        assertEquals(xy.length / 2, points.length);
        for (int i = 0; i < points.length; i++) {
            assertEquals(xy[2 * i], points[i].x, 0.00001f, "x du point " + i);
            assertEquals(xy[2 * i + 1], points[i].y, 0.00001f, "y du point " + i);
        }
        assertEquals(min, (Float) champ(vue, "yMin"), 0.00001f);
        assertEquals(max, (Float) champ(vue, "yMax"), 0.00001f);
    }

    /**
     * Exécute les vrais constructeurs/changer/paint de VueCourbe sur des images.
     * Seuls l'affichage et les repeints asynchrones sont neutralisés. JFrame
     * nécessite toujours un environnement graphique, même sans fenêtre visible.
     */
    static final class CourbeHorsEcran extends VueCourbe implements AutoCloseable {
        final BufferedImage fond = new BufferedImage(240, 160, BufferedImage.TYPE_INT_RGB);
        final BufferedImage trace = new BufferedImage(220, 120, BufferedImage.TYPE_INT_RGB);
        private final Graphics graphicsFond = fond.getGraphics();
        private final Graphics graphicsTrace = trace.getGraphics();

        CourbeHorsEcran(float[] valeurs) {
            super(valeurs, "Test du dessin");
            preparer();
        }

        CourbeHorsEcran(boolean[] valeurs, int nbPixels) {
            super(valeurs, nbPixels, "Test du dessin");
            preparer();
        }

        private void preparer() {
            graphicsTrace.setColor(Color.WHITE);
            graphicsTrace.fillRect(0, 0, trace.getWidth(), trace.getHeight());
            graphicsTrace.setColor(Color.BLACK);
            JPanel panneau = new JPanel() {
                @Override public Graphics getGraphics() { return graphicsTrace; }
            };
            setContentPane(panneau);
            panneau.setSize(220, 120);
        }

        @Override public void setVisible(boolean visible) { }
        @Override public void repaint() { }
        @Override public Graphics getGraphics() { return graphicsFond; }

        @Override public void close() {
            graphicsFond.dispose();
            graphicsTrace.dispose();
            dispose();
        }
    }
}
