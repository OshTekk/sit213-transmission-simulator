package sources;

import information.Information;
import java.util.Random;

/**
 * Composant Source qui génère aléatoirement une suite de bits (Boolean).
 */
public class SourceAleatoire extends Source<Boolean> {

    /** Longueur du message binaire */
    private int nbBits = 100;

    /** Semence pour le générateur aléatoire */
    private Integer seed = null;

    /**
     * Constructeur par défaut : message de 100 bits sans germe.
     */
    public SourceAleatoire() {
        super();
        generer();
    }

    /**
     * Constructeur avec une taille de message spécifiée.
     * 
     * @param nbBits nombre de bits à générer
     */
    public SourceAleatoire(int nbBits) {
        super();
        this.nbBits = nbBits;
        generer();
    }

    /**
     * Constructeur avec une taille de message et une semence.
     * 
     * @param nbBits nombre de bits à générer
     * @param seed semence pour réinitialiser le générateur aléatoire
     */
    public SourceAleatoire(int nbBits, Integer seed) {
        super();
        this.nbBits = nbBits;
        this.seed = seed;
        generer();
    }

    /**
     * Génère la séquence aléatoire de bits.
     */
    private void generer() {
        Random rand;
        if (seed == null) {
            rand = new Random();
        } else {
            rand = new Random(seed);
        }
        informationGeneree = new Information<Boolean>();
        for (int i = 0; i < nbBits; i++) {
            informationGeneree.add(rand.nextBoolean());
        }
    }

    public int getNbBits() {
        return nbBits;
    }

    public Integer getSeed() {
        return seed;
    }
}