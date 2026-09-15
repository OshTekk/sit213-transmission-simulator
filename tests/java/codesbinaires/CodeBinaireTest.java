package codesbinaires;

import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

abstract class CodeBinaireTest {
    protected CodeBinaire codeur;
    protected int nbEch;
    protected float ampliMax, ampliMin;
    protected static final Boolean[] boolTestChain = {true, false, true, true, true, false, false, false, true, false};
    protected static final Float[] invalidSignalChain = {0.0f, 1.5f, 3.0f, 1.5f, 0.0f};

    @BeforeEach
    abstract void setUp();

    @DisplayName("Test d'invariance (boolean -> encoder -> float -> decoder -> boolean)")
    @Test
    void testInvariance() throws InformationNonConformeException {
        Information<Boolean> info = new Information<>(boolTestChain);
        Information<Float> signal = codeur.encoder(info);
        Information<Boolean> infoDecode = codeur.decoder(signal);
        assertEquals(info, infoDecode, "Erreur, l'information doit être identique à l'émission.");
    }

    @DisplayName("Test décodage signal invalide")
    @Test
    void testSignalInvalide() {
        Information<Float> signal = new Information<>(invalidSignalChain);
        assertThrows(InformationNonConformeException.class, () -> {
            codeur.decoder(signal);
        });
    }

    @DisplayName("Test lors du codage/décodage d'Information vide")
    @Test
    void testInformationVide() throws InformationNonConformeException {
        Information<Boolean> info = new Information<>();
        Information<Float> signal = codeur.encoder(info);
        Information<Boolean> infoDecodee = codeur.decoder(signal);
        assertEquals(0, signal.nbElements());
        assertEquals(0, infoDecodee.nbElements());
    }

    @DisplayName("Test lors du codage/décodage d'Information contenant 1 bit")
    @Test
    void testInformationUnBit() throws InformationNonConformeException {
        Information<Boolean> info = new Information<>(new Boolean[] {true});
        Information<Float> signal = codeur.encoder(info);
        Information<Boolean> infoDecodee = codeur.decoder(signal);
        assertEquals(nbEch, signal.nbElements());
        assertEquals(1, infoDecodee.nbElements());
    }

    @DisplayName("Test du dimensionnement (nbElements == nbBits * nbEchParBit)")
    @Test
    void testNombreEchantillon() {
        Information<Boolean> info = new Information<>(boolTestChain);
        Information<Float> signal = codeur.encoder(info);
        assertEquals(info.nbElements() * nbEch, signal.nbElements(),
                "Erreur, le nombre d'échantillon du signal ne correspond pas à nbEch * nbBits");
    }
}