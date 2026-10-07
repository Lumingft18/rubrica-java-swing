package rubrica.archivio;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import rubrica.Password;
import rubrica.Persona;

/**
 * Stessi dati, ma su MySQL. Lo uso solo se in credenziali_database.properties
 * c'è modalita=database e il server risponde.
 * Apro e chiudo la connessione ogni volta: la rubrica è piccola.
 */
public class ArchivioMysql {

    private final Credenziali credenziali;

    public ArchivioMysql(Credenziali credenziali) {
        this.credenziali = credenziali;
    }

    public void provaConnessione() throws SQLException {
        Connection conn = apri();
        try {
            Statement st = conn.createStatement();
            st.execute("SELECT 1");
            st.close();
        } finally {
            conn.close();
        }
    }

    public void assicuraUtenteDiProva() throws SQLException {
        Connection conn = apri();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utente");
            rs.next();
            int quanti = rs.getInt(1);
            rs.close();
            st.close();
            if (quanti > 0) {
                return;
            }
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO utente (username, password) VALUES (?, ?)");
            ps.setString(1, "admin");
            ps.setString(2, Password.hash("admin"));
            ps.executeUpdate();
            ps.close();
        } finally {
            conn.close();
        }
    }

    public boolean login(String username, String password) throws SQLException {
        Connection conn = apri();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT password FROM utente WHERE username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            boolean ok = false;
            String salvata = null;
            if (rs.next()) {
                salvata = rs.getString(1);
                ok = Password.verifica(password, salvata);
            }
            rs.close();
            ps.close();
            if (ok && Password.inChiaro(salvata)) {
                // riga inserita in chiaro (es. dallo schema): la riscrivo con l'hash
                PreparedStatement aggiorna = conn.prepareStatement(
                        "UPDATE utente SET password = ? WHERE username = ?");
                aggiorna.setString(1, Password.hash(password));
                aggiorna.setString(2, username);
                aggiorna.executeUpdate();
                aggiorna.close();
            }
            return ok;
        } finally {
            conn.close();
        }
    }

    public Vector<Persona> caricaPersone() throws SQLException {
        Vector<Persona> persone = new Vector<Persona>();
        Connection conn = apri();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT id, nome, cognome, indirizzo, telefono, eta FROM persona ORDER BY id");
            while (rs.next()) {
                Persona persona = new Persona(
                        testo(rs, "nome"),
                        testo(rs, "cognome"),
                        testo(rs, "indirizzo"),
                        testo(rs, "telefono"),
                        rs.getInt("eta"));
                persona.setId(rs.getInt("id"));
                persone.add(persona);
            }
            rs.close();
            st.close();
        } finally {
            conn.close();
        }
        return persone;
    }

    /**
     * Riscrivo tutta la tabella, come faccio col file.
     * Per una rubrica di questo tipo va bene e non lascio righe vecchie.
     */
    public void riscrivi(Vector<Persona> persone) throws SQLException {
        Connection conn = apri();
        int[] nuoviId = new int[persone.size()];
        try {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            st.executeUpdate("DELETE FROM persona");
            st.close();

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO persona (nome, cognome, indirizzo, telefono, eta) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < persone.size(); i++) {
                Persona persona = persone.get(i);
                ps.setString(1, persona.getNome());
                ps.setString(2, persona.getCognome());
                ps.setString(3, persona.getIndirizzo());
                ps.setString(4, persona.getTelefono());
                ps.setInt(5, persona.getEta());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    nuoviId[i] = keys.getInt(1);
                }
                keys.close();
            }
            ps.close();
            conn.commit();
            // cambio gli id in memoria solo quando la transazione è riuscita
            for (int i = 0; i < persone.size(); i++) {
                persone.get(i).setId(nuoviId[i]);
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignorata) {
                // tengo l'errore originale
            }
            throw e;
        } finally {
            conn.close();
        }
    }

    private Connection apri() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trovato.", e);
        }
        String url = "jdbc:mysql://" + credenziali.getHost() + ":" + credenziali.getPorta()
                + "/" + credenziali.getDatabase()
                // sslMode=PREFERRED: cifrato se il server lo permette (MySQL 8 sì, di serie)
                + "?sslMode=PREFERRED&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                + "&connectTimeout=3000&socketTimeout=5000&characterEncoding=utf8";
        return DriverManager.getConnection(url, credenziali.getUsername(), credenziali.getPassword());
    }

    private String testo(ResultSet rs, String colonna) throws SQLException {
        String valore = rs.getString(colonna);
        if (valore == null) {
            return "";
        }
        return valore;
    }
}
