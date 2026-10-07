package rubrica;

import java.io.File;
import java.net.URL;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import rubrica.archivio.GestoreDati;
import rubrica.gui.FinestraLogin;

/**
 * Avvio. Si apre solo il login.
 * Utente di prova: admin / admin
 */
public class RubricaMain {

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Rubrica");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // resta il look di default
        }

        final GestoreDati dati = new GestoreDati(cartellaApplicazione());

        SwingUtilities.invokeLater(() -> {
            // prepara() lo chiama il login, in background
            FinestraLogin login = new FinestraLogin(dati);
            login.setVisible(true);
        });
    }

    /**
     * I file dati stanno accanto al jar, non nella home.
     * toURI tiene gli spazi nel percorso della cartella.
     */
    static File cartellaApplicazione() {
        try {
            URL url = RubricaMain.class.getProtectionDomain().getCodeSource().getLocation();
            File qui = new File(url.toURI());
            if (qui.isFile()) {
                File padre = qui.getParentFile();
                if (padre != null) {
                    return padre;
                }
            }
            // da Eclipse, o con java -cp bin, le classi stanno in bin
            if ("bin".equals(qui.getName()) && qui.getParentFile() != null) {
                return qui.getParentFile();
            }
            return qui;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }
}
