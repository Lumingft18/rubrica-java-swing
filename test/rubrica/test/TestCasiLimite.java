package rubrica.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rubrica.Password;
import rubrica.Persona;
import rubrica.Validatore;
import rubrica.archivio.*;

class TestCasiLimite {
    @TempDir Path temp;

    @Test void configurazioneIlleggibilePermetteLogin() throws Exception {
        Files.createDirectory(temp.resolve("credenziali_database.properties"));
        GestoreDati dati = new GestoreDati(temp.toFile());
        assertNotNull(dati.prepara());
        assertFalse(dati.usaDatabase());
        assertTrue(dati.login("admin", "admin"));
    }

    @Test void configurazioneMalformataPermetteLogin() throws Exception {
        scrivi("credenziali_database.properties", "password=\\uZZZZ\n");
        GestoreDati dati = new GestoreDati(temp.toFile());
        assertNotNull(dati.prepara());
        assertTrue(dati.login("admin", "admin"));
    }

    @Test void cartellaDatiSostituitaDaFileSegnalaErrore() throws Exception {
        scrivi("informazioni", "non è una cartella");
        assertThrows(IOException.class, () -> archivio().carica());
    }

    @Test void eliminaTuttiERiapreVuoto() throws Exception {
        ArchivioSuFile archivio = archivio();
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("René", "Ferretti", "via Roma", "001", 50));
        archivio.salva(persone);
        persone.clear();
        archivio.salva(persone);
        assertTrue(archivio.carica().isEmpty());
        assertEquals(0, temp.resolve("informazioni").toFile().list().length);
        assertEquals(0, Files.size(temp.resolve("informazioni.txt")));
    }

    @Test void fileVuotoPrevaleSullaCartella() throws Exception {
        Files.createDirectory(temp.resolve("informazioni"));
        scrivi("informazioni/Ferretti-René.txt", "René;Ferretti;;001;50");
        scrivi("informazioni.txt", "");
        assertTrue(archivio().carica().isEmpty());
    }

    @Test void recuperoCartellaConRigaRotta() throws Exception {
        Files.createDirectory(temp.resolve("informazioni"));
        scrivi("informazioni/Ferretti-René.txt", "\uFEFFRené;Ferretti;;001;50");
        scrivi("informazioni/rotta.txt", "riga rotta");
        ArchivioSuFile archivio = archivio();
        Vector<Persona> persone = archivio.carica();
        assertEquals(1, persone.size());
        assertEquals("René", persone.get(0).getNome());
        assertEquals(1, archivio.getRigheIgnorate().size());
        archivio.salva(persone);
        assertEquals("riga rotta", leggi("informazioni/rotta.txt.bak"));
    }

    @Test void nomiFileDifficiliEDoppioni() throws Exception {
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("René", "Fer/retti", "", "001", 50));
        persone.add(new Persona("RENÉ", "FERRETTI", "", "002", 51));
        persone.add(new Persona("René", "Ferretti", "", "003", 52));
        archivio().salva(persone);
        String[] nomi = temp.resolve("informazioni").toFile().list();
        assertEquals(3, nomi.length);
        assertTrue(Arrays.stream(nomi).noneMatch(n -> n.contains("/")));
        Files.delete(temp.resolve("informazioni.txt"));
        assertEquals(3, archivio().carica().size());
    }

    @Test void campiFacoltativiVuotiEUnicode() throws Exception {
        Persona persona = Validatore.crea(" René ", " Ferretti ", "", "", "0");
        assertEquals("René;Ferretti;;;0", persona.aRiga());
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(persona);
        archivio().salva(persone);
        assertEquals(persona.aRiga(), archivio().carica().get(0).aRiga());
    }

    @Test void validatoreRifiutaNullEAcapo() {
        assertNotNull(Validatore.errore(null, "Ferretti", "", "", "20"));
        assertNotNull(Validatore.errore("René", "Ferretti", "via\nRoma", "", "20"));
        assertNotNull(Validatore.errore("René", "Ferretti", "", "001\r002", "20"));
        assertNotNull(Validatore.errore("René", "Ferretti", "", "", "2147483648"));
        assertNotNull(Validatore.errore("René", "", "", "", "20"));
    }

    @Test void hashMalformatiNonFannoEntrare() {
        for (String hash : new String[] {"pbkdf2$", "pbkdf2$x$AA==$AA==",
                "pbkdf2$0$AA==$AA==", "pbkdf2$1$!$!", "pbkdf2$1$$AA=="}) {
            assertFalse(Password.verifica("ciao", hash), hash);
        }
        assertFalse(Password.verifica(null, "ciao"));
        assertFalse(Password.verifica("ciao", null));
    }

    @Test void passwordConSeparatoreEUtenteEsistente() throws Exception {
        scrivi("utenti.txt", "duccio;ci;ao\n");
        ArchivioUtenti utenti = new ArchivioUtenti(temp.resolve("utenti.txt").toFile());
        utenti.assicuraUtenteDiProva();
        assertFalse(utenti.verifica("admin", "admin"));
        assertTrue(utenti.verifica("duccio", "ci;ao"));
        assertTrue(utenti.verifica("duccio", "ci;ao"));
        assertEquals(1, utenti.carica().size());
        assertFalse(leggi("utenti.txt").contains("ci;ao"));
    }

    private ArchivioSuFile archivio() {
        return new ArchivioSuFile(temp.resolve("informazioni.txt").toFile(), temp.resolve("informazioni").toFile());
    }
    private void scrivi(String nome, String testo) throws IOException {
        Files.write(temp.resolve(nome), testo.getBytes(StandardCharsets.UTF_8));
    }
    private String leggi(String nome) throws IOException {
        return new String(Files.readAllBytes(temp.resolve(nome)), StandardCharsets.UTF_8);
    }
}
