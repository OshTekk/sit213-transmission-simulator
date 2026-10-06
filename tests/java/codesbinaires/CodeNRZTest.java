package codesbinaires;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeNRZTest extends CodeBinaireTest {

    @BeforeEach
    void setUp() {
        nbEch = 10;
        ampliMin = -5.0f;
        ampliMax = 5.0f;
        codeur = new CodeNRZ(nbEch, ampliMin, ampliMax);
    }

    @Test
    @DisplayName("Rejet des amplitudes et nombres d'échantillons invalides")
    void testConstructeurParametresInvalides() {
        assertThrows(AmplitudeInvalideException.class, () -> new CodeNRZ(nbEch, 1.0f, ampliMax));
        assertThrows(AmplitudeInvalideException.class, () -> new CodeNRZ(nbEch, ampliMin, -1.0f));
        assertThrows(AmplitudeInvalideException.class, () -> new CodeNRZ(nbEch, 0.0f, 0.0f));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeNRZ(-3, ampliMin, ampliMax));
    }
}