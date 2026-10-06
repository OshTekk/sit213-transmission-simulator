package codesbinaires;

import information.Information;
import information.InformationNonConformeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

abstract class CodeBinaireTest {
    protected CodeBinaire codeur;
    protected int nbEch;
    protected float ampliMax, ampliMin;
    protected static final Boolean[] boolTestChain = {true, false, true, true, true, false, false, false, true, false};
    protected static final Float[] invalidSignalChain = {0.0f, 1.5f, 3.0f, 1.5f, 0.0f};

    @Test
    @DisplayName("Invariance codage puis décodage (booléen -> float -> booléen)")
    void testInvariance() throws InformationNonConformeException {
        Information<Boolean> info = new Information<>(boolTestChain);
        Information<Float> signal = codeur.encoder(info);
        Information<Boolean> infoDecode = codeur.decoder(signal);
        assertEquals(info, infoDecode, "Erreur, l'information doit être identique à l'émission.");
    }

    @Test
    @DisplayName("Rejet d'un signal analogique invalide au décodage")
    void testSignalInvalide() {
        Information<Float> signal = new Information<>(invalidSignalChain);
        assertThrows(InformationNonConformeException.class, () -> codeur.decoder(signal));
    }

    @Test
    @DisplayName("Codage et décodage d'une information vide")
    void testInformationVide() throws InformationNonConformeException {
        Information<Boolean> info = new Information<>();
        Information<Float> signal = codeur.encoder(info);
        Information<Boolean> infoDecodee = codeur.decoder(signal);
        assertEquals(0, signal.nbElements());
        assertEquals(0, infoDecodee.nbElements());
    }

    @Test
    @DisplayName("Codage et décodage d'une information à un seul bit")
    void testInformationUnBit() throws InformationNonConformeException {
        Information<Boolean> info = new Information<>(new Boolean[] {true});
        Information<Float> signal = codeur.encoder(info);
        Information<Boolean> infoDecodee = codeur.decoder(signal);
        assertEquals(nbEch, signal.nbElements());
        assertEquals(1, infoDecodee.nbElements());
    }

    @Test
    @DisplayName("Dimensionnement du signal codé (nbElements == nbBits * nbEchParBit)")
    void testNombreEchantillons() {
        Information<Boolean> info = new Information<>(boolTestChain);
        Information<Float> signal = codeur.encoder(info);
        assertEquals(info.nbElements() * nbEch, signal.nbElements(),
                "Erreur, le nombre d'échantillons du signal ne correspond pas à nbEch * nbBits");
    }
}