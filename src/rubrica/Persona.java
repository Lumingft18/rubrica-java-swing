package rubrica;

/**
 * Un contatto della rubrica.
 * Campi: nome, cognome, indirizzo, telefono, eta.
 */
public class Persona {

    private int id;
    private String nome;
    private String cognome;
    private String indirizzo;
    private String telefono;
    private int eta;

    public Persona(String nome, String cognome, String indirizzo, String telefono, int eta) {
        this.nome = nome;
        this.cognome = cognome;
        this.indirizzo = indirizzo;
        this.telefono = telefono;
        this.eta = eta;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public String getTelefono() {
        return telefono;
    }

    public int getEta() {
        return eta;
    }

    public String nomeCompleto() {
        return nome + " " + cognome;
    }

    /**
     * Stessa codifica del file: nome;cognome;indirizzo;telefono;eta
     */
    public String aRiga() {
        return nome + ";" + cognome + ";" + indirizzo + ";" + telefono + ";" + eta;
    }

    /**
     * Ritorna null se la riga non ha i 5 campi, o se l'eta non è un intero.
     * Il telefono resta una stringa, così non perdo gli zeri davanti.
     */
    public static Persona daRiga(String riga) {
        if (riga == null) {
            return null;
        }
        String[] parti = riga.split(";", -1);
        if (parti.length != 5) {
            return null;
        }
        try {
            int eta = Integer.parseInt(parti[4].trim());
            return new Persona(
                    parti[0].trim(),
                    parti[1].trim(),
                    parti[2].trim(),
                    parti[3].trim(),
                    eta);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
