package rubrica;

/**
 * Controlli prima di salvare. Se qualcosa non va, torno il messaggio da mostrare.
 */
public final class Validatore {

    // stesse misure delle colonne in schema_database.sql
    public static final int MAX_NOME = 80;
    public static final int MAX_COGNOME = 80;
    public static final int MAX_INDIRIZZO = 200;
    public static final int MAX_TELEFONO = 40;
    public static final int MAX_ETA = 150;

    private Validatore() {
    }

    public static String errore(String nome, String cognome, String indirizzo, String telefono, String etaTesto) {
        if (nome == null || cognome == null || indirizzo == null || telefono == null || etaTesto == null) {
            return "Compila i campi.";
        }
        if (nome.trim().length() == 0 || cognome.trim().length() == 0) {
            return "Nome e cognome sono obbligatori.";
        }
        if (contieneSeparatore(nome) || contieneSeparatore(cognome)
                || contieneSeparatore(indirizzo) || contieneSeparatore(telefono)) {
            return "I campi non possono contenere ';' o andare a capo.";
        }
        if (nome.trim().length() > MAX_NOME) {
            return "Il nome può avere al massimo " + MAX_NOME + " caratteri.";
        }
        if (cognome.trim().length() > MAX_COGNOME) {
            return "Il cognome può avere al massimo " + MAX_COGNOME + " caratteri.";
        }
        if (indirizzo.trim().length() > MAX_INDIRIZZO) {
            return "L'indirizzo può avere al massimo " + MAX_INDIRIZZO + " caratteri.";
        }
        if (telefono.trim().length() > MAX_TELEFONO) {
            return "Il telefono può avere al massimo " + MAX_TELEFONO + " caratteri.";
        }
        try {
            int eta = Integer.parseInt(etaTesto.trim());
            if (eta < 0) {
                return "L'età non può essere negativa.";
            }
            if (eta > MAX_ETA) {
                return "L'età non può superare " + MAX_ETA + ".";
            }
        } catch (NumberFormatException e) {
            return "L'età deve essere un numero intero.";
        }
        return null;
    }

    public static Persona crea(String nome, String cognome, String indirizzo, String telefono, String etaTesto) {
        String errore = errore(nome, cognome, indirizzo, telefono, etaTesto);
        if (errore != null) {
            throw new IllegalArgumentException(errore);
        }
        return new Persona(
                nome.trim(),
                cognome.trim(),
                indirizzo.trim(),
                telefono.trim(),
                Integer.parseInt(etaTesto.trim()));
    }

    private static boolean contieneSeparatore(String valore) {
        return valore.indexOf(';') >= 0 || valore.indexOf('\n') >= 0 || valore.indexOf('\r') >= 0;
    }
}
