package codesbinaires;

import information.Information;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeNRZTest extends CodeBinaireTest {

    private Information<Boolean> info;
    private CodeNRZ codeNRZObject;

    @BeforeEach
    void setUp() {
        nbEch = 12;
        ampliMin = -5.0f;
        ampliMax = 5.0f;
        codeur = new CodeNRZ(nbEch, ampliMin, ampliMax);
    }

    /** Vérifie que le constructeur NRZ refuse les amplitudes et les nombres d'échantillons invalides. */
    @DisplayName("Vérification de la levée d'exception en cas d'amplitude invalide")
    @Test
    void testConstructeur() {
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeNRZ(nbEch, 1.0f, ampliMax);
        });
        assertThrows(AmplitudeInvalideException.class, () -> {
            new CodeNRZ(nbEch, ampliMin, -1.0f);
        });
        assertThrows(AmplitudeInvalideException.class, () -> {
           new CodeNRZ(nbEch, 0.0f, 0.0f);
        });
        assertThrows(NbEchantillonInvalideException.class, () -> {
            new CodeNRZ(-3, ampliMin, ampliMax);
        });
    }

}