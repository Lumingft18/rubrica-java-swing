package rubrica.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import rubrica.Password;
import rubrica.Persona;
import rubrica.Validatore;
import rubrica.archivio.ArchivioSuFile;
import rubrica.archivio.ArchivioUtenti;
import rubrica.archivio.Credenziali;
import rubrica.archivio.GestoreDati;

class TestRobustezza {

    @TempDir
    Path temp;

    @Test
    @DisplayName("riga rotta: avviso e copia .bak, così il salvataggio non la perde per sempre")
    void rigaRottaHaLaCopia() throws Exception {
        File file = new File(temp.toFile(), "informazioni.txt");
        scrivi(file, "Arianna;Dell’Arti;via 1;111;20\nrotta;senza;campi\n");

        GestoreDati dati = new GestoreDati(temp.toFile());
        assertNull(dati.prepara());
        Vector<Persona> persone = dati.caricaPersone();
        assertEquals(1, persone.size());
        String avviso = dati.avvisoCaricamento();
        assertNotNull(avviso);
        assertTrue(avviso.contains("rotta;senza;campi"));

        File bak = new File(temp.toFile(), "informazioni.txt.bak");
        assertTrue(bak.exists());
        dati.salvaPersone(persone);
        assertTrue(leggi(bak).contains("rotta;senza;campi"));
    }

    @Test
    @DisplayName("file pulito: nessun avviso, nessun .bak")
    void filePulito() throws Exception {
        scrivi(new File(temp.toFile(), "informazioni.txt"), "Arianna;Dell’Arti;via 1;111;20\n");
        GestoreDati dati = new GestoreDati(temp.toFile());
        dati.prepara();
        dati.caricaPersone();
        assertNull(dati.avvisoCaricamento());
        assertFalse(new File(temp.toFile(), "informazioni.txt.bak").exists());
    }

    @Test
    @DisplayName("dopo il salvataggio non resta il file .tmp")
    void niente_tmp() throws Exception {
        ArchivioSuFile archivio = new ArchivioSuFile(
                new File(temp.toFile(), "informazioni.txt"), new File(temp.toFile(), "informazioni"));
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("Arianna", "Dell’Arti", "via 1", "111", 20));
        archivio.salva(persone);
        archivio.salva(persone);
        assertFalse(new File(temp.toFile(), "informazioni.txt.tmp").exists());
        assertEquals("Arianna;Dell’Arti;via 1;111;20", leggi(new File(temp.toFile(), "informazioni.txt")).trim());
    }

    @Test
    @DisplayName("se la cartella non si scrive, informazioni.txt resta quello di prima")
    void cartellaRottaNonToccaIlTxt() throws Exception {
        File file = new File(temp.toFile(), "informazioni.txt");
        File cartella = new File(temp.toFile(), "informazioni");
        ArchivioSuFile archivio = new ArchivioSuFile(file, cartella);
        Vector<Persona> persone = new Vector<Persona>();
        persone.add(new Persona("Arianna", "Dell’Arti", "via 1", "111", 20));
        archivio.salva(persone);

        // al posto della cartella metto un file: mkdirs fallisce
        deleteTree(cartella);
        assertTrue(cartella.createNewFile());
        persone.add(new Persona("Duccio", "Patane", "via 2", "222", 30));
        try {
            archivio.salva(persone);
        } catch (java.io.IOException atteso) {
            // va bene
        }
        assertEquals("Arianna;Dell’Arti;via 1;111;20", leggi(file).trim());
        assertFalse(new File(temp.toFile(), "informazioni.txt.tmp").exists());
    }

    @Test
    @DisplayName("BOM del Blocco note: il primo nome resta pulito, anche nel properties")
    void bom() throws Exception {
        scrivi(new File(temp.toFile(), "informazioni.txt"), "\uFEFFSteve;Jobs;via Cupertino 13;0612344;56\n");
        Vector<Persona> persone = new ArchivioSuFile(
                new File(temp.toFile(), "informazioni.txt"), new File(temp.toFile(), "informazioni")).carica();
        assertEquals("Steve", persone.get(0).getNome());

        File props = new File(temp.toFile(), "credenziali_database.properties");
        scrivi(props, "\uFEFFmodalita=database\nusername=duccio\n");
        assertTrue(Credenziali.dalFile(props).usaDatabase());
    }

    @Test
    @DisplayName("lunghezze come nello schema, età fino a 150")
    void limiti() {
        assertNull(Validatore.errore(ripeti(80), ripeti(80), ripeti(200), ripeti(40), "150"));
        assertNotNull(Validatore.errore(ripeti(81), "Ferretti", "", "", "1"));
        assertNotNull(Validatore.errore("Arianna", ripeti(81), "", "", "1"));
        assertNotNull(Validatore.errore("Arianna", "Ferretti", ripeti(201), "", "1"));
        assertNotNull(Validatore.errore("Arianna", "Ferretti", "", ripeti(41), "1"));
        assertNotNull(Validatore.errore("Arianna", "Ferretti", "", "", "151"));
    }

    @Test
    @DisplayName("password: hash con sale, mai in chiaro nel file")
    void hashPassword() throws Exception {
        String h1 = Password.hash("admin");
        String h2 = Password.hash("admin");
        assertFalse(h1.equals(h2));
        assertTrue(Password.verifica("admin", h1));
        assertFalse(Password.verifica("Admin", h1));
        assertFalse(Password.inChiaro(h1));

        File file = new File(temp.toFile(), "utenti.txt");
        new ArchivioUtenti(file).assicuraUtenteDiProva();
        assertFalse(leggi(file).contains("admin;admin"));
        assertTrue(new ArchivioUtenti(file).verifica("admin", "admin"));
    }

    @Test
    @DisplayName("riga in chiaro scritta a mano: entra e viene riscritta con l'hash")
    void passwordInChiaroMigrata() throws Exception {
        File file = new File(temp.toFile(), "utenti.txt");
        scrivi(file, "rene;ciao\n");
        ArchivioUtenti utenti = new ArchivioUtenti(file);
        assertFalse(utenti.verifica("rene", "sbagliata"));
        assertTrue(leggi(file).contains("rene;ciao"));
        assertTrue(utenti.verifica("rene", "ciao"));
        assertFalse(leggi(file).contains("ciao"));
        assertTrue(utenti.verifica("rene", "ciao"));
    }

    private static String ripeti(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append('a');
        }
        return b.toString();
    }

    private static void scrivi(File file, String testo) throws Exception {
        PrintStream out = new PrintStream(new FileOutputStream(file), true, "UTF-8");
        try {
            out.print(testo);
        } finally {
            out.close();
        }
    }

    private static String leggi(File file) throws Exception {
        List<String> righe = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return String.join("\n", righe);
    }

    private static void deleteTree(File f) {
        File[] figli = f.listFiles();
        if (figli != null) {
            for (File figlio : figli) {
                deleteTree(figlio);
            }
        }
        f.delete();
    }
}
