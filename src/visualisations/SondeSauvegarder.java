package visualisations;

import information.Information;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Classe réalisant la sauvegarde d'information composée d'éléments
 * réels (float)
 */
public class SondeSauvegarder extends Sonde<Float> {
    private String fichier = "out.txt";
    /**
     * pour construire une sonde analogique
     * @param nom  le nom de la fenêtre d'affichage
     * @param fichier nom du fichier pour sauvegarder un signal
     */
    public SondeSauvegarder(String nom, String fichier) {
        super(nom);

        if (fichier != null) {
            this.fichier = fichier;
        }
    }

    /**
     * Reçoit une information analogique et l'affiche sous forme de courbe.
     *
     * @param information information analogique à afficher
     */
    public void recevoir(Information<Float> information) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.fichier))) {
            for (Float val : information) {
                writer.write(Float.toString(val));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde dans " + this.fichier + " : " + e.getMessage());
        }
    }
}
