package visualisations;

import java.util.*;
import javax.swing.*;

/**
 * Fenêtre de base commune aux vues du simulateur.
 * Le code d'origine a été fourni par B. Prou et mis à jour par E. Cousin en 2021.
 *
 * @author Antonin Pons
 */
public class Vue extends JFrame {
    private static LinkedList<Vue> lesVues=new LinkedList<Vue>();
    private static final long serialVersionUID = 1917L;
    
    protected  static int xPosition = 0;
    protected  static int yPosition = 0;
    private static int yDecalage = 200;
 
    /**
     * Retourne la position horizontale de la prochaine vue.
     *
     * @return position horizontale en pixels
     */
    public static int getXPosition() {
        xPosition += 0;
        return xPosition - 0;
    }  

    /**
     * Retourne la position verticale de la prochaine vue et prépare la suivante.
     *
     * @return position verticale en pixels
     */
    public static int getYPosition() {
        yPosition += yDecalage;
        return yPosition - yDecalage;
    }  
   	
   
    /**
     * Construit et enregistre une vue.
     *
     * @param nom titre de la fenêtre
     */
    public  Vue (String nom) {          
        super(nom);
        lesVues.add(this);
    }
    /**
     * Réinitialise la position verticale utilisée pour placer les vues.
     */
    public static void resetPosition(){
	yPosition = 0;
    }
    /**
     * Définit la position horizontale utilisée pour placer les vues.
     *
     * @param x position horizontale en pixels
     */
    public static void setXPosition(int x){
	xPosition = x;
    }
    /**
     * Ferme toutes les vues ouvertes et réinitialise leur position.
     */
    public static void kill() {
        for (Vue v : lesVues)
            v.dispose();
        lesVues.clear();
        resetPosition();
    }
}
