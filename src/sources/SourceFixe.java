package sources;

import information.*;

public class SourceFixe extends Source<Boolean> {

    public SourceFixe(String chaine) {
        super();

        Information<Boolean> information = new Information<Boolean>();

        for (int i = 0; i < chaine.length(); i++) {
            if (chaine.charAt(i) == '0') {
                information.add(false);
            } else if (chaine.charAt(i) == '1') {
                information.add(true);
            }
        }

        this.informationGeneree = information;
    }
}
