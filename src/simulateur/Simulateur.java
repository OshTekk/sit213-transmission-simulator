package simulateur;
import destinations.Destination;
import destinations.DestinationFinale;
import sources.Source;
import sources.SourceAleatoire;
import sources.SourceFixe;
import transmetteurs.Transmetteur;
import transmetteurs.TransmetteurBruite;
import transmetteurs.TransmetteurParfait;
import visualisations.SondeLogique;
import visualisations.SondeAnalogique;
import information.Information;
import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import codesbinaires.CodeNRZT;
import codesbinaires.CodeRZ;
import emetteurs.Emetteur;
import recepteurs.Recepteur;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/** La classe Simulateur permet de construire et simuler une chaîne de
 * transmission composée d'une Source, d'un nombre variable de
 * Transmetteur(s) et d'une Destination.
 * @author cousin
 * @author prou
 *
 */
public class Simulateur {
      	
    /** indique si le Simulateur utilise des sondes d'affichage */
    private boolean affichage = false;
    
    /** indique si le Simulateur utilise un message généré de manière aléatoire (message imposé sinon) */
    private boolean messageAleatoire = true;

    /** la valeur de la semence utilisée pour les générateurs aléatoires */
    private Integer seed = null; // pas de semence par défaut
    
    /** la longueur du message aléatoire à transmettre si un message n'est pas imposé */
    private int nbBitsMess = 100; 
    
    /** la chaîne de caractères correspondant à m dans l'argument -mess m */
    private String messageString = "100";

    /** indique si le Simulateur utilise une transmission analogique (par défaut false = logique) */
    private boolean transmissionAnalogique = false;

    /** la forme d'onde utilisée pour la transmission analogique ("RZ", "NRZ", "NRZT") */
    private String formeOnde = "RZ";

    /** le nombre d'échantillons par bit pour la transmission analogique */
    private int nbEch = 30;

    /** l'amplitude minimale du signal analogique */
    private float amplMin = 0.0f;

    /** l'amplitude maximale du signal analogique */
    private float amplMax = 1.0f;

    /** indique si le canal analogique doit être bruité */
    private boolean snrpbActif = false;

    /** rapport signal sur bruit par bit */
    private float snrpb = 0.0f;

    /** le  composant Source de la chaine de transmission */
    private Source<Boolean> source = null;

    /** le  composant Destination de la chaine de transmission */
    private Destination<Boolean> destination = null;

    /** fichier dans lequel le signal de sortie est sauvegardé*/
    private String fichierSauvegarde = "out.txt";

    /** indique si le signal en sortie du transmetteur analogique doit être enregistré */
    private boolean sauvegarder = false;
   
