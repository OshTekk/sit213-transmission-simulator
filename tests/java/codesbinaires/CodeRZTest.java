package codesbinaires;

import information.Information;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeRZTest extends CodeBinaireTest {
    @BeforeEach
    void setUp() {
        nbEch = 10;
        ampliMin = 0.0f;
        ampliMax = 5.0f;
        codeur = new CodeRZ(nbEch, ampliMin, ampliMax);
    }

    /** Vérifie que le constructeur RZ impose une amplitude minimale nulle et un nombre d'échantillons valide. */
    @DisplayName("Vérification de la levée d'exception en cas de paramètre constructeur invalide")
    @Test
    void testConstructeur() {
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeRZ(nbEch, 1.0f, ampliMax);
        });
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeRZ(nbEch, ampliMin, ampliMin);
        });
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeRZ(nbEch, ampliMin, -1.0f);
        });
        assertThrows(NbEchantillonInvalideException.class, () -> {
            new CodeRZ(-3, ampliMin, ampliMax);
        });
    }

    /** Vérifie le rejet des nombres d'échantillons par bit strictement inférieurs à 3. */
    @DisplayName("Test de rejet si nbEchParBit < 3")
    @Test
    void testNbEchantillonInferieurATrois() {
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(0, ampliMin, ampliMax));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(1, ampliMin, ampliMax));
        assertThrows(NbEchantillonInvalideException.class, () -> new CodeRZ(2, ampliMin, ampliMax));
        assertDoesNotThrow(() -> new CodeRZ(3, ampliMin, ampliMax));
    }

}