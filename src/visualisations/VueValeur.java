package visualisations;
import javax.swing.*;

/**
 * Fenêtre affichant une valeur unique.
 * Le code d'origine a été fourni par B. Prou et mis à jour par E. Cousin en 2021.
 *
 */
public class VueValeur extends Vue {

    private static final long serialVersionUID = 1917L;
    
    private JLabel jLabel;
		
		
  
    /**
     * Construit une fenêtre affichant la valeur fournie.
     *
     * @param valeur valeur à afficher
     * @param nom titre de la fenêtre
     */
    public VueValeur(Object valeur, String nom) {
		super(nom);
		String s = " " + valeur;
		jLabel = new JLabel(s);

		int xPosition = Vue.getXPosition();
		int yPosition = Vue.getYPosition();
		setLocation(xPosition, yPosition);

		add(jLabel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(300, 100);
		setVisible(true);
		repaint();
    }
}