    /** Le constructeur de Simulateur construit une chaîne de
     * transmission composée d'une {@code Source<Boolean>}, d'une {@code Destination<Boolean>}
     * et de Transmetteur(s) [voir la méthode
     * analyseArguments]...  <br> Les différents composants de la
     * chaîne de transmission (Source, Transmetteur(s), Destination,
     * Sonde(s) de visualisation) sont créés et connectés.
     * @param args le tableau des différents arguments.
     *
     * @throws ArgumentsException si un des arguments est incorrect
     *
     */   
    public Simulateur(String[] args) throws ArgumentsException {
    	// analyser et récupérer les arguments   	
    	analyseArguments(args);
      
        // Instanciation de la source (aléatoire par défaut, fixe quand la classe sera fusionnée)
        source = messageAleatoire
                ? new SourceAleatoire(nbBitsMess, seed)
                : new SourceFixe(messageString);

        // Instanciation de la destination
        destination = new DestinationFinale();

        if (!transmissionAnalogique) { // Chaîne de TRANSMISSION LOGIQUE (Étape 1)
            Transmetteur<Boolean, Boolean> transmetteurLogique = new TransmetteurParfait<>();

            source.connecter(transmetteurLogique);
            transmetteurLogique.connecter(destination);

            // Ajout des sondes logiques si l'affichage est demandé (-s)
            if (affichage) afficherSondesLogiques(transmetteurLogique);

        } else { // Chaîne de TRANSMISSION ANALOGIQUE bruitée (Étape 3)
            CodeBinaire codeBinaire = getCodeBinaire();

            Emetteur emetteur = new Emetteur(codeBinaire);
            Transmetteur<Float, Float> transmetteurAnalogique = snrpbActif
                    ? new TransmetteurBruite(snrpb, nbEch, seed)
                    : new TransmetteurParfait<>();
            Recepteur recepteur = new Recepteur(codeBinaire);

            // Connexion de la chaîne analogique
            source.connecter(emetteur);
            emetteur.connecter(transmetteurAnalogique);
            transmetteurAnalogique.connecter(recepteur);
            recepteur.connecter(destination);

            // Ajout des sondes analogiques et logiques si l'affichage est demandé (-s)
            if (affichage) afficherSondesAnalogiques(emetteur, transmetteurAnalogique, recepteur);

            if (sauvegarder) {
                // On connecte une destination anonyme à la sortie du canal analogique
                // Cette destination
                transmetteurAnalogique.connecter(new Destination<>() {
                    @Override
                    public void recevoir(Information<Float> information) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierSauvegarde))) {
                            for (Float val : information) {
                                writer.write(Float.toString(val));
                                writer.newLine();
                            }
                        } catch (IOException e) {
                            System.err.println("Erreur lors de la sauvegarde dans " + fichierSauvegarde + " : " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    /**
     * Affiche les sondes analogiques
     */
    private void afficherSondesAnalogiques(Emetteur emetteur, Transmetteur<Float, Float> transmetteurAnalogique, Recepteur recepteur) {
        SondeLogique sondeSource = new SondeLogique("Sonde source", 100);
        SondeAnalogique sondeEmetteur = new SondeAnalogique("Sonde émetteur");
        SondeAnalogique sondeTransmetteur = new SondeAnalogique("Sonde canal");
        SondeLogique sondeRecepteur = new SondeLogique("Sonde récepteur", 100);

        source.connecter(sondeSource);
        emetteur.connecter(sondeEmetteur);
        transmetteurAnalogique.connecter(sondeTransmetteur);
        recepteur.connecter(sondeRecepteur);
    }

    /**
     * Affiche les sondes logiques
     */
    private void afficherSondesLogiques(Transmetteur<Boolean, Boolean> transmetteurLogique) {
        SondeLogique sondeSource = new SondeLogique("Sonde source", 100);
        SondeLogique sondeTransmetteur = new SondeLogique("Sonde transmetteur", 100);
        source.connecter(sondeSource);
        transmetteurLogique.connecter(sondeTransmetteur);
    }

    /**
     * Construit le code en ligne sélectionné dans les arguments du simulateur.
     *
     * @return code binaire configuré
     * @throws ArgumentsException si les paramètres du code sont invalides
     */
    private CodeBinaire getCodeBinaire() throws ArgumentsException {
        CodeBinaire codeBinaire;
        try {
            switch (formeOnde) {
                case "NRZ":
                    codeBinaire = new CodeNRZ(nbEch, amplMin, amplMax);
                    break;
                case "NRZT":
                    codeBinaire = new CodeNRZT(nbEch, amplMin, amplMax);
                    break;
                case "RZ":
                default:
                    codeBinaire = new CodeRZ(nbEch, amplMin, amplMax);
                    break;
            }
        } catch (Exception e) {
            throw new ArgumentsException(e.getMessage());
        }
        return codeBinaire;
    }


    /** La méthode analyseArguments extrait d'un tableau de chaînes de
     * caractères les différentes options de la simulation.  <br>Elle met
     * à jour les attributs correspondants du Simulateur.
     *
     * @param args le tableau des différents arguments.
     * <br>
     * <br>Les arguments autorisés sont : 
     * <br> 
     * <dl>
     * <dt> -mess m  </dt><dd> m (String) constitué de 7 ou plus digits à 0 | 1, le message à transmettre</dd>
     * <dt> -mess m  </dt><dd> m (int) constitué de 1 à 6 digits, le nombre de bits du message "aléatoire" à transmettre</dd> 
     * <dt> -s </dt><dd> pour demander l'utilisation des sondes d'affichage</dd>
     * <dt> -seed v </dt><dd> v (int) d'initialisation pour les générateurs aléatoires</dd> 
     * <dt> -form f </dt><dd> f (String) forme d'onde ("RZ", "NRZ", "NRZT") pour transmission analogique</dd>
     * <dt> -nbEch ne </dt><dd> ne (int) nombre d'échantillons par bit</dd>
     * <dt> -ampl min max </dt><dd> min max (float) amplitudes min et max</dd>
     * <dt> -snrpb snr </dt><dd> snr (float) Rapport signal sur bruit par bit</dd>
     * </dl>
     *
     * @throws ArgumentsException si un des arguments est incorrect.
     *
     */   
    private void analyseArguments(String[] args) throws ArgumentsException {

    	for (int i=0; i<args.length; i++) { // traiter les arguments 1 par 1

    		if (args[i].equals("-s")) affichage = true;

    		else if (args[i].equals("-seed")) {
    			i++;
                // vérifier si une valeur est associée
                if (i >= args.length) throw new ArgumentsException("Valeur de la graine -seed manquante");

    			// traiter la valeur associee
    			try { 
    				seed = Integer.valueOf(args[i]);
    			}
    			catch (Exception e) {
    				throw new ArgumentsException("Valeur du parametre -seed  invalide :" + args[i]);
    			}           		
    		}

    		else if (args[i].equals("-mess")){
    			i++;
                // vérifier si une valeur est associée
                if (i >= args.length) throw new ArgumentsException("Valeur du parametre -mess manquante");

    			// traiter la valeur associee
    			messageString = args[i];
    			if (args[i].contains(",")) {
                    throw new ArgumentsException ("Valeur du parametre -mess invalide : " + nbBitsMess);
                }
                else if (args[i].matches("[01]{7,}")) { // au moins 7 digits
    				messageAleatoire = false;
    				nbBitsMess = args[i].length();
    			} 
    			else if (args[i].matches("[0-9]{1,6}")) { // de 1 à 6 chiffres
    				messageAleatoire = true;
    				nbBitsMess = Integer.valueOf(args[i]);
    				if (nbBitsMess < 1) 
    					throw new ArgumentsException ("Valeur du parametre -mess invalide : " + nbBitsMess);
    			}
    			else throw new ArgumentsException("Valeur du parametre -mess invalide : " + args[i]);
    		}
    		
    		else if (args[i].equals("-form")) {
    			transmissionAnalogique = true;
    			i++;
    			if (i >= args.length) {
    				throw new ArgumentsException("Valeur du parametre -form manquante");
    			}
    			formeOnde = args[i];
    			if (!formeOnde.matches("NRZ|NRZT|RZ")) {
    				throw new ArgumentsException("Valeur du parametre -form invalide : " + args[i]);
    			}
    		}

    		else if (args[i].equals("-nbEch")) {
    			transmissionAnalogique = true;
    			i++;
    			if (i >= args.length) {
    				throw new ArgumentsException("Valeur du parametre -nbEch manquante");
    			}
    			try {
    				nbEch = Integer.valueOf(args[i]);
    				if (nbEch <= 0) {
    					throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + args[i]);
    				}
    			} catch (Exception e) {
    				throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + args[i]);
    			}
    		}

    		else if (args[i].equals("-ampl")) {
    			transmissionAnalogique = true;
    			i++;
    			if (i >= args.length) {
    				throw new ArgumentsException("Valeurs du parametre -ampl manquantes");
    			}
    			try {
    				amplMin = Float.parseFloat(args[i]);
    			} catch (NumberFormatException e) {
    				throw new ArgumentsException("Valeur min du parametre -ampl invalide : " + args[i]);
    			}
    			i++;
    			if (i >= args.length) {
    				throw new ArgumentsException("Valeur max du parametre -ampl manquante");
    			}
    			try {
    				amplMax = Float.parseFloat(args[i]);
    			} catch (NumberFormatException e) {
    				throw new ArgumentsException("Valeur max du parametre -ampl invalide : " + args[i]);
    			}
    		}

            else if (args[i].equals("-snrpb")) {
                transmissionAnalogique = true;
                snrpbActif = true;
                i++;
                if (i >= args.length) {
                    throw new ArgumentsException("Valeur du parametre -snrpb manquante");
                }
                try {
                    snrpb = Float.parseFloat(args[i]);
                } catch (Exception e) {
                    throw new ArgumentsException("Valeur du parametre -snrpb invalide : " + args[i]);
                }
            }

            else if (args[i].equals("-save")) {
                i++;
                sauvegarder = true;
                if (i >= args.length) {
                    throw new ArgumentsException("Valeur du parametre -save manquante");
                }
                fichierSauvegarde = args[i].replace(" ", "_");
            }

    		//TODO : ajouter ci-après le traitement des nouvelles options

    		else throw new ArgumentsException("Option invalide :"+ args[i]);
    	}

        // Verification de la coherence des contraintes d'amplitude pour la transmission analogique
        if (transmissionAnalogique) {
            if (amplMin >= amplMax) {
                throw new ArgumentsException("Valeurs du parametre -ampl invalides : min doit etre strictement inferieur a max (" + amplMin + " >= " + amplMax + ")");
            }
            if (formeOnde.equals("RZ")) {
                if (amplMin != 0.0f) {
                    throw new ArgumentsException("Contrainte RZ invalide : amplMin doit etre egal a 0.0 (" + amplMin + ")");
                }
                if (amplMax < 0.0f) {
                    throw new ArgumentsException("Contrainte RZ invalide : amplMax doit etre positif (" + amplMax + ")");
                }
            } else { // NRZ et NRZT
                if (amplMin > 0.0f) {
                    throw new ArgumentsException("Contrainte " + formeOnde + " invalide : amplMin doit etre negatif ou nul (" + amplMin + ")");
                }
                if (amplMax < 0.0f) {
                    throw new ArgumentsException("Contrainte " + formeOnde + " invalide : amplMax doit etre positif ou nul (" + amplMax + ")");
                }
            }
        }
      
    }
     
    
   	
    /** La méthode execute effectue un envoi de message par la source
     * de la chaîne de transmission du Simulateur.
     *
     * @throws Exception si un problème survient lors de l'exécution
     *
     */ 
    public void execute() throws Exception {      
    	source.emettre(); 
    }
   
   	   	
   	
    /** La méthode qui calcule le taux d'erreur binaire en comparant
     * les bits du message émis avec ceux du message reçu.
     *
     * @return  La valeur du Taux dErreur Binaire.
     */   	   
    public float calculTauxErreurBinaire() {
        Information<Boolean> infoEmise = source.getInformationEmise();
        Information<Boolean> infoRecue = destination.getInformationRecue();

        if (infoEmise == null || infoRecue == null || infoEmise.nbElements() == 0) {
            return 0.0f;
        }

        int nbErreurs = 0;
        int tailleMin = Math.min(infoEmise.nbElements(), infoRecue.nbElements());

        for (int i = 0; i < tailleMin; i++) {
            if (!infoEmise.iemeElement(i).equals(infoRecue.iemeElement(i))) {
                nbErreurs++;
            }
        }

        nbErreurs += Math.abs(infoEmise.nbElements() - infoRecue.nbElements());

        return (float) nbErreurs / infoEmise.nbElements();
    }
   
   
   
   
    /** La fonction main instancie un Simulateur à l'aide des
     *  arguments paramètres et affiche le résultat de l'exécution
     *  d'une transmission.
     *  @param args les différents arguments qui serviront à l'instanciation du Simulateur.
     */
    public static void main(String [] args) {

    	Simulateur simulateur = null;

    	try {
    		simulateur = new Simulateur(args);
    	}
    	catch (Exception e) {
    		System.out.println(e); 
    		System.exit(-1);
    	} 

    	try {
    		simulateur.execute();
    		String s = "java  Simulateur  ";
    		for (int i = 0; i < args.length; i++) { //copier tous les paramètres de simulation
    			s += args[i] + "  ";
    		}
    		System.out.println(s + "  =>   TEB : " + simulateur.calculTauxErreurBinaire());
    	}
    	catch (Exception e) {
    		System.out.println(e);
    		e.printStackTrace();
    		System.exit(-2);
    	}              	
    }
}
