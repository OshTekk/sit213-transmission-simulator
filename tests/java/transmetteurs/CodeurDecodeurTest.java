package transmetteurs;

import destinations.DestinationFinale;
import information.Information;
import sources.SourceAleatoire;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeurDecodeurTest {

    @Test
    @DisplayName("Boucle idéale Codeur -> Décodeur sans canal (TEB = 0.0)")
    void testBoucleParfaite() throws Exception {
        Codeur codeur = new Codeur();
        Decodeur decodeur = new Decodeur();
        DestinationFinale destination = new DestinationFinale();

        codeur.connecter(decodeur);
        decodeur.connecter(destination);

        SourceAleatoire source = new SourceAleatoire(100, 42);
        source.connecter(codeur);
        source.emettre();

        assertEquals(source.getInformationEmise(), destination.getInformationRecue());
    }

    @Test
    @DisplayName("Correction d'une erreur binaire par triplet injectée volontairement")
    void testCorrectionErreursInjectees() throws Exception {
        Codeur codeur = new Codeur();
        Decodeur decodeur = new Decodeur();
        DestinationFinale destination = new DestinationFinale();

        decodeur.connecter(destination);

        // Message initial : [0, 1]
        Information<Boolean> message = new Information<>(new Boolean[]{false, true});
        codeur.recevoir(message);

        // Erreur sur le 1er bit du 1er triplet : [1, 1, 0] (proche de 010 -> décodé en 0)
        // Erreur sur le 2ème bit du 2ème triplet : [1, 1, 1] (proche de 101 -> décodé en 1)
        Information<Boolean> signalBruite = new Information<>(new Boolean[]{
                true, true, false,
                true, true, true
        });

        decodeur.recevoir(signalBruite);

        // Le décodeur doit avoir parfaitement corrigé les erreurs et restauré [0, 1]
        assertEquals(message, destination.getInformationRecue());
    }
}
