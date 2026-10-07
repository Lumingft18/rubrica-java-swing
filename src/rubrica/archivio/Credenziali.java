package rubrica.archivio;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Legge credenziali_database.properties.
 * Se il file non c'è, o se modalita non è "database", resto sui file.
 */
public class Credenziali {

    private final String modalita;
    private final String username;
    private final String password;
    private final String host;
    private final String porta;
    private final String database;

    private Credenziali(String modalita, String username, String password, String host, String porta, String database) {
        this.modalita = modalita;
        this.username = username;
        this.password = password;
        this.host = host;
        this.porta = porta;
        this.database = database;
    }

    public static Credenziali soloFile() {
        return new Credenziali("file", "root", "", "localhost", "3306", "rubrica");
    }

    public static Credenziali dalFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            return soloFile();
        }
        Properties props = new Properties();
        // Properties di default legge ISO-8859-1. Forzo UTF-8.
        // Tolgo il BOM del Blocco note: se resta, la prima chiave non si chiama più "modalita".
        String contenuto = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        try {
            props.load(new StringReader(FileTesto.senzaBom(contenuto)));
        } catch (IllegalArgumentException e) {
            throw new IOException("Formato delle credenziali non valido.", e);
        }
        String modalita = props.getProperty("modalita", "file");
        if (modalita == null || modalita.trim().length() == 0) {
            modalita = "file";
        }
        return new Credenziali(
                modalita.trim(),
                testo(props, "username", "root"),
                props.getProperty("password", ""),
                testo(props, "ip-server-mysql", "localhost"),
                testo(props, "porta", "3306"),
                testo(props, "database", "rubrica"));
    }

    private static String testo(Properties props, String chiave, String seManca) {
        String valore = props.getProperty(chiave, seManca);
        if (valore == null) {
            return seManca;
        }
        return valore.trim();
    }

    public boolean usaDatabase() {
        return "database".equalsIgnoreCase(modalita);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public String getPorta() {
        return porta;
    }

    public String getDatabase() {
        return database;
    }
}
