package codesbinaires;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CodeRZTest extends CodeBinaireTest {
    @BeforeEach
    void setUp() {
        nbEch = 10;
        ampliMin = 0.0f;
        ampliMax = 5.0f;
        codeur = new CodeRZ(nbEch, ampliMin, ampliMax);
    }

    @Test
    @DisplayName("Rejet des amplitudes et nombres d'échantillons invalides")
    void testConstructeurParametresInvalides() {
        assertThrows(AmplitudeInvalideException.class, () -> new CodeRZ(nbEch, 1.0f, ampliMax));
        assertThrows(AmplitudeInvalideException.class, () -> new CodeRZ(nbEch, ampliMin, ampliMin));
        assertThrows(AmplitudeInvalideException.class, () -> new CodeRZ(nbEch, ampliMin, -1.0f));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(-3, ampliMin, ampliMax));
    }

    @Test
    @DisplayName("Rejet d'un nombre d'échantillons par bit strictement inférieur à 3")
    void testNbEchInferieurATrois() {
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(0, ampliMin, ampliMax));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(1, ampliMin, ampliMax));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(2, ampliMin, ampliMax));
        assertDoesNotThrow(() -> new CodeRZ(3, ampliMin, ampliMax));
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 10, 11, 12, 13, 14})
    @DisplayName("Invariance encodage puis décodage pour différents nombres d'échantillons (modulo 3)")
    void testInvarianceModuloTrois(int nbEchTest) throws Exception {
        CodeRZ code = new CodeRZ(nbEchTest, ampliMin, ampliMax);
        information.Information<Boolean> info = new information.Information<>(boolTestChain);
        information.Information<Float> sign = code.encoder(info);
        assertEquals(info.nbElements() * nbEchTest, sign.nbElements());
        information.Information<Boolean> decode = code.decoder(sign);
        assertEquals(info, decode);
    }
}