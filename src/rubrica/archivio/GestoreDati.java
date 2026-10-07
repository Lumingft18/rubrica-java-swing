package rubrica.archivio;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Vector;

import rubrica.Persona;

/**
 * Decide se i dati stanno sui file o su MySQL.
 * Di default sono i file: il programma parte anche senza MySQL.
 */
public class GestoreDati {

    private final File cartella;
    private final ArchivioSuFile archivio;
    private final ArchivioUtenti utenti;
    private final File fileCredenziali;
    private Credenziali credenziali;
    private ArchivioMysql mysql;

    public GestoreDati(File cartella) {
        this.cartella = cartella;
        this.archivio = new ArchivioSuFile(
                new File(cartella, "informazioni.txt"),
                new File(cartella, "informazioni"));
        this.utenti = new ArchivioUtenti(new File(cartella, "utenti.txt"));
        this.fileCredenziali = new File(cartella, "credenziali_database.properties");
        this.credenziali = Credenziali.soloFile();
    }

    /**
     * Da chiamare all'avvio. Se il database è richiesto ma non risponde,
     * torno un avviso e continuo coi file. Non blocco il programma.
     */
    public String prepara() {
        try {
            credenziali = Credenziali.dalFile(fileCredenziali);
        } catch (IOException e) {
            credenziali = Credenziali.soloFile();
            mysql = null;
            try {
                utenti.assicuraUtenteDiProva();
            } catch (IOException io) {
                return "Non riesco a preparare gli utenti.\n" + io.getMessage();
            }
            return "Non leggo le credenziali, uso i file.\n" + e.getMessage();
        }

        if (!credenziali.usaDatabase()) {
            mysql = null;
            try {
                utenti.assicuraUtenteDiProva();
            } catch (IOException e) {
                return "Non riesco a preparare gli utenti.\n" + e.getMessage();
            }
            return null;
        }

        mysql = new ArchivioMysql(credenziali);
        try {
            mysql.provaConnessione();
            mysql.assicuraUtenteDiProva();
            return null;
        } catch (SQLException e) {
            String messaggio = e.getMessage();
            credenziali = Credenziali.soloFile();
            mysql = null;
            try {
                utenti.assicuraUtenteDiProva();
            } catch (IOException io) {
                return "Database non raggiungibile e non riesco a creare gli utenti.\n" + messaggio;
            }
            return "Database non raggiungibile, continuo con i file.\n" + messaggio;
        }
    }

    public boolean usaDatabase() {
        return credenziali.usaDatabase();
    }

    public String descrizioneSalvataggio() {
        if (credenziali.usaDatabase()) {
            return "Database " + credenziali.getDatabase() + " su " + credenziali.getHost();
        }
        return "File: " + new File(cartella, "informazioni.txt").getAbsolutePath();
    }

    public String descrizioneBreve() {
        if (credenziali.usaDatabase()) {
            return "Salvataggio su database " + credenziali.getDatabase();
        }
        return "Salvataggio su informazioni.txt";
    }

    /**
     * Dopo caricaPersone: se qualche riga del file non era leggibile, il messaggio da mostrare.
     */
    public String avvisoCaricamento() {
        if (credenziali.usaDatabase() || archivio.getRigheIgnorate().isEmpty()) {
            return null;
        }
        Vector<String> righe = archivio.getRigheIgnorate();
        StringBuilder testo = new StringBuilder();
        testo.append(righe.size() == 1 ? "1 riga non è leggibile ed è stata saltata:\n"
                : righe.size() + " righe non sono leggibili e sono state saltate:\n");
        for (int i = 0; i < righe.size() && i < 5; i++) {
            testo.append("  ").append(righe.get(i)).append("\n");
        }
        if (righe.size() > 5) {
            testo.append("  ...\n");
        }
        testo.append("\nUna copia del file com'era: ").append(archivio.copiaDiSicurezza().getName())
                .append(" (o .bak nella cartella informazioni).");
        return testo.toString();
    }

    public boolean login(String username, String password) throws IOException {
        if (credenziali.usaDatabase()) {
            try {
                return mysql.login(username, password);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return utenti.verifica(username, password);
    }

    public Vector<Persona> caricaPersone() throws IOException {
        if (credenziali.usaDatabase()) {
            try {
                return mysql.caricaPersone();
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return archivio.carica();
    }

    public void salvaPersone(Vector<Persona> persone) throws IOException {
        if (credenziali.usaDatabase()) {
            try {
                mysql.riscrivi(persone);
            } catch (SQLException e) {
                throw new IOException(e.getMessage(), e);
            }
            return;
        }
        archivio.salva(persone);
    }
}
