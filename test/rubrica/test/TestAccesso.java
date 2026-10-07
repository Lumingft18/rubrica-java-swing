package rubrica.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import rubrica.archivio.ArchivioUtenti;
import rubrica.archivio.Credenziali;
import rubrica.archivio.GestoreDati;

class TestAccesso {

    @TempDir
    Path temp;

    @Test
    @DisplayName("primo avvio: creo admin/admin, la password sbagliata non entra")
    void utenteDiProva() throws Exception {
        ArchivioUtenti utenti = new ArchivioUtenti(new File(temp.toFile(), "utenti.txt"));
        utenti.assicuraUtenteDiProva();
        utenti.assicuraUtenteDiProva();

        assertTrue(utenti.verifica("admin", "admin"));
        assertFalse(utenti.verifica("admin", "sbagliata"));
        assertFalse(utenti.verifica("nessuno", "admin"));
        assertEquals(1, utenti.carica().size());
    }

    @Test
    @DisplayName("il gestore fa entrare admin e rifiuta il resto")
    void loginDalGestore() throws Exception {
        GestoreDati dati = new GestoreDati(temp.toFile());
        assertNull(dati.prepara());
        assertFalse(dati.usaDatabase());
        assertTrue(dati.login("admin", "admin"));
        assertFalse(dati.login("admin", "no"));
        assertTrue(dati.descrizioneSalvataggio().contains("informazioni.txt"));
    }

    @Test
    @DisplayName("le credenziali si leggono dal properties")
    void leggeCredenziali() throws Exception {
        File file = new File(temp.toFile(), "credenziali_database.properties");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            writer.write("modalita=database\n");
            writer.write("username=duccio\n");
            writer.write("password=segreta\n");
            writer.write("ip-server-mysql=192.168.0.10\n");
            writer.write("porta=3307\n");
            writer.write("database=rubrica\n");
        } finally {
            writer.close();
        }

        Credenziali credenziali = Credenziali.dalFile(file);
        assertTrue(credenziali.usaDatabase());
        assertEquals("duccio", credenziali.getUsername());
        assertEquals("segreta", credenziali.getPassword());
        assertEquals("192.168.0.10", credenziali.getHost());
        assertEquals("3307", credenziali.getPorta());
        assertEquals("rubrica", credenziali.getDatabase());

        assertFalse(Credenziali.dalFile(new File(temp.toFile(), "non-esiste.properties")).usaDatabase());
    }

    @Test
    @DisplayName("database spento: avviso e si continua coi file")
    void databaseSpento() throws Exception {
        File file = new File(temp.toFile(), "credenziali_database.properties");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            writer.write("modalita=database\n");
            writer.write("username=root\n");
            writer.write("password=\n");
            writer.write("ip-server-mysql=127.0.0.1\n");
            writer.write("porta=1\n");
            writer.write("database=rubrica\n");
        } finally {
            writer.close();
        }

        GestoreDati dati = new GestoreDati(temp.toFile());
        String avviso = dati.prepara();
        assertNotNull(avviso);
        assertFalse(dati.usaDatabase());
        assertTrue(dati.login("admin", "admin"));
    }
}
