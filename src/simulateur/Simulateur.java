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
import codesbinaires.AmplitudeInvalideException;
import codesbinaires.CodeBinaire;
import codesbinaires.CodeNRZ;
import codesbinaires.CodeNRZT;
import codesbinaires.CodeRZ;
import codesbinaires.NbEchantillonInvalideException;
import transmetteurs.Emetteur;
import transmetteurs.Recepteur;
import transmetteurs.Trajet;
import visualisations.SondeSauvegarder;
import transmetteurs.Codeur;
import transmetteurs.Decodeur;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    /** Indique si le codage de canal est activé. */
    private boolean codageCanal = false;

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

    /** liste des decalages temporels des multi-trajets */
    private List<Trajet> multiTrajets = new ArrayList<>();

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

            if (codageCanal) {
                Codeur codeur = new Codeur();
                Decodeur decodeur = new Decodeur();

                source.connecter(codeur);
                codeur.connecter(transmetteurLogique);
                transmetteurLogique.connecter(decodeur);
                decodeur.connecter(destination);

                if (affichage) {
                    codeur.connecter(
                            new SondeLogique("Sonde après encodage", 100)
                    );
                    decodeur.connecter(
                            new SondeLogique("Sonde après décodage", 100)
                    );
                }
            } else {
                source.connecter(transmetteurLogique);
                transmetteurLogique.connecter(destination);
            }

            // Ajout des sondes logiques si l'affichage est demandé (-s)
            if (affichage) afficherSondesLogiques(transmetteurLogique);

            if (sauvegarder) throw new ArgumentsException("Impossible de sauvegarder une transmission logique");

        } else { // Chaîne de TRANSMISSION ANALOGIQUE
            CodeBinaire codeBinaire = getCodeBinaire();

            Emetteur emetteur = new Emetteur(codeBinaire);
            Transmetteur<Float, Float> transmetteurAnalogique = (snrpbActif || !multiTrajets.isEmpty())
                    ? new TransmetteurBruite(snrpbActif ? snrpb : null, nbEch, multiTrajets, seed)
                    : new TransmetteurParfait<>();
            Recepteur recepteur = new Recepteur(codeBinaire);

            // Connexion de la chaîne analogique
            if (codageCanal) {
                Codeur codeur = new Codeur();
                Decodeur decodeur = new Decodeur();

                source.connecter(codeur);
                codeur.connecter(emetteur);
                emetteur.connecter(transmetteurAnalogique);
                transmetteurAnalogique.connecter(recepteur);
                recepteur.connecter(decodeur);
                decodeur.connecter(destination);

                if (affichage) {
                    codeur.connecter(
                            new SondeLogique("Sonde après encodage", 100)
                    );
                    decodeur.connecter(
                            new SondeLogique("Sonde après décodage", 100)
                    );
                }
            } else {
                source.connecter(emetteur);
                emetteur.connecter(transmetteurAnalogique);
                transmetteurAnalogique.connecter(recepteur);
                recepteur.connecter(destination);
            }

            // Ajout des sondes analogiques et logiques si l'affichage est demandé (-s)
            if (affichage) afficherSondesAnalogiques(emetteur, transmetteurAnalogique, recepteur);

            if (sauvegarder) {
                SondeSauvegarder sondeSauv = new SondeSauvegarder("Sonde Sauvegarder", fichierSauvegarde);
                transmetteurAnalogique.connecter(sondeSauv);
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
     * @throws ArgumentsException si la forme d'onde ou les paramètres du code sont invalides
     */
    private CodeBinaire getCodeBinaire() throws ArgumentsException {
        try {
            return switch (formeOnde) {
                case "NRZ" -> new CodeNRZ(nbEch, amplMin, amplMax);
                case "NRZT" -> new CodeNRZT(nbEch, amplMin, amplMax);
                case "RZ" -> new CodeRZ(nbEch, amplMin, amplMax);
                default -> throw new ArgumentsException("Valeur du parametre -form invalide : " + formeOnde);
            };
        } catch (AmplitudeInvalideException | NbEchantillonInvalideException e) {
            throw new ArgumentsException(e.getMessage());
        }
    }


    /**
     * Lit les options dans leur ordre d'apparition et configure la simulation.
     * Une option répétée remplace sa valeur précédente.
     *
     * <dl>
     * <dt>-mess m</dt><dd>Message binaire d'au moins 7 bits, ou nombre de bits aléatoires sur 1 à 6 chiffres.</dd>
     * <dt>-s</dt><dd>Active les sondes d'affichage.</dd>
     * <dt>-seed v</dt><dd>Fixe la semence des générateurs aléatoires.</dd>
     * <dt>-form f</dt><dd>Sélectionne RZ, NRZ ou NRZT et active la transmission analogique.</dd>
     * <dt>-nbEch n</dt><dd>Fixe le nombre d'échantillons par bit.</dd>
     * <dt>-ampl min max</dt><dd>Fixe les amplitudes minimale et maximale.</dd>
     * <dt>-snrpb snr</dt><dd>Active le canal bruité avec le rapport signal sur bruit par bit.</dd>
     * <dt>-save fichier</dt><dd>Demande la sauvegarde du signal analogique.</dd>
     * </dl>
     *
     * @param args options de la ligne de commande
     * @throws ArgumentsException si une option ou la configuration résultante est invalide
     */
    private void analyseArguments(String[] args) throws ArgumentsException {
        for (int i = 0; i < args.length; ) {
            switch (args[i]) {
                case "-s" -> {
                    affichage = true;
                    i++;
                }
                case "-seed" -> {
                    seed = lireEntier(lireValeurObligatoire(args, i + 1, "-seed"), "-seed");
                    i += 2;
                }
                case "-mess" -> {
                    analyserMessage(lireValeurObligatoire(args, i + 1, "-mess"));
                    i += 2;
                }
                case "-form" -> {
                    transmissionAnalogique = true;
                    formeOnde = lireValeurObligatoire(args, i + 1, "-form");
                    if (!formeOnde.matches("NRZ|NRZT|RZ")) {
                        throw new ArgumentsException("Valeur du parametre -form invalide : " + formeOnde);
                    }
                    i += 2;
                }
                case "-nbEch" -> {
                    transmissionAnalogique = true;
                    String valeur = lireValeurObligatoire(args, i + 1, "-nbEch");
                    nbEch = lireEntier(valeur, "-nbEch");
                    if (nbEch <= 0) {
                        throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + valeur);
                    }
                    i += 2;
                }
                case "-ampl" -> {
                    transmissionAnalogique = true;
                    amplMin = lireReel(lireValeurObligatoire(args, i + 1, "-ampl"), "min du parametre -ampl");
                    amplMax = lireReel(lireValeurObligatoire(args, i + 2, "-ampl"), "max du parametre -ampl");
                    i += 3;
                }
                case "-snrpb" -> {
                    transmissionAnalogique = true;
                    snrpbActif = true;
                    snrpb = lireReel(lireValeurObligatoire(args, i + 1, "-snrpb"), "du parametre -snrpb");
                    i += 2;
                }
                case "-save" -> {
                    sauvegarder = true;
                    fichierSauvegarde = lireValeurObligatoire(args, i + 1, "-save").replace(" ", "_");
                    i += 2;
                }
                case "-ti" -> {
                    transmissionAnalogique = true;
                    multiTrajets.clear();
                    i = analyserMultiTrajets(args, i);
                }
                case "-codeur" -> {
                    codageCanal = true;
                    i++;
                }
                default -> throw new ArgumentsException("Option invalide :" + args[i]);
            }
        }
        validerConfigurationAnalogique();
    }

    /**
     * Analyse les multitrajets à traiter
     *
     * @param args arguments de la ligne de commande
     * @param i position actuelle
     * @return position de la fin des multitrajets
     * @throws ArgumentsException si la valeur manque
     */
    private int analyserMultiTrajets(String[] args, int i) throws ArgumentsException {
        ++i;

        if (i >= args.length || args[i].startsWith("-")) {
            throw new ArgumentsException("Valeur du parametre -ti manquante");
        }

        while (i < args.length && !args[i].startsWith("-")) {
            if (multiTrajets.size() >= 5) {
                throw new ArgumentsException("Nombre de trajets indirects superieur a 5 pour le parametre -ti");
            }
            int dt = lireEntier(args[i], "-ti (retard)");
            if (dt < 0) {
                throw new ArgumentsException("Le retard du trajet (-ti) doit etre positif : " + dt);
            }
            i++;
            if (i >= args.length || args[i].startsWith("-")) {
                throw new ArgumentsException("Amplitude manquante pour le trajet indirect de retard " + dt);
            }
            float ar = lireReel(args[i], "du parametre -ti (attenuation)");
            if (ar < 0.0f || ar > 1.0f) {
                throw new ArgumentsException("L'amplitude du trajet (-ti) doit etre comprise entre 0.0 et 1.0 : " + ar);
            }
            i++;
            multiTrajets.add(new Trajet(dt, ar));
        }

        if (multiTrajets.size() > 5) {
            throw new ArgumentsException("Erreur, il ne peut y avoir que 5 multitrajets au maximum.");
        }

        return i;
    }

    /**
     * Renvoie la valeur qui suit une option nécessitant un argument.
     *
     * @param args arguments de la ligne de commande
     * @param position position attendue de la valeur
     * @param option nom de l'option concernée
     * @return valeur présente à la position demandée
     * @throws ArgumentsException si la valeur manque
     */
    private static String lireValeurObligatoire(String[] args, int position, String option) throws ArgumentsException {
        if (position >= args.length) {
            throw new ArgumentsException("Valeur du parametre " + option + " manquante");
        }
        return args[position];
    }

    /**
     * Convertit la valeur d'une option en entier.
     *
     * @param valeur texte à convertir
     * @param option nom de l'option concernée
     * @return entier lu
     * @throws ArgumentsException si la valeur n'est pas un entier valide
     */
    private static int lireEntier(String valeur, String option) throws ArgumentsException {
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new ArgumentsException("Valeur du parametre " + option + " invalide : " + valeur);
        }
    }

    /**
     * Convertit la valeur d'une option en nombre réel.
     *
     * @param valeur texte à convertir
     * @param description rôle de la valeur dans l'option
     * @return nombre réel lu
     * @throws ArgumentsException si la valeur n'est pas un nombre réel valide
     */
    private static float lireReel(String valeur, String description) throws ArgumentsException {
        try {
            return Float.parseFloat(valeur);
        } catch (NumberFormatException e) {
            throw new ArgumentsException("Valeur " + description + " invalide : " + valeur);
        }
    }

    /**
     * Interprète l'argument de {@code -mess} comme un message fixe ou une longueur aléatoire.
     *
     * @param valeur message binaire d'au moins 7 bits ou longueur sur 1 à 6 chiffres
     * @throws ArgumentsException si la valeur ne respecte aucun de ces formats
     */
    private void analyserMessage(String valeur) throws ArgumentsException {
        if (valeur.matches("[01]{7,}")) {
            messageString = valeur;
            messageAleatoire = false;
            nbBitsMess = valeur.length();
        } else if (valeur.matches("[0-9]{1,6}")) {
            int longueur = Integer.parseInt(valeur);
            if (longueur < 1) {
                throw new ArgumentsException("Valeur du parametre -mess invalide : " + valeur);
            }
            messageString = valeur;
            messageAleatoire = true;
            nbBitsMess = longueur;
        } else {
            throw new ArgumentsException("Valeur du parametre -mess invalide : " + valeur);
        }
    }

    /**
     * Vérifie les contraintes d'amplitude de la forme d'onde analogique choisie.
     *
     * @throws ArgumentsException si les amplitudes sont incompatibles avec la forme choisie
     */
    private void validerConfigurationAnalogique() throws ArgumentsException {
        if (!transmissionAnalogique) {
            return;
        }
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
        Iterator<Boolean> bitsEmis = infoEmise.iterator();
        Iterator<Boolean> bitsRecus = infoRecue.iterator();

        while (bitsEmis.hasNext() && bitsRecus.hasNext()) {
            if (!bitsEmis.next().equals(bitsRecus.next())) {
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
