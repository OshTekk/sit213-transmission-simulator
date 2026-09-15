package codesbinaires;

import information.Information;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeRZTest extends CodeBinaireTest {
    @BeforeEach
    void setUp() {
        nbEch = 12;
        ampliMin = 0.0f;
        ampliMax = 5.0f;
        codeur = new CodeRZ(nbEch, ampliMin, ampliMax);
    }

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
        assertThrows(NbEchantillonInvalideException.class, () -> {
            new CodeRZ(11, ampliMin, ampliMax);
        });
    }

}