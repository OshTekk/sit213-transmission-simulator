package visualisations;

import information.Information;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static visualisations.SupportVisualisations.*;

class SondePuissanceTest {
    @Test
    @DisplayName("Nom et état initial à la construction")
    void testConstructeur() {
        SondePuissance sonde = new SondePuissance("Puissance");
        assertEquals("Puissance", sonde.nom);
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @Tag("graphique")
    @DisplayName("Affichage de la moyenne des carrés et mise à jour de la réception")
    void testRecevoirSignal() throws Throwable {
        avecInterface(() -> {
            SondePuissance sonde = new SondePuissance("Puissance");
            Information<Float> premier = new Information<>(new Float[]{-2f, 1f, 3f});
            sonde.recevoir(premier);
            assertSame(premier, sonde.getInformationRecue());
            VueValeur vue = vueUnique(VueValeur.class);
            assertEquals("Puissance", vue.getTitle());
            assertEquals(14.0 / 3, Double.parseDouble(texte(vue).trim()), 1e-12);
            Vue.kill();
            Information<Float> second = new Information<>(new Float[]{0f, 0f});
            sonde.recevoir(second);
            assertSame(second, sonde.getInformationRecue());
            assertEquals(" 0.0", texte(vueUnique(VueValeur.class)));
        });
    }

    @ParameterizedTest
    @CsvSource({"-0.5, 0.25", "0.0, 0.0", "2.0, 4.0"})
    @Tag("graphique")
    @DisplayName("Puissance d'un échantillon unique égale à son carré")
    void testRecevoirUnSeulEchantillon(float valeur, double puissance) throws Throwable {
        avecInterface(() -> {
            new SondePuissance("P").recevoir(new Information<>(new Float[]{valeur}));
            assertEquals(puissance, Double.parseDouble(texte(vueUnique(VueValeur.class)).trim()), 1e-12);
        });
    }

    @Test
    @Tag("graphique")
    @DisplayName("Affichage de NaN pour une information vide")
    void testRecevoirInformationVide() throws Throwable {
        avecInterface(() -> {
            SondePuissance sonde = new SondePuissance("P");
            Information<Float> vide = new Information<>();
            sonde.recevoir(vide);
            assertSame(vide, sonde.getInformationRecue());
            assertEquals(" NaN", texte(vueUnique(VueValeur.class)));
        });
    }

    @Test
    @DisplayName("Rejet d'une réception nulle")
    void testRecevoirNull() {
        SondePuissance sonde = new SondePuissance("P");
        assertThrows(NullPointerException.class, () -> sonde.recevoir(null));
        assertNull(sonde.getInformationRecue());
    }

    @Test
    @DisplayName("Échec sur un élément nul dans l'information")
    void testRecevoirElementNull() {
        SondePuissance sonde = new SondePuissance("P");
        Information<Float> info = new Information<>(new Float[]{1f, null});
        assertThrows(NullPointerException.class, () -> sonde.recevoir(info));
        assertSame(info, sonde.getInformationRecue());
    }
}
