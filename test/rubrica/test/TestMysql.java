package rubrica.test;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import rubrica.Persona;
import rubrica.archivio.*;

@EnabledIfEnvironmentVariable(named = "RUBRICA_TEST_MYSQL_PORT", matches = "[0-9]+")
class TestMysql {
    @TempDir Path temp;
    private ArchivioMysql archivio;

    @BeforeEach void prepara() throws Exception {
        Files.write(temp.resolve("credenziali_database.properties"), (
                "modalita=database\nusername=root\npassword=rubrica-test\n"
                + "ip-server-mysql=127.0.0.1\nporta=" + System.getenv("RUBRICA_TEST_MYSQL_PORT")
                + "\ndatabase=rubrica_test\n").getBytes(StandardCharsets.UTF_8));
        archivio = new ArchivioMysql(Credenziali.dalFile(temp.resolve("credenziali_database.properties").toFile()));
        try (Connection conn = connessione(); Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM persona");
            st.executeUpdate("DELETE FROM utente");
            st.executeUpdate("INSERT INTO utente (username, password) VALUES ('admin', 'admin')");
        }
    }

    @Test void loginMigraHashERifiutaInjection() throws Exception {
        archivio.provaConnessione();
        assertFalse(archivio.login("admin", "errata"));
        assertFalse(archivio.login("' OR '1'='1", "admin"));
        assertEquals("admin", passwordSalvata());
        assertTrue(archivio.login("admin", "admin"));
        assertTrue(passwordSalvata().startsWith("pbkdf2$"));
        assertTrue(archivio.login("admin", "admin"));
    }

    @Test void creaAdminSoloSeTabellaVuota() throws Exception {
        try (Connection conn = connessione(); Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM utente");
        }
        archivio.assicuraUtenteDiProva();
        archivio.assicuraUtenteDiProva();
        assertTrue(archivio.login("admin", "admin"));
        assertTrue(passwordSalvata().startsWith("pbkdf2$"));
        try (Connection conn = connessione(); Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utente")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test void salvaModificaEliminaERicarica() throws Exception {
        Vector<Persona> persone = persone();
        archivio.riscrivi(persone);
        assertEquals(3, archivio.caricaPersone().size());
        for (int i = 0; i < 3; i++) {
            assertTrue(persone.get(i).getId() > 0);
            assertEquals(persone.get(i).getId(), archivio.caricaPersone().get(i).getId());
        }
        persone.remove(1);
        persone.set(1, new Persona("Arianna", "Dell’Arti", "via Nuova", "00004", 30));
        archivio.riscrivi(persone);
        Vector<Persona> rilette = archivio.caricaPersone();
        assertEquals("René", rilette.get(0).getNome());
        assertEquals("00004", rilette.get(1).getTelefono());
        persone.clear();
        archivio.riscrivi(persone);
        assertTrue(archivio.caricaPersone().isEmpty());
    }

    @Test void rollbackConservaDatiEIdInMemoria() throws Exception {
        Vector<Persona> persone = persone();
        archivio.riscrivi(persone);
        int idPrima = persone.get(0).getId();
        String rigaPrima = archivio.caricaPersone().get(0).aRiga();
        char[] lungo = new char[81];
        java.util.Arrays.fill(lungo, 'a');
        persone.set(1, new Persona(new String(lungo), "Patane", "", "", 30));
        assertThrows(SQLException.class, () -> archivio.riscrivi(persone));
        assertEquals(3, archivio.caricaPersone().size());
        assertEquals(rigaPrima, archivio.caricaPersone().get(0).aRiga());
        assertEquals(idPrima, persone.get(0).getId());
    }

    @Test void gestoreUsaDatabaseSenzaScrivereFileDati() throws Exception {
        GestoreDati dati = new GestoreDati(temp.toFile());
        assertNull(dati.prepara());
        assertTrue(dati.usaDatabase());
        assertTrue(dati.login("admin", "admin"));
        assertFalse(dati.login("admin", "errata"));
        dati.salvaPersone(persone());
        assertEquals(3, dati.caricaPersone().size());
        GestoreDati riaperto = new GestoreDati(temp.toFile());
        assertNull(riaperto.prepara());
        assertEquals("00001", riaperto.caricaPersone().get(0).getTelefono());
        assertFalse(Files.exists(temp.resolve("informazioni.txt")));
        assertFalse(Files.exists(temp.resolve("utenti.txt")));
        assertTrue(dati.descrizioneSalvataggio().contains("rubrica_test"));
        assertNull(dati.avvisoCaricamento());
    }

    private Vector<Persona> persone() {
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("René", "Ferretti", "via Roma", "00001", 50));
        persone.add(new Persona("Duccio", "Patane", "via Milano", "00002", 40));
        persone.add(new Persona("Arianna", "Dell’Arti", "via Torino", "00003", 30));
        return persone;
    }
    private Connection connessione() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://127.0.0.1:"
                + System.getenv("RUBRICA_TEST_MYSQL_PORT")
                + "/rubrica_test?sslMode=PREFERRED&allowPublicKeyRetrieval=true", "root", "rubrica-test");
    }
    private String passwordSalvata() throws SQLException {
        try (Connection conn = connessione(); Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT password FROM utente WHERE username = 'admin'")) {
            assertTrue(rs.next());
            return rs.getString(1);
        }
    }
}
