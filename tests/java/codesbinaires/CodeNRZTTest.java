package codesbinaires;

import information.Information;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeNRZTTest extends CodeBinaireTest {
    @BeforeEach
    void setUp() {
        nbEch = 12;
        ampliMin = -5.0f;
        ampliMax = 5.0f;
        codeur = new CodeNRZT(nbEch, ampliMin, ampliMax);
    }

    @DisplayName("Vérification de la levée d'exception en cas de nbEch invalide")
    @Test
    void testNbEchantillonNonMultiple3() {
        assertThrows(NbEchantillonInvalideException.class, () -> {
            new CodeNRZT(11, ampliMin, ampliMax);
        });
    }

    @DisplayName("Vérification de la levée d'exception en cas d'amplitude invalide")
    @Test
    void testAmplitudeInvalide() {
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeNRZT(nbEch, 1.0f, ampliMax);
        });
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeNRZT(nbEch, ampliMin, -1.0f);
        });
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeNRZT(nbEch, 0.0f, 0.0f);
        });
        assertThrows(NbEchantillonInvalideException.class, () -> {
            new CodeNRZT(-3, ampliMin, ampliMax);
        });
        assertThrows(NbEchantillonInvalideException.class, () -> {
            new CodeNRZT(11, ampliMin, ampliMax);
        });
    }

}