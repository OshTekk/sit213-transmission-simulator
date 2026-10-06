package codesbinaires;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeNRZTTest extends CodeBinaireTest {
    @BeforeEach
    void setUp() {
        nbEch = 10;
        ampliMin = -5.0f;
        ampliMax = 5.0f;
        codeur = new CodeNRZT(nbEch, ampliMin, ampliMax);
    }

    @Test
    @DisplayName("Rejet des amplitudes et nombres d'échantillons invalides")
    void testConstructeurParametresInvalides() {
        assertThrows(AmplitudeInvalideException.class, () -> new CodeNRZT(nbEch, 1.0f, ampliMax));
        assertThrows(AmplitudeInvalideException.class, () -> new CodeNRZT(nbEch, ampliMin, -1.0f));
        assertThrows(AmplitudeInvalideException.class, () -> new CodeNRZT(nbEch, 0.0f, 0.0f));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeNRZT(-3, ampliMin, ampliMax));
    }

    @Test
    @DisplayName("Rejet d'un nombre d'échantillons par bit strictement inférieur à 3")
    void testNbEchInferieurATrois() {
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeNRZT(0, ampliMin, ampliMax));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeNRZT(1, ampliMin, ampliMax));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeNRZT(2, ampliMin, ampliMax));
        assertDoesNotThrow(() -> new CodeNRZT(3, ampliMin, ampliMax));
    }
}